package dk.serik.recipes.controllers;

import tools.jackson.databind.json.JsonMapper;
import dk.serik.recipes.dto.RecipeDTO;
import dk.serik.recipes.dto.RecipeIngredientDTO;
import dk.serik.recipes.dto.RecipeRatingDTO;
import dk.serik.recipes.exceptions.ApplicationErrorCodes;
import dk.serik.recipes.exceptions.ServiceException;
import dk.serik.recipes.mockutil.MockRecipeUtil;
import dk.serik.recipes.service.RecipeService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RecipeController.class)
// The security chain is AuthenticationIT's business; these tests are about the controller.
@AutoConfigureMockMvc(addFilters = false)
public class RecipeControllerTest {

    private static final String BASE = "/api/v1/recipes";
    private static final String ID = "932b9ecc-ae01-47d3-996a-0e83c27f39b7";
    private static final String INGREDIENT_ID = "5f01d434-5a68-4359-9f2e-0a6793dce48d";
    private static final String UNIT_ID = "f7823293-7874-4459-9fb7-6b420a0627fa";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JsonMapper objectMapper;

    @MockitoBean
    private RecipeService recipeService;

    private RecipeDTO aRecipe() {
        return MockRecipeUtil.mockSavedRecipeWheatBreadWithIngredientsAndRatingDTO();
    }

    private String newRecipeJson() throws Exception {
        return objectMapper.writeValueAsString(RecipeDTO.builder()
                .name("Hvedebrød")
                .description("Lækkert brød")
                .instructions("Bland og bag")
                .categoryId("913a5159-3717-4b9d-a290-0158d31ea8ab")
                .categoryName("Brød")
                .build());
    }

    // ------------------------------------------------------------------ recipe CRUD

    @Test
    @DisplayName("Given recipes exist, When GET the collection, Then 200 and the list is returned")
    public void shouldReturnAllRecipes() throws Exception {
        given(recipeService.findAll()).willReturn(List.of(aRecipe()));

        mockMvc.perform(get(BASE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].name").value("Hvedebrød"));
    }

    @Test
    @DisplayName("Given an existing recipe, When GET by id, Then 200 with its category and ingredients")
    public void shouldReturnRecipeById() throws Exception {
        given(recipeService.findById(ID)).willReturn(Optional.of(aRecipe()));

        mockMvc.perform(get(BASE + "/{id}", ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Hvedebrød"))
                .andExpect(jsonPath("$.category.name").value("Brød"))
                .andExpect(jsonPath("$.recipeIngredients").isArray());
    }

    @Test
    @DisplayName("Given a recipe with ratings, When GET by id, Then ratings are returned read-only")
    public void shouldReturnRatingsOnRead() throws Exception {
        given(recipeService.findById(ID)).willReturn(Optional.of(aRecipe()));

        mockMvc.perform(get(BASE + "/{id}", ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recipeRatings").isArray());
    }

    @Test
    @DisplayName("Given an unknown recipe, When GET by id, Then 404 and the error envelope")
    public void shouldReturn404WhenRecipeUnknown() throws Exception {
        given(recipeService.findById(ID)).willReturn(Optional.empty());

        mockMvc.perform(get(BASE + "/{id}", ID))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value(ApplicationErrorCodes.RECIPE_NOT_FOUND.getCode()))
                .andExpect(jsonPath("$.message").value(containsString("could not be found")));
    }

    @Test
    @DisplayName("Given a valid payload, When POST, Then 201 with a Location header")
    public void shouldCreateRecipe() throws Exception {
        given(recipeService.save(any())).willReturn(aRecipe());

        mockMvc.perform(post(BASE).contentType(MediaType.APPLICATION_JSON).content(newRecipeJson()))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", endsWith(BASE + "/" + ID)))
                .andExpect(jsonPath("$.name").value("Hvedebrød"));
    }

    @Test
    @DisplayName("Given a payload with no name, When POST, Then 400 and the service is never called")
    public void shouldRejectCreateWithoutName() throws Exception {
        mockMvc.perform(post(BASE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                RecipeDTO.builder().categoryId("913a5159-3717-4b9d-a290-0158d31ea8ab").build())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value(ApplicationErrorCodes.VALIDATION_EXCEPTION.getCode()))
                .andExpect(jsonPath("$.validationExceptions[0].objectName").value("name"));

        verify(recipeService, never()).save(any());
    }

    @Test
    @DisplayName("Given a payload carrying ratings, When POST, Then the service's 400 is surfaced")
    public void shouldRejectCreateCarryingRatings() throws Exception {
        given(recipeService.save(any())).willThrow(ServiceException.badRequest(
                ApplicationErrorCodes.RECIPE_RATING_NOT_SUPPORTED,
                "Recipe ratings cannot be set through this endpoint in this version"));

        RecipeDTO withRatings = RecipeDTO.builder()
                .name("Hvedebrød")
                .categoryId("913a5159-3717-4b9d-a290-0158d31ea8ab")
                .recipeRatings(Set.of(RecipeRatingDTO.builder().rating(5).build()))
                .build();

        mockMvc.perform(post(BASE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(withRatings)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value(ApplicationErrorCodes.RECIPE_RATING_NOT_SUPPORTED.getCode()))
                .andExpect(jsonPath("$.message").value(containsString("cannot be set through this endpoint")));
    }

    @Test
    @DisplayName("Given a valid payload, When PUT, Then 200 and the path id wins over the body id")
    public void shouldUpdateRecipeUsingPathId() throws Exception {
        given(recipeService.update(any())).willReturn(aRecipe());

        mockMvc.perform(put(BASE + "/{id}", ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(RecipeDTO.builder()
                                .id("00000000-0000-0000-0000-000000000000")   // deliberately different
                                .name("Hvedebrød")
                                .categoryId("913a5159-3717-4b9d-a290-0158d31ea8ab")
                                .build())))
                .andExpect(status().isOk());

        ArgumentCaptor<RecipeDTO> captor = ArgumentCaptor.forClass(RecipeDTO.class);
        verify(recipeService).update(captor.capture());
        assertThat(captor.getValue().getId()).isEqualTo(ID);
    }

    @Test
    @DisplayName("Given an unknown recipe, When PUT, Then the service's 404 is surfaced")
    public void shouldReturn404WhenUpdatingUnknownRecipe() throws Exception {
        given(recipeService.update(any())).willThrow(ServiceException.builder()
                .message("Cannot find recipe with id: " + ID)
                .code(ApplicationErrorCodes.RECIPE_NOT_FOUND.getCode())
                .httpStatus(HttpStatus.NOT_FOUND)
                .build());

        mockMvc.perform(put(BASE + "/{id}", ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(newRecipeJson()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value(ApplicationErrorCodes.RECIPE_NOT_FOUND.getCode()));
    }

    @Test
    @DisplayName("Given an existing recipe, When DELETE, Then 204 with no body")
    public void shouldDeleteRecipe() throws Exception {
        given(recipeService.delete(ID)).willReturn(true);

        mockMvc.perform(delete(BASE + "/{id}", ID))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("Given an unknown recipe, When DELETE, Then 404")
    public void shouldReturn404WhenDeletingUnknownRecipe() throws Exception {
        given(recipeService.delete(ID)).willReturn(false);

        mockMvc.perform(delete(BASE + "/{id}", ID))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value(ApplicationErrorCodes.RECIPE_NOT_FOUND.getCode()));
    }

    // ------------------------------------------------------------------ ingredients sub-resource

    @Test
    @DisplayName("Given a recipe, When POST an ingredient, Then 201 with a Location header and the path recipe id is used")
    public void shouldAddIngredient() throws Exception {
        given(recipeService.addRecipeIngredient(any())).willReturn(aRecipe());

        mockMvc.perform(post(BASE + "/{id}/ingredients", ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(RecipeIngredientDTO.builder()
                                .ingredientId(INGREDIENT_ID)
                                .unitId(UNIT_ID)
                                .amount(new BigDecimal(500))
                                .build())))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", endsWith("/ingredients/" + INGREDIENT_ID)));

        ArgumentCaptor<RecipeIngredientDTO> captor = ArgumentCaptor.forClass(RecipeIngredientDTO.class);
        verify(recipeService).addRecipeIngredient(captor.capture());
        assertThat(captor.getValue().getRecipeId()).isEqualTo(ID);
        assertThat(captor.getValue().getIngredientId()).isEqualTo(INGREDIENT_ID);
    }

    @Test
    @DisplayName("Given an ingredient payload with no amount, When POST, Then 400 and the service is never called")
    public void shouldRejectAddIngredientWithoutAmount() throws Exception {
        mockMvc.perform(post(BASE + "/{id}/ingredients", ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(RecipeIngredientDTO.builder()
                                .ingredientId(INGREDIENT_ID)
                                .unitId(UNIT_ID)
                                .build())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value(ApplicationErrorCodes.VALIDATION_EXCEPTION.getCode()));

        verify(recipeService, never()).addRecipeIngredient(any());
    }

    @Test
    @DisplayName("Given an amount of zero, When POST an ingredient, Then 400")
    public void shouldRejectAddIngredientWithZeroAmount() throws Exception {
        mockMvc.perform(post(BASE + "/{id}/ingredients", ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(RecipeIngredientDTO.builder()
                                .ingredientId(INGREDIENT_ID)
                                .unitId(UNIT_ID)
                                .amount(BigDecimal.ZERO)
                                .build())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationExceptions[0].objectName").value("amount"));

        verify(recipeService, never()).addRecipeIngredient(any());
    }

    @Test
    @DisplayName("Given a duplicate ingredient, When POST, Then the service's 409 is surfaced")
    public void shouldReturn409ForDuplicateIngredient() throws Exception {
        given(recipeService.addRecipeIngredient(any())).willThrow(ServiceException.builder()
                .message("Ingredient " + INGREDIENT_ID + " is already part of recipe " + ID)
                .code(ApplicationErrorCodes.RECIPE_INGREDIENT_ALREADY_EXISTS.getCode())
                .httpStatus(HttpStatus.CONFLICT)
                .build());

        mockMvc.perform(post(BASE + "/{id}/ingredients", ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(RecipeIngredientDTO.builder()
                                .ingredientId(INGREDIENT_ID)
                                .unitId(UNIT_ID)
                                .amount(new BigDecimal(500))
                                .build())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode")
                        .value(ApplicationErrorCodes.RECIPE_INGREDIENT_ALREADY_EXISTS.getCode()));
    }

    @Test
    @DisplayName("Given a recipe ingredient, When PUT, Then both ids come from the path")
    public void shouldUpdateIngredientUsingPathIds() throws Exception {
        given(recipeService.updateRecipeIngredient(any())).willReturn(aRecipe());

        mockMvc.perform(put(BASE + "/{id}/ingredients/{ingredientId}", ID, INGREDIENT_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(RecipeIngredientDTO.builder()
                                .recipeId("00000000-0000-0000-0000-000000000000")   // ignored
                                .ingredientId("11111111-1111-1111-1111-111111111111") // ignored
                                .unitId(UNIT_ID)
                                .amount(new BigDecimal(750))
                                .build())))
                .andExpect(status().isOk());

        ArgumentCaptor<RecipeIngredientDTO> captor = ArgumentCaptor.forClass(RecipeIngredientDTO.class);
        verify(recipeService).updateRecipeIngredient(captor.capture());
        assertThat(captor.getValue().getRecipeId()).isEqualTo(ID);
        assertThat(captor.getValue().getIngredientId()).isEqualTo(INGREDIENT_ID);
        assertThat(captor.getValue().getAmount()).isEqualByComparingTo(new BigDecimal(750));
    }

    @Test
    @DisplayName("Given an ingredient not on the recipe, When PUT, Then the service's 404 is surfaced")
    public void shouldReturn404WhenUpdatingUnknownRecipeIngredient() throws Exception {
        given(recipeService.updateRecipeIngredient(any())).willThrow(ServiceException.builder()
                .message("Ingredient " + INGREDIENT_ID + " is not part of recipe " + ID)
                .code(ApplicationErrorCodes.RECIPE_INGREDIENT_NOT_FOUND.getCode())
                .httpStatus(HttpStatus.NOT_FOUND)
                .build());

        mockMvc.perform(put(BASE + "/{id}/ingredients/{ingredientId}", ID, INGREDIENT_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(RecipeIngredientDTO.builder()
                                .unitId(UNIT_ID)
                                .amount(new BigDecimal(750))
                                .ingredientId(INGREDIENT_ID)
                                .build())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode")
                        .value(ApplicationErrorCodes.RECIPE_INGREDIENT_NOT_FOUND.getCode()));
    }

    @Test
    @DisplayName("Given a recipe ingredient, When DELETE, Then 200 and the updated recipe is returned")
    public void shouldDeleteIngredient() throws Exception {
        given(recipeService.deleteRecipeIngredient(any())).willReturn(aRecipe());

        mockMvc.perform(delete(BASE + "/{id}/ingredients/{ingredientId}", ID, INGREDIENT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Hvedebrød"));

        ArgumentCaptor<RecipeIngredientDTO> captor = ArgumentCaptor.forClass(RecipeIngredientDTO.class);
        verify(recipeService).deleteRecipeIngredient(captor.capture());
        assertThat(captor.getValue().getRecipeId()).isEqualTo(ID);
        assertThat(captor.getValue().getIngredientId()).isEqualTo(INGREDIENT_ID);
    }
}
