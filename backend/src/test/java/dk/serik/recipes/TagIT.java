package dk.serik.recipes;

import dk.serik.recipes.dto.TagDTO;
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
 * Tag writes through the whole stack, against the database's own constraints - see
 * {@code CategoryIT}, which this mirrors. Tag names are unique: the schema has always said so,
 * whatever earlier comments claimed. A tag is referenced from {@code recipe_tag}.
 */
@SpringBootTest(classes = RecipesApplication.class)
@AutoConfigureMockMvc
@AutoConfigureTestDatabase
@WithMockUser(username = "it-user")
class TagIT {

	private static final String TAGS = "/api/v1/tags";

	// Seeded by db.changelog_1.1.xml, which the tests load too.
	private static final String BROED_CATEGORY_ID = "14d4c0b0-46ea-498d-a3a5-56060a3d7a7c";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JsonMapper jsonMapper;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Test
	@DisplayName("Given an existing name, When POST a tag with it, Then 409 naming the name field")
	void shouldRejectDuplicateNameOnCreate() throws Exception {
		String name = uniqueName();
		create(name);

		mockMvc.perform(post(TAGS).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(body(name)))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.errorCode").value(ApplicationErrorCodes.TAG_ALREADY_EXISTS.getCode()))
				.andExpect(jsonPath("$.validationExceptions[0].objectName").value("name"))
				.andExpect(jsonPath("$.validationExceptions[0].message").value("A tag named '" + name + "' already exists"));
	}

	@Test
	@DisplayName("Given two tags, When PUT one with the other's name, Then 409 and the name is unchanged")
	void shouldRejectDuplicateNameOnUpdate() throws Exception {
		String taken = uniqueName();
		create(taken);
		String original = uniqueName();
		String id = create(original);

		mockMvc.perform(put(TAGS + "/" + id).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(body(taken)))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.errorCode").value(ApplicationErrorCodes.TAG_ALREADY_EXISTS.getCode()))
				.andExpect(jsonPath("$.validationExceptions[0].objectName").value("name"));

		mockMvc.perform(get(TAGS + "/" + id))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.name").value(original));
	}

	@Test
	@DisplayName("Given a tag, When PUT it with its own name, Then 200 - it does not collide with itself")
	void shouldAllowUpdateKeepingItsOwnName() throws Exception {
		String name = uniqueName();
		String id = create(name);

		mockMvc.perform(put(TAGS + "/" + id).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(body(name)))
				.andExpect(status().isOk());
	}

	@Test
	@DisplayName("Given a recipe is tagged with a tag, When DELETE it, Then 409 and the tag still exists")
	void shouldRejectDeleteOfTagInUse() throws Exception {
		String name = uniqueName();
		String id = create(name);
		// Inserted directly: recipe writes through the API are Part 7's to prove.
		String recipeId = UUID.randomUUID().toString();
		jdbcTemplate.update("INSERT INTO recipe (id, name, created, created_by, category_id) VALUES (?, ?, CURRENT_TIMESTAMP, 'it-user', ?)",
				recipeId, "Recipe with " + name, BROED_CATEGORY_ID);
		jdbcTemplate.update("INSERT INTO recipe_tag (recipe_id, tag_id, created, created_by) VALUES (?, ?, CURRENT_TIMESTAMP, 'it-user')",
				recipeId, id);

		mockMvc.perform(delete(TAGS + "/" + id).with(csrf()))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.errorCode").value(ApplicationErrorCodes.TAG_IN_USE.getCode()))
				.andExpect(jsonPath("$.message").value("Tag '" + name + "' is used by 1 recipe and cannot be deleted"));

		mockMvc.perform(get(TAGS + "/" + id))
				.andExpect(status().isOk());
	}

	@Test
	@DisplayName("Given an unused tag, When DELETE it, Then 204 and it is gone")
	void shouldDeleteUnusedTag() throws Exception {
		String id = create(uniqueName());

		mockMvc.perform(delete(TAGS + "/" + id).with(csrf()))
				.andExpect(status().isNoContent());

		mockMvc.perform(get(TAGS + "/" + id))
				.andExpect(status().isNotFound());
	}

	private String create(String name) throws Exception {
		String response = mockMvc.perform(post(TAGS).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(body(name)))
				.andExpect(status().isCreated())
				.andReturn().getResponse().getContentAsString();
		// Only the id is read: TagDTO cannot deserialize the "yyyy-MM-dd HH:mm" dates it serializes.
		return jsonMapper.readTree(response).get("id").asString();
	}

	private String body(String name) {
		return jsonMapper.writeValueAsString(TagDTO.builder().name(name).build());
	}

	/** Every test makes its own rows: the database lives as long as the cached application context. */
	private static String uniqueName() {
		return "Tag " + UUID.randomUUID().toString().substring(0, 8);
	}
}
