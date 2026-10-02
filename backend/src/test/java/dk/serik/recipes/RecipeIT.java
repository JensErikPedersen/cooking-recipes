package dk.serik.recipes;

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
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Recipe writes through the whole stack: the seam where a recipe's category, tags and ingredient
 * lines are assembled into {@code recipe}, {@code recipe_tag} and {@code recipe_ingredient} rows.
 * Every other recipe test mocks the layer beneath it, so a relation dropped here passes them all.
 * <p>
 * Assertions are on a separate GET, or on the join tables themselves - never on the object just
 * posted, which would only echo the request. Bodies are plain maps carrying ids only, as the
 * frontend sends them.
 */
@SpringBootTest(classes = RecipesApplication.class)
@AutoConfigureMockMvc
@AutoConfigureTestDatabase
@WithMockUser(username = "it-user")
class RecipeIT {

	private static final String RECIPES = "/api/v1/recipes";

	// Seeded by db.changelog_1.1.xml, which the tests load too.
	private static final String BROED = "14d4c0b0-46ea-498d-a3a5-56060a3d7a7c";
	private static final String HOVEDRET = "c5994e2b-93fa-43df-9cd7-90d8d4c9dcc0";
	private static final String SPICY = "17a6448b-4efe-43f4-970b-8bf1ce2b754e";
	private static final String THAI = "1b24e657-01e7-4279-a6c4-4aa7840d771c";
	private static final String HVEDEMEL = "549ab6e6-f2d8-4ab3-8ba8-6bc7af82f2fb";
	private static final String SALT = "e0aa2252-c5f1-4c87-b42c-9dd10486f366";
	private static final String VAND = "e00aec55-2eb7-4eeb-a594-0bbb948f09c1";
	private static final String GRAM = "c5173731-3a7e-498c-84b1-b2d3abe68cef";
	private static final String DECILITER = "b9cef3df-4bb5-49ab-8bde-5848d1363bce";
	private static final String TESKE = "615ba803-966f-43e5-8d2a-d38b5198a421";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JsonMapper jsonMapper;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Test
	@DisplayName("Given a category, two tags and three ingredients, When POST, Then a fresh GET has every relation with its amount and unit")
	void shouldKeepEveryRelationOnCreate() throws Exception {
		String name = uniqueName();
		String id = create(name);

		mockMvc.perform(get(RECIPES + "/" + id))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.name").value(name))
				.andExpect(jsonPath("$.instructions").value("Mix and bake"))
				.andExpect(jsonPath("$.category.id").value(BROED))
				.andExpect(jsonPath("$.tags[*].id", containsInAnyOrder(SPICY, THAI)))
				.andExpect(jsonPath("$.recipeIngredients", hasSize(3)))
				.andExpect(jsonPath("$.recipeIngredients[?(@.ingredientId == '" + HVEDEMEL + "')].amount", contains(500.0)))
				.andExpect(jsonPath("$.recipeIngredients[?(@.ingredientId == '" + HVEDEMEL + "')].unitId", contains(GRAM)))
				.andExpect(jsonPath("$.recipeIngredients[?(@.ingredientId == '" + VAND + "')].amount", contains(3.5)))
				.andExpect(jsonPath("$.recipeIngredients[?(@.ingredientId == '" + VAND + "')].unitId", contains(DECILITER)))
				.andExpect(jsonPath("$.recipeIngredients[?(@.ingredientId == '" + SALT + "')].amount", contains(2.0)))
				.andExpect(jsonPath("$.recipeIngredients[?(@.ingredientId == '" + SALT + "')].unitId", contains(TESKE)));

		// The rows themselves, not just the response built from them.
		assertThat(jdbcTemplate.queryForList("SELECT tag_id FROM recipe_tag WHERE recipe_id = ?", String.class, id))
				.containsExactlyInAnyOrder(SPICY, THAI);
	}

	@Test
	@DisplayName("Given a recipe, When PUT a new category, one tag dropped, one ingredient changed and one removed, Then a fresh GET has exactly that")
	void shouldApplyEveryRelationChangeOnUpdate() throws Exception {
		String name = uniqueName();
		String id = create(name);

		Map<String, Object> changed = Map.of(
				"name", name,
				"instructions", "Mix and bake longer",
				"category", Map.of("id", HOVEDRET),
				"tags", List.of(Map.of("id", THAI)),
				"recipeIngredients", List.of(
						line(HVEDEMEL, 600, GRAM),
						line(VAND, 4, TESKE)));  // Salt removed; Vand's amount and unit changed
		mockMvc.perform(put(RECIPES + "/" + id).with(csrf()).contentType(MediaType.APPLICATION_JSON)
						.content(jsonMapper.writeValueAsString(changed)))
				.andExpect(status().isOk());

		mockMvc.perform(get(RECIPES + "/" + id))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.instructions").value("Mix and bake longer"))
				.andExpect(jsonPath("$.category.id").value(HOVEDRET))
				.andExpect(jsonPath("$.tags[*].id", contains(THAI)))
				.andExpect(jsonPath("$.recipeIngredients", hasSize(2)))
				.andExpect(jsonPath("$.recipeIngredients[?(@.ingredientId == '" + HVEDEMEL + "')].amount", contains(600.0)))
				.andExpect(jsonPath("$.recipeIngredients[?(@.ingredientId == '" + VAND + "')].amount", contains(4.0)))
				.andExpect(jsonPath("$.recipeIngredients[?(@.ingredientId == '" + VAND + "')].unitId", contains(TESKE)));
	}

	@Test
	@DisplayName("Given a recipe, When DELETE, Then its join rows go with it and its ingredients and tags remain")
	void shouldRemoveJoinRowsButNotTheirTargetsOnDelete() throws Exception {
		String id = create(uniqueName());

		mockMvc.perform(delete(RECIPES + "/" + id).with(csrf()))
				.andExpect(status().isNoContent());

		mockMvc.perform(get(RECIPES + "/" + id))
				.andExpect(status().isNotFound());
		assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM recipe_ingredient WHERE recipe_id = ?", Integer.class, id)).isZero();
		assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM recipe_tag WHERE recipe_id = ?", Integer.class, id)).isZero();
		mockMvc.perform(get("/api/v1/ingredients/" + HVEDEMEL)).andExpect(status().isOk());
		mockMvc.perform(get("/api/v1/tags/" + SPICY)).andExpect(status().isOk());
	}

	@Test
	@DisplayName("Given an existing name, When POST a recipe with it, Then 409 naming the name field")
	void shouldRejectDuplicateName() throws Exception {
		String name = uniqueName();
		create(name);

		mockMvc.perform(post(RECIPES).with(csrf()).contentType(MediaType.APPLICATION_JSON)
						.content(jsonMapper.writeValueAsString(recipe(name))))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.errorCode").value(ApplicationErrorCodes.RECIPE_ALREADY_EXISTS.getCode()))
				.andExpect(jsonPath("$.validationExceptions[0].objectName").value("name"))
				.andExpect(jsonPath("$.validationExceptions[0].message").value("A recipe named '" + name + "' already exists"));
	}

	@Test
	@DisplayName("Given a recipe, When PUT it with its own name, Then 200 - it does not collide with itself")
	void shouldAllowUpdateKeepingItsOwnName() throws Exception {
		String name = uniqueName();
		String id = create(name);

		mockMvc.perform(put(RECIPES + "/" + id).with(csrf()).contentType(MediaType.APPLICATION_JSON)
						.content(jsonMapper.writeValueAsString(recipe(name))))
				.andExpect(status().isOk());
	}

	/** POSTs the standard recipe and returns its id. */
	private String create(String name) throws Exception {
		String response = mockMvc.perform(post(RECIPES).with(csrf()).contentType(MediaType.APPLICATION_JSON)
						.content(jsonMapper.writeValueAsString(recipe(name))))
				.andExpect(status().isCreated())
				.andReturn().getResponse().getContentAsString();
		return jsonMapper.readTree(response).get("id").asString();
	}

	/** Brød, tagged Spicy and Thai, with 500 g Hvedemel, 3.5 dl Vand and 2 tsk Salt. */
	private static Map<String, Object> recipe(String name) {
		return Map.of(
				"name", name,
				"instructions", "Mix and bake",
				"category", Map.of("id", BROED),
				"tags", List.of(Map.of("id", SPICY), Map.of("id", THAI)),
				"recipeIngredients", List.of(
						line(HVEDEMEL, 500, GRAM),
						line(VAND, 3.5, DECILITER),
						line(SALT, 2, TESKE)));
	}

	private static Map<String, Object> line(String ingredientId, Number amount, String unitId) {
		return Map.of("ingredientId", ingredientId, "amount", amount, "unitId", unitId);
	}

	/** Every test makes its own rows: the database lives as long as the cached application context. */
	private static String uniqueName() {
		return "Recipe " + UUID.randomUUID().toString().substring(0, 8);
	}
}
