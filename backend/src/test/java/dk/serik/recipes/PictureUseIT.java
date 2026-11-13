package dk.serik.recipes;

import dk.serik.recipes.exceptions.ApplicationErrorCodes;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import tools.jackson.databind.json.JsonMapper;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static dk.serik.recipes.testutil.TestPictures.jpeg;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Where a picture is shown (docs/PLAN_V8.md decisions 2 and 6): on recipes, ingredients and the
 * Welcome page, each by the picture's id, and the delete that is refused while any of them uses it.
 * Assertions are on fresh GETs. Each test removes what it creates and leaves the Welcome page
 * without a picture, as it starts.
 */
@SpringBootTest(classes = RecipesApplication.class)
@AutoConfigureMockMvc
@AutoConfigureTestDatabase
@WithMockUser(username = "it-user")
class PictureUseIT {

	private static final String PICTURES = "/api/v1/pictures";
	private static final String RECIPES = "/api/v1/recipes";
	private static final String INGREDIENTS = "/api/v1/ingredients";
	private static final String WELCOME_PAGE = "/api/v1/welcome-page";

	// Seeded by db.changelog_1.1.xml.
	private static final String BROED = "14d4c0b0-46ea-498d-a3a5-56060a3d7a7c";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JsonMapper jsonMapper;

	@Test
	@DisplayName("Given a picture on a recipe, When the recipe and the picture are read, Then each names the other")
	void shouldLinkARecipe() throws Exception {
		String picture = upload("Rugbrød", "Et rugbrød på et bræt");
		String name = "Billedbrød " + UUID.randomUUID().toString().substring(0, 8);
		String recipe = create(RECIPES, recipe(name, picture));

		mockMvc.perform(get(RECIPES + "/" + recipe))
				.andExpect(jsonPath("$.picture.id").value(picture))
				.andExpect(jsonPath("$.picture.name").value("Rugbrød"))
				.andExpect(jsonPath("$.picture.altText").value("Et rugbrød på et bræt"))
				// nested, a picture does not say where it is used
				.andExpect(jsonPath("$.picture.recipes").doesNotExist());
		mockMvc.perform(get(PICTURES + "/" + picture))
				.andExpect(jsonPath("$.recipes[*].id", contains(recipe)))
				.andExpect(jsonPath("$.recipes[*].name", contains(name)))
				.andExpect(jsonPath("$.ingredients", empty()))
				.andExpect(jsonPath("$.welcomePage").value(false));

		remove(RECIPES, recipe);
		remove(PICTURES, picture);
	}

	@Test
	@DisplayName("Given a recipe with a picture, When PUT without one, Then the picture is unlinked and stays in the library")
	void shouldUnlinkARecipe() throws Exception {
		String picture = upload("Rugbrød", "Et rugbrød");
		String name = "Billedbrød " + UUID.randomUUID().toString().substring(0, 8);
		String recipe = create(RECIPES, recipe(name, picture));

		mockMvc.perform(json(put(RECIPES + "/" + recipe), recipe(name, null))).andExpect(status().isOk());

		mockMvc.perform(get(RECIPES + "/" + recipe)).andExpect(jsonPath("$.picture").doesNotExist());
		mockMvc.perform(get(PICTURES + "/" + picture)).andExpect(jsonPath("$.recipes", empty()));

		remove(RECIPES, recipe);
		remove(PICTURES, picture);
	}

	@Test
	@DisplayName("Given a recipe with a picture, When the recipe is deleted, Then the picture stays in the library")
	void shouldKeepThePictureOfADeletedRecipe() throws Exception {
		String picture = upload("Rugbrød", "Et rugbrød");
		String recipe = create(RECIPES, recipe("Billedbrød " + UUID.randomUUID().toString().substring(0, 8), picture));

		remove(RECIPES, recipe);

		mockMvc.perform(get(PICTURES + "/" + picture))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.recipes", empty()));
		remove(PICTURES, picture);
	}

	@Test
	@DisplayName("Given one picture on two ingredients, When the picture is read, Then it names both, in order of name")
	void shouldShareAPictureBetweenIngredients() throws Exception {
		String picture = upload("Mel", "En sæk mel");
		String suffix = UUID.randomUUID().toString().substring(0, 8);
		String second = create(INGREDIENTS, ingredient("Billedmel B " + suffix, picture));
		String first = create(INGREDIENTS, ingredient("Billedmel A " + suffix, picture));

		mockMvc.perform(get(INGREDIENTS + "/" + first)).andExpect(jsonPath("$.picture.id").value(picture));
		mockMvc.perform(get(PICTURES + "/" + picture))
				.andExpect(jsonPath("$.ingredients[*].id", contains(first, second)))
				.andExpect(jsonPath("$.recipes", empty()));

		mockMvc.perform(json(put(INGREDIENTS + "/" + first), ingredient("Billedmel A " + suffix, null))).andExpect(status().isOk());
		mockMvc.perform(get(INGREDIENTS + "/" + first)).andExpect(jsonPath("$.picture").doesNotExist());
		mockMvc.perform(get(PICTURES + "/" + picture)).andExpect(jsonPath("$.ingredients[*].id", contains(second)));

		remove(INGREDIENTS, first);
		remove(INGREDIENTS, second);
		remove(PICTURES, picture);
	}

	@Test
	@DisplayName("Given an unknown picture's id, When a recipe or an ingredient is written with it, Then 400 on the field picture")
	void shouldRefuseAnUnknownPicture() throws Exception {
		String unknown = UUID.randomUUID().toString();
		String suffix = UUID.randomUUID().toString().substring(0, 8);

		mockMvc.perform(json(post(RECIPES), recipe("Billedbrød " + suffix, unknown)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errorCode").value(ApplicationErrorCodes.PICTURE_NOT_FOUND.getCode()))
				.andExpect(jsonPath("$.validationExceptions[0].objectName").value("picture"));
		mockMvc.perform(json(post(INGREDIENTS), ingredient("Billedmel " + suffix, unknown)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.validationExceptions[0].objectName").value("picture"));
		mockMvc.perform(get(RECIPES).param("name", "Billedbrød " + suffix)).andExpect(jsonPath("$", empty()));
		mockMvc.perform(get(INGREDIENTS).param("name", "Billedmel " + suffix)).andExpect(jsonPath("$", empty()));
	}

	@Test
	@DisplayName("Given the Welcome page, When its picture is set and then removed, Then a fresh GET shows each, and the picture says so")
	void shouldSetAndRemoveTheWelcomePagePicture() throws Exception {
		String picture = upload("Køkkenet", "Et køkken i morgenlys");

		mockMvc.perform(get(WELCOME_PAGE)).andExpect(jsonPath("$.picture").isEmpty());
		try {
			mockMvc.perform(json(put(WELCOME_PAGE), welcomePage(picture))).andExpect(status().isOk());
			mockMvc.perform(get(WELCOME_PAGE))
					.andExpect(jsonPath("$.picture.id").value(picture))
					.andExpect(jsonPath("$.picture.altText").value("Et køkken i morgenlys"));
			mockMvc.perform(get(PICTURES + "/" + picture)).andExpect(jsonPath("$.welcomePage").value(true));
		} finally {
			mockMvc.perform(json(put(WELCOME_PAGE), welcomePage(null))).andExpect(status().isOk());
		}

		mockMvc.perform(get(WELCOME_PAGE)).andExpect(jsonPath("$.picture").isEmpty());
		mockMvc.perform(get(PICTURES + "/" + picture)).andExpect(jsonPath("$.welcomePage").value(false));
		remove(PICTURES, picture);
	}

	@Test
	@DisplayName("Given an unknown picture's id, When the Welcome page is given it, Then 400 on the field picture")
	void shouldRefuseAnUnknownPictureOnTheWelcomePage() throws Exception {
		mockMvc.perform(json(put(WELCOME_PAGE), welcomePage(UUID.randomUUID().toString())))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.validationExceptions[0].objectName").value("picture"));
		mockMvc.perform(get(WELCOME_PAGE)).andExpect(jsonPath("$.picture").isEmpty());
	}

	@Test
	@DisplayName("Given a picture on a recipe, an ingredient and the Welcome page, When deleted, Then 409 naming each, until all three let go")
	void shouldRefuseDeletingAPictureInUse() throws Exception {
		String picture = upload("Rugbrød", "Et rugbrød");
		String suffix = UUID.randomUUID().toString().substring(0, 8);
		String recipe = create(RECIPES, recipe("Billedbrød " + suffix, picture));
		String ingredient = create(INGREDIENTS, ingredient("Billedmel " + suffix, picture));
		try {
			mockMvc.perform(json(put(WELCOME_PAGE), welcomePage(picture))).andExpect(status().isOk());

			mockMvc.perform(delete(PICTURES + "/" + picture).with(csrf()))
					.andExpect(status().isConflict())
					.andExpect(jsonPath("$.errorCode").value(ApplicationErrorCodes.PICTURE_IN_USE.getCode()))
					.andExpect(jsonPath("$.message").value(
							"Picture 'Rugbrød' is used by 1 recipe, 1 ingredient and the Welcome page and cannot be deleted"));
		} finally {
			mockMvc.perform(json(put(WELCOME_PAGE), welcomePage(null))).andExpect(status().isOk());
		}
		mockMvc.perform(delete(PICTURES + "/" + picture).with(csrf()))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.message").value("Picture 'Rugbrød' is used by 1 recipe and 1 ingredient and cannot be deleted"));

		remove(RECIPES, recipe);
		remove(INGREDIENTS, ingredient);
		mockMvc.perform(get(PICTURES + "/" + picture)).andExpect(status().isOk());
		remove(PICTURES, picture);
	}

	private static Map<String, Object> recipe(String name, String picture) {
		Map<String, Object> body = new HashMap<>();
		body.put("name", name);
		body.put("category", Map.of("id", BROED));
		if (picture != null) {
			body.put("picture", Map.of("id", picture));
		}
		return body;
	}

	private static Map<String, Object> ingredient(String name, String picture) {
		Map<String, Object> body = new HashMap<>();
		body.put("name", name);
		if (picture != null) {
			body.put("picture", Map.of("id", picture));
		}
		return body;
	}

	private static Map<String, Object> welcomePage(String picture) {
		Map<String, Object> body = new HashMap<>();
		body.put("picture", picture == null ? null : Map.of("id", picture));
		return body;
	}

	private String upload(String name, String altText) throws Exception {
		String response = mockMvc.perform(multipart(PICTURES)
						.file(new MockMultipartFile("file", "picture.jpg", MediaType.IMAGE_JPEG_VALUE, jpeg(400, 300)))
						.param("name", name)
						.param("altText", altText)
						.with(csrf()))
				.andExpect(status().isCreated())
				.andReturn().getResponse().getContentAsString();
		return jsonMapper.readTree(response).get("id").asString();
	}

	private String create(String path, Map<String, Object> body) throws Exception {
		String response = mockMvc.perform(json(post(path), body))
				.andExpect(status().isCreated())
				.andReturn().getResponse().getContentAsString();
		return jsonMapper.readTree(response).get("id").asString();
	}

	private void remove(String path, String id) throws Exception {
		mockMvc.perform(delete(path + "/" + id).with(csrf())).andExpect(status().isNoContent());
	}

	private MockHttpServletRequestBuilder json(MockHttpServletRequestBuilder request, Object body) {
		return request.with(csrf()).contentType(MediaType.APPLICATION_JSON).content(jsonMapper.writeValueAsString(body));
	}
}
