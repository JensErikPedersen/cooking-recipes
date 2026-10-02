package dk.serik.recipes;

import dk.serik.recipes.dto.CategoryDTO;
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
 * Category writes through the whole stack: controller, service, repository and the database's own
 * constraints. The slice tests mock the layer beneath, so a unique or foreign key violation - which
 * only the database raises, and only when the transaction commits - is visible here and nowhere else.
 * <p>
 * Sign-in and the real CSRF cookie are {@code AuthenticationIT}'s concern; here a mock user and
 * MockMvc's {@code csrf()} stand in for them. No test runs in a transaction of its own, since one
 * would postpone the commit, and with it the violation, past the response being asserted.
 */
@SpringBootTest(classes = RecipesApplication.class)
@AutoConfigureMockMvc
@AutoConfigureTestDatabase
@WithMockUser(username = "it-user")
class CategoryIT {

	private static final String CATEGORIES = "/api/v1/categories";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JsonMapper jsonMapper;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Test
	@DisplayName("Given an existing name, When POST a category with it, Then 409 naming the name field")
	void shouldRejectDuplicateNameOnCreate() throws Exception {
		String name = uniqueName();
		create(name);

		mockMvc.perform(post(CATEGORIES).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(body(name)))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.errorCode").value(ApplicationErrorCodes.CATEGORY_ALREADY_EXISTS.getCode()))
				.andExpect(jsonPath("$.validationExceptions[0].objectName").value("name"))
				.andExpect(jsonPath("$.validationExceptions[0].message").value("A category named '" + name + "' already exists"));
	}

	@Test
	@DisplayName("Given two categories, When PUT one with the other's name, Then 409 and the name is unchanged")
	void shouldRejectDuplicateNameOnUpdate() throws Exception {
		String taken = uniqueName();
		create(taken);
		String original = uniqueName();
		String id = create(original);

		mockMvc.perform(put(CATEGORIES + "/" + id).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(body(taken)))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.errorCode").value(ApplicationErrorCodes.CATEGORY_ALREADY_EXISTS.getCode()))
				.andExpect(jsonPath("$.validationExceptions[0].objectName").value("name"));

		mockMvc.perform(get(CATEGORIES + "/" + id))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.name").value(original));
	}

	@Test
	@DisplayName("Given a category, When PUT it with its own name, Then 200 - it does not collide with itself")
	void shouldAllowUpdateKeepingItsOwnName() throws Exception {
		String name = uniqueName();
		String id = create(name);

		mockMvc.perform(put(CATEGORIES + "/" + id).with(csrf()).contentType(MediaType.APPLICATION_JSON)
						.content(jsonMapper.writeValueAsString(CategoryDTO.builder().name(name).description("Changed").build())))
				.andExpect(status().isOk());

		mockMvc.perform(get(CATEGORIES + "/" + id))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.description").value("Changed"));
	}

	@Test
	@DisplayName("Given a category a recipe uses, When DELETE it, Then 409 and the category still exists")
	void shouldRejectDeleteOfCategoryInUse() throws Exception {
		String name = uniqueName();
		String id = create(name);
		// No recipe is created through the API: that path is Part 7's to prove, and this test is
		// about the category's foreign key only.
		jdbcTemplate.update("INSERT INTO recipe (id, name, created, created_by, category_id) VALUES (?, ?, CURRENT_TIMESTAMP, 'it-user', ?)",
				UUID.randomUUID().toString(), "Recipe of " + name, id);

		mockMvc.perform(delete(CATEGORIES + "/" + id).with(csrf()))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.errorCode").value(ApplicationErrorCodes.CATEGORY_IN_USE.getCode()))
				.andExpect(jsonPath("$.message").value("Category '" + name + "' is used by 1 recipe and cannot be deleted"));

		mockMvc.perform(get(CATEGORIES + "/" + id))
				.andExpect(status().isOk());
	}

	@Test
	@DisplayName("Given an unused category, When DELETE it, Then 204 and it is gone")
	void shouldDeleteUnusedCategory() throws Exception {
		String id = create(uniqueName());

		mockMvc.perform(delete(CATEGORIES + "/" + id).with(csrf()))
				.andExpect(status().isNoContent());

		mockMvc.perform(get(CATEGORIES + "/" + id))
				.andExpect(status().isNotFound());
	}

	private String create(String name) throws Exception {
		String response = mockMvc.perform(post(CATEGORIES).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(body(name)))
				.andExpect(status().isCreated())
				.andReturn().getResponse().getContentAsString();
		// Only the id is read: CategoryDTO cannot deserialize the "yyyy-MM-dd HH:mm" dates it serializes.
		return jsonMapper.readTree(response).get("id").asString();
	}

	private String body(String name) {
		return jsonMapper.writeValueAsString(CategoryDTO.builder().name(name).build());
	}

	/** Every test makes its own rows: the database lives as long as the cached application context. */
	private static String uniqueName() {
		return "Category " + UUID.randomUUID().toString().substring(0, 8);
	}
}
