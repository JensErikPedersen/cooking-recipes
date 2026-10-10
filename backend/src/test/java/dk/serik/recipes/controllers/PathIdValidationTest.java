package dk.serik.recipes.controllers;

import dk.serik.recipes.exceptions.ApplicationErrorCodes;
import dk.serik.recipes.service.CategoryService;
import dk.serik.recipes.service.IngredientService;
import dk.serik.recipes.service.RecipeService;
import dk.serik.recipes.service.TagService;
import dk.serik.recipes.service.UnitService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasItem;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Every path id is a {@code @UUID}, so a malformed one is refused at the web boundary - before any
 * service runs - with the same envelope a body validation failure gets.
 * <p>
 * It also guards the side effect: once a parameter carries a constraint, Spring validates a
 * {@code @Valid} body as part of method validation, and its errors arrive through a different
 * exception than a POST's. They must still be reported on the field.
 */
@WebMvcTest({CategoryController.class, IngredientController.class, RecipeController.class, TagController.class,
        UnitController.class})
// The security chain is AuthenticationIT's business; these tests are about the controllers.
@AutoConfigureMockMvc(addFilters = false)
public class PathIdValidationTest {

    private static final String ID = "913a5159-3717-4b9d-a290-0158d31ea8aa";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CategoryService categoryService;

    @MockitoBean
    private IngredientService ingredientService;

    @MockitoBean
    private RecipeService recipeService;

    @MockitoBean
    private TagService tagService;

    @MockitoBean
    private UnitService unitService;

    @ParameterizedTest(name = "{0} {1}")
    @CsvSource(delimiter = '|', value = {
            "GET    | /api/v1/categories/not-a-uuid  |",
            "PUT    | /api/v1/categories/not-a-uuid  | {\"name\":\"Dessert\"}",
            "DELETE | /api/v1/categories/not-a-uuid  |",
            "GET    | /api/v1/ingredients/not-a-uuid |",
            "PUT    | /api/v1/ingredients/not-a-uuid | {\"name\":\"Salt\"}",
            "DELETE | /api/v1/ingredients/not-a-uuid |",
            "GET    | /api/v1/recipes/not-a-uuid     |",
            "PUT    | /api/v1/recipes/not-a-uuid     | {\"name\":\"Brød\"}",
            "DELETE | /api/v1/recipes/not-a-uuid     |",
            "GET    | /api/v1/tags/not-a-uuid        |",
            "PUT    | /api/v1/tags/not-a-uuid        | {\"name\":\"Hurtig\"}",
            "DELETE | /api/v1/tags/not-a-uuid        |",
            "GET    | /api/v1/units/not-a-uuid       |",
            "PUT    | /api/v1/units/not-a-uuid       | {\"name\":\"Gram\",\"label\":\"g\"}",
            "DELETE | /api/v1/units/not-a-uuid       |",
    })
    @DisplayName("Given a malformed id, When the request names it, Then 400 on the field id and no service is called")
    public void shouldRejectMalformedId(String method, String path, String body) throws Exception {
        perform(method, path, body)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value(ApplicationErrorCodes.VALIDATION_EXCEPTION.getCode()))
                .andExpect(jsonPath("$.validationExceptions[*].objectName", containsInAnyOrder("id")))
                .andExpect(jsonPath("$.validationExceptions[0].message").value("not-a-uuid is not a valid UUID"));

        verifyNoInteractions(categoryService, ingredientService, recipeService, tagService, unitService);
    }

    @ParameterizedTest(name = "PUT {0} -> {1}")
    @CsvSource(delimiter = '|', value = {
            "/api/v1/categories/" + ID + "                    | name",
            "/api/v1/ingredients/" + ID + "                   | name",
            "/api/v1/recipes/" + ID + "                       | name",
            "/api/v1/tags/" + ID + "                          | name",
            "/api/v1/units/" + ID + "                         | label",
    })
    @DisplayName("Given a well-formed id and an invalid body, When PUT, Then 400 on the body's field, not on the parameter")
    public void shouldReportPutBodyErrorsOnTheField(String path, String field) throws Exception {
        perform("PUT", path, "{}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value(ApplicationErrorCodes.VALIDATION_EXCEPTION.getCode()))
                .andExpect(jsonPath("$.validationExceptions[*].objectName", hasItem(field)));

        verifyNoInteractions(categoryService, ingredientService, recipeService, tagService, unitService);
    }

    @Test
    @DisplayName("Given a malformed id and an invalid body, When PUT, Then 400 reporting both")
    public void shouldReportMalformedIdAndBodyErrorsTogether() throws Exception {
        perform("PUT", "/api/v1/categories/not-a-uuid", "{}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationExceptions[*].objectName", containsInAnyOrder("id", "name")));

        verifyNoInteractions(categoryService);
    }

    private ResultActions perform(String method, String path, String body) throws Exception {
        var request = request(HttpMethod.valueOf(method), path);
        if (body != null) {
            request.contentType(MediaType.APPLICATION_JSON).content(body);
        }
        return mockMvc.perform(request);
    }
}
