package dk.serik.recipes;

import dk.serik.recipes.dto.UnitDTO;
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

import java.util.List;
import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Unit writes through the whole stack, against the database's own constraints - see
 * {@code CategoryIT}, which this mirrors. A unit is referenced from {@code recipe_ingredient}, where
 * one recipe can use it on several lines, so the in-use count is of recipes, not of lines.
 */
@SpringBootTest(classes = RecipesApplication.class)
@AutoConfigureMockMvc
@AutoConfigureTestDatabase
@WithMockUser(username = "it-user")
class UnitIT {

	private static final String UNITS = "/api/v1/units";

	// Seeded by db.changelog_1.1.xml, which the tests load too.
	private static final String BROED_CATEGORY_ID = "14d4c0b0-46ea-498d-a3a5-56060a3d7a7c";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JsonMapper jsonMapper;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Test
	@DisplayName("Given an existing name, When POST a unit with it, Then 409 naming the name field")
	void shouldRejectDuplicateNameOnCreate() throws Exception {
		String name = uniqueName();
		create(name);

		mockMvc.perform(post(UNITS).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(body(name, "x")))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.errorCode").value(ApplicationErrorCodes.UNIT_ALREADY_EXISTS.getCode()))
				.andExpect(jsonPath("$.validationExceptions[0].objectName").value("name"))
				.andExpect(jsonPath("$.validationExceptions[0].message").value("A unit named '" + name + "' already exists"));
	}

	@Test
	@DisplayName("Given two units, When PUT one with the other's name, Then 409 and the name is unchanged")
	void shouldRejectDuplicateNameOnUpdate() throws Exception {
		String taken = uniqueName();
		create(taken);
		String original = uniqueName();
		String id = create(original);

		mockMvc.perform(put(UNITS + "/" + id).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(body(taken, "x")))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.errorCode").value(ApplicationErrorCodes.UNIT_ALREADY_EXISTS.getCode()))
				.andExpect(jsonPath("$.validationExceptions[0].objectName").value("name"));

		mockMvc.perform(get(UNITS + "/" + id))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.name").value(original));
	}

	@Test
	@DisplayName("Given a unit, When PUT it with its own name, Then 200 - it does not collide with itself")
	void shouldAllowUpdateKeepingItsOwnName() throws Exception {
		String name = uniqueName();
		String id = create(name);

		mockMvc.perform(put(UNITS + "/" + id).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(body(name, "new")))
				.andExpect(status().isOk());

		mockMvc.perform(get(UNITS + "/" + id))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.label").value("new"));
	}

	@Test
	@DisplayName("Given one recipe uses a unit on two lines, When DELETE it, Then 409 counting one recipe, and the unit still exists")
	void shouldRejectDeleteOfUnitInUse() throws Exception {
		String name = uniqueName();
		String id = create(name);
		// Inserted directly: recipe writes through the API are Part 7's to prove.
		String recipeId = UUID.randomUUID().toString();
		jdbcTemplate.update("INSERT INTO recipe (id, name, created, created_by, category_id) VALUES (?, ?, CURRENT_TIMESTAMP, 'it-user', ?)",
				recipeId, "Recipe with " + name, BROED_CATEGORY_ID);
		List<String> ingredientIds = jdbcTemplate.queryForList("SELECT id FROM ingredient ORDER BY id LIMIT 2", String.class);
		for (String ingredientId : ingredientIds) {
			jdbcTemplate.update("INSERT INTO recipe_ingredient (recipe_id, ingredient_id, unit_id, amount, created, created_by) VALUES (?, ?, ?, 1, CURRENT_TIMESTAMP, 'it-user')",
					recipeId, ingredientId, id);
		}

		mockMvc.perform(delete(UNITS + "/" + id).with(csrf()))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.errorCode").value(ApplicationErrorCodes.UNIT_IN_USE.getCode()))
				.andExpect(jsonPath("$.message").value("Unit '" + name + "' is used by 1 recipe and cannot be deleted"));

		mockMvc.perform(get(UNITS + "/" + id))
				.andExpect(status().isOk());
	}

	@Test
	@DisplayName("Given an unused unit, When DELETE it, Then 204 and it is gone")
	void shouldDeleteUnusedUnit() throws Exception {
		String id = create(uniqueName());

		mockMvc.perform(delete(UNITS + "/" + id).with(csrf()))
				.andExpect(status().isNoContent());

		mockMvc.perform(get(UNITS + "/" + id))
				.andExpect(status().isNotFound());
	}

	private String create(String name) throws Exception {
		String response = mockMvc.perform(post(UNITS).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(body(name, "x")))
				.andExpect(status().isCreated())
				.andReturn().getResponse().getContentAsString();
		// Only the id is read: UnitDTO cannot deserialize the "yyyy-MM-dd HH:mm" dates it serializes.
		return jsonMapper.readTree(response).get("id").asString();
	}

	private String body(String name, String label) {
		return jsonMapper.writeValueAsString(UnitDTO.builder().name(name).label(label).build());
	}

	/** Every test makes its own rows: the database lives as long as the cached application context. */
	private static String uniqueName() {
		return "Unit " + UUID.randomUUID().toString().substring(0, 8);
	}
}
