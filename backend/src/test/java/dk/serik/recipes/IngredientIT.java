package dk.serik.recipes;

import dk.serik.recipes.dto.IngredientDTO;
import dk.serik.recipes.exceptions.ApplicationErrorCodes;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.json.JsonMapper;

import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Ingredient writes through the whole stack, against the database's own constraints - see
 * {@code CategoryIT}, which this mirrors. An ingredient is referenced from {@code recipe_ingredient},
 * whose key is (recipe, ingredient), so each referencing row is a different recipe.
 */
@SpringBootTest(classes = RecipesApplication.class)
@AutoConfigureMockMvc
@AutoConfigureTestDatabase
@WithMockUser(username = "it-user")
class IngredientIT {

	private static final String INGREDIENTS = "/api/v1/ingredients";

	// Seeded by db.changelog_1.1.xml, which the tests load too.
	private static final String BROED_CATEGORY_ID = "14d4c0b0-46ea-498d-a3a5-56060a3d7a7c";
	private static final String GRAM_UNIT_ID = "c5173731-3a7e-498c-84b1-b2d3abe68cef";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JsonMapper jsonMapper;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Test
	@DisplayName("Given an existing name, When POST an ingredient with it, Then 409 naming the name field")
	void shouldRejectDuplicateNameOnCreate() throws Exception {
		String name = uniqueName();
		create(name);

		mockMvc.perform(post(INGREDIENTS).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(body(name, null)))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.errorCode").value(ApplicationErrorCodes.INGREDIENT_ALREADY_EXISTS.getCode()))
				.andExpect(jsonPath("$.validationExceptions[0].objectName").value("name"))
				.andExpect(jsonPath("$.validationExceptions[0].message").value("An ingredient named '" + name + "' already exists"));
	}

	@Test
	@DisplayName("Given two ingredients, When PUT one with the other's name, Then 409 and the name is unchanged")
	void shouldRejectDuplicateNameOnUpdate() throws Exception {
		String taken = uniqueName();
		create(taken);
		String original = uniqueName();
		String id = create(original);

		mockMvc.perform(put(INGREDIENTS + "/" + id).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(body(taken, null)))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.errorCode").value(ApplicationErrorCodes.INGREDIENT_ALREADY_EXISTS.getCode()))
				.andExpect(jsonPath("$.validationExceptions[0].objectName").value("name"));

		mockMvc.perform(get(INGREDIENTS + "/" + id))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.name").value(original));
	}

	@Test
	@DisplayName("Given an ingredient, When PUT it with its own name, Then 200 - it does not collide with itself")
	void shouldAllowUpdateKeepingItsOwnName() throws Exception {
		String name = uniqueName();
		String id = create(name);

		mockMvc.perform(put(INGREDIENTS + "/" + id).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(body(name, "Changed")))
				.andExpect(status().isOk());

		mockMvc.perform(get(INGREDIENTS + "/" + id))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.description").value("Changed"));
	}

	@Test
	@DisplayName("Given a recipe uses an ingredient, When DELETE it, Then 409 and the ingredient still exists")
	void shouldRejectDeleteOfIngredientInUse() throws Exception {
		String name = uniqueName();
		String id = create(name);
		// Inserted directly: recipe writes through the API are Part 7's to prove.
		String recipeId = UUID.randomUUID().toString();
		jdbcTemplate.update("INSERT INTO recipe (id, name, created, created_by, category_id) VALUES (?, ?, CURRENT_TIMESTAMP, 'it-user', ?)",
				recipeId, "Recipe with " + name, BROED_CATEGORY_ID);
		jdbcTemplate.update("INSERT INTO recipe_ingredient (recipe_id, ingredient_id, unit_id, amount, created, created_by) VALUES (?, ?, ?, 1, CURRENT_TIMESTAMP, 'it-user')",
				recipeId, id, GRAM_UNIT_ID);

		mockMvc.perform(delete(INGREDIENTS + "/" + id).with(csrf()))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.errorCode").value(ApplicationErrorCodes.INGREDIENT_IN_USE.getCode()))
				.andExpect(jsonPath("$.message").value("Ingredient '" + name + "' is used by 1 recipe and cannot be deleted"));

		mockMvc.perform(get(INGREDIENTS + "/" + id))
				.andExpect(status().isOk());
	}

	@Test
	@DisplayName("Given an unused ingredient, When DELETE it, Then 204 and it is gone")
	void shouldDeleteUnusedIngredient() throws Exception {
		String id = create(uniqueName());

		mockMvc.perform(delete(INGREDIENTS + "/" + id).with(csrf()))
				.andExpect(status().isNoContent());

		mockMvc.perform(get(INGREDIENTS + "/" + id))
				.andExpect(status().isNotFound());
	}

	private String create(String name) throws Exception {
		String response = mockMvc.perform(post(INGREDIENTS).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(body(name, null)))
				.andExpect(status().isCreated())
				.andReturn().getResponse().getContentAsString();
		// Only the id is read: IngredientDTO cannot deserialize the "yyyy-MM-dd HH:mm" dates it serializes.
		return jsonMapper.readTree(response).get("id").asString();
	}

	private String body(String name, String description) {
		return jsonMapper.writeValueAsString(IngredientDTO.builder().name(name).description(description).build());
	}

	/** Every test makes its own rows: the database lives as long as the cached application context. */
	private static String uniqueName() {
		return "Ingredient " + UUID.randomUUID().toString().substring(0, 8);
	}
}
