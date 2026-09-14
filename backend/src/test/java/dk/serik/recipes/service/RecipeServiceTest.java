package dk.serik.recipes.service;

import dk.serik.recipes.dto.RecipeDTO;
import dk.serik.recipes.dto.RecipeIngredientDTO;
import dk.serik.recipes.exceptions.ApplicationErrorCodes;
import dk.serik.recipes.exceptions.ServiceException;
import dk.serik.recipes.mockutil.MockIngredientUtil;
import dk.serik.recipes.mockutil.MockUnitUtil;
import dk.serik.recipes.model.RecipeIngredient;
import dk.serik.recipes.mockutil.MockCategoryUtil;
import dk.serik.recipes.mockutil.MockRecipeIngredientUtil;
import dk.serik.recipes.mockutil.MockRecipeUtil;
import dk.serik.recipes.model.Recipe;
import dk.serik.recipes.repository.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static dk.serik.recipes.mockutil.MockRecipeUtil.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class RecipeServiceTest {
    private static final Logger log = LoggerFactory.getLogger(RecipeServiceTest.class);
    @Mock
    private RecipeJpaRepository recipeJpaRepository;

    @Mock
    private CategoryJpaRepository categoryJpaRepository;

    @Mock
    private RecipeIngredientJpaRepository recipeIngredientJpaRepository;

    @Mock
    private IngredientJpaRepository ingredientJpaRepository;

    @Mock
    private UnitJpaRepository unitJpaRepository;

    @InjectMocks
    private RecipeServiceImpl service;

    @Captor
    private ArgumentCaptor<Recipe> recipeCaptor;

    @Captor
    private ArgumentCaptor<RecipeIngredient> recipeIngredientCaptor;

    private static final String RECIPE_ID = "932b9ecc-ae01-47d3-996a-0e83c27f39b7";
    private static final String HVEDEMEL_ID = "5f01d434-5a68-4359-9f2e-0a6793dce48d";
    private static final String RUGMEL_ID = "3aebe786-f8fd-4d2e-92f5-cfe1669f9aa4";
    private static final String GRAM_ID = "f7823293-7874-4459-9fb7-6b420a0627fa";

    @Test
    @DisplayName("Given existing Recipe, When fetched by Id, Then RecipeDTO is returned")
    public void shouldReturnRecipeById() {
        // Given
        String uuid = "932b9ecc-ae01-47d3-996a-0e83c27f39b7";
        when(recipeJpaRepository.findById(UUID.fromString(uuid))).thenReturn(Optional.of(mockSavedRecipeWheatBread()));

        // When
        Optional<RecipeDTO> recipeDTO = service.findById(uuid);

        // Then
        assertThat(recipeDTO).isPresent();
        assertThat(recipeDTO.get().getRecipeIngredients().size()).isEqualTo(3);
        assertThat(recipeDTO.get().getName()).isEqualTo("Hvedebrød");
        assertThat(recipeDTO.get().getCreatedBy()).isEqualTo("Jens");
        assertThat(recipeDTO.get().getId()).isEqualTo(uuid);
        assertThat(recipeDTO.get().getRecipeIngredients().size()).isEqualTo(3);
    }

    @Test
    @DisplayName("Given new RecipeDTO and New RecipeIngredientDTOs, When Recipe is saved, Then new Recipe with RecipeIngredients is saved and RecipeDTO is returned")
    public void shouldSaveNewRecipeWithIngredients() {
        // Given
        when(recipeJpaRepository.save(any())).thenReturn(mockSavedRecipeWheatBread());
        when(categoryJpaRepository.findById(UUID.fromString("913a5159-3717-4b9d-a290-0158d31ea8ab"))).thenReturn(Optional.of(MockCategoryUtil.mockBread()));
        // A recipe being created has no id, so there is no existing recipe_ingredient row to find -
        // the ingredients and units are resolved directly. This test previously stubbed
        // findByRecipeIdAndIngredientId with the id of the not-yet-created recipe, which no real
        // caller could supply, and so never exercised the create path at all.
        when(ingredientJpaRepository.findById(UUID.fromString("5f01d434-5a68-4359-9f2e-0a6793dce48d")))
                .thenReturn(Optional.of(MockIngredientUtil.mockHvedemel()));
        when(ingredientJpaRepository.findById(UUID.fromString("01a50907-8141-4dd1-acdf-c4384669c2b2")))
                .thenReturn(Optional.of(MockIngredientUtil.mockHavsalt()));
        when(ingredientJpaRepository.findById(UUID.fromString("1d150b3f-1a7c-4c08-8264-2910af5b9d25")))
                .thenReturn(Optional.of(MockIngredientUtil.mockSurdej()));
        when(unitJpaRepository.findById(UUID.fromString("f7823293-7874-4459-9fb7-6b420a0627fa")))
                .thenReturn(Optional.of(MockUnitUtil.mockGram()));
        when(unitJpaRepository.findById(UUID.fromString("046d0928-0806-480b-ad8b-c6845e99643b")))
                .thenReturn(Optional.of(MockUnitUtil.mockDl()));

        // When
        log.info("Saving new (mock) Recipe: {}", MockRecipeUtil.mockRecipeWheatBreadToBeSaved());
        RecipeDTO recipeDTO = service.save(mockRecipeWheatBreadWithIngredientsDTOToBeSaved());

        // Then
        verify(recipeJpaRepository).save(recipeCaptor.capture());
        Recipe recipe = recipeCaptor.getValue();
        assertThat(recipe.getRecipeIngredients().size()).isEqualTo(3);
        assertThat(recipe.getName()).isEqualTo("Hvedebrød");
        assertThat(recipe.getDescription()).isEqualTo("Lækkert brød til morgenmaden");
        assertThat(recipeDTO).isNotNull();
        assertThat(recipeDTO.getRecipeIngredients().size()).isEqualTo(3);
        assertThat(recipeDTO.getName()).isEqualTo("Hvedebrød");
        assertThat(recipeDTO.getCreatedBy()).isEqualTo("Jens");
        assertThat(recipeDTO.getId()).isEqualTo("932b9ecc-ae01-47d3-996a-0e83c27f39b7");

    }

    @Test
    @DisplayName("Given a create payload whose ingredients carry no recipeId, When saved, Then they are attached rather than dropped")
    public void shouldAttachIngredientsOnCreateWithoutRecipeId() {
        // Given - a realistic create payload: the client cannot know the recipe id, because the
        // recipe does not exist yet, so the nested ingredients carry none.
        when(recipeJpaRepository.save(any())).thenReturn(mockSavedRecipeWheatBread());
        when(categoryJpaRepository.findById(UUID.fromString("913a5159-3717-4b9d-a290-0158d31ea8ab")))
                .thenReturn(Optional.of(MockCategoryUtil.mockBread()));
        when(ingredientJpaRepository.findById(UUID.fromString("5f01d434-5a68-4359-9f2e-0a6793dce48d")))
                .thenReturn(Optional.of(MockIngredientUtil.mockHvedemel()));
        when(unitJpaRepository.findById(UUID.fromString("f7823293-7874-4459-9fb7-6b420a0627fa")))
                .thenReturn(Optional.of(MockUnitUtil.mockGram()));

        RecipeDTO toBeSaved = RecipeDTO.builder()
                .name("Hvedebrød")
                .categoryId("913a5159-3717-4b9d-a290-0158d31ea8ab")
                .recipeIngredients(Set.of(RecipeIngredientDTO.builder()
                        .ingredientId("5f01d434-5a68-4359-9f2e-0a6793dce48d")
                        .unitId("f7823293-7874-4459-9fb7-6b420a0627fa")
                        .amount(new BigDecimal(500))
                        .build()))
                .build();

        // When
        service.save(toBeSaved);

        // Then
        verify(recipeJpaRepository).save(recipeCaptor.capture());
        assertThat(recipeCaptor.getValue().getRecipeIngredients())
                .as("the ingredient the caller asked for must reach the persisted recipe")
                .hasSize(1);
    }

    @Test
    @DisplayName("Given a create payload naming an unknown ingredient, When saved, Then it is rejected rather than silently dropped")
    public void shouldRejectSaveWhenIngredientIsUnknown() {
        // Given
        when(categoryJpaRepository.findById(UUID.fromString("913a5159-3717-4b9d-a290-0158d31ea8ab")))
                .thenReturn(Optional.of(MockCategoryUtil.mockBread()));
        when(ingredientJpaRepository.findById(UUID.fromString("5f01d434-5a68-4359-9f2e-0a6793dce48d")))
                .thenReturn(Optional.empty());

        RecipeDTO toBeSaved = RecipeDTO.builder()
                .name("Hvedebrød")
                .categoryId("913a5159-3717-4b9d-a290-0158d31ea8ab")
                .recipeIngredients(Set.of(RecipeIngredientDTO.builder()
                        .ingredientId("5f01d434-5a68-4359-9f2e-0a6793dce48d")
                        .unitId("f7823293-7874-4459-9fb7-6b420a0627fa")
                        .amount(new BigDecimal(500))
                        .build()))
                .build();

        // When / Then
        assertThatThrownBy(() -> service.save(toBeSaved))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("5f01d434-5a68-4359-9f2e-0a6793dce48d")
                .extracting("httpStatus").isEqualTo(HttpStatus.BAD_REQUEST);

        verify(recipeJpaRepository, never()).save(any());
    }

    @Test
    @DisplayName("Given a create payload with an ingredient but no unit, When saved, Then it is rejected rather than silently dropped")
    public void shouldRejectSaveWhenIngredientHasNoUnit() {
        // Given
        when(categoryJpaRepository.findById(UUID.fromString("913a5159-3717-4b9d-a290-0158d31ea8ab")))
                .thenReturn(Optional.of(MockCategoryUtil.mockBread()));
        when(ingredientJpaRepository.findById(UUID.fromString("5f01d434-5a68-4359-9f2e-0a6793dce48d")))
                .thenReturn(Optional.of(MockIngredientUtil.mockHvedemel()));

        // the ingredient resolves; it is the missing unit that must be reported
        RecipeDTO toBeSaved = RecipeDTO.builder()
                .name("Hvedebrød")
                .categoryId("913a5159-3717-4b9d-a290-0158d31ea8ab")
                .recipeIngredients(Set.of(RecipeIngredientDTO.builder()
                        .ingredientId("5f01d434-5a68-4359-9f2e-0a6793dce48d")
                        .amount(new BigDecimal(500))
                        .build()))
                .build();

        // When / Then
        assertThatThrownBy(() -> service.save(toBeSaved))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("unit")
                .extracting("httpStatus").isEqualTo(HttpStatus.BAD_REQUEST);

        verify(recipeJpaRepository, never()).save(any());
    }

    @Test
    @DisplayName("Given new RecipeDTO with no RecipeIngredients, When Recipe is saved, Then it is saved without ingredients")
    public void shouldSaveNewRecipeWithoutIngredients() {
        // Given
        when(recipeJpaRepository.save(any())).thenReturn(mockSavedRecipeWheatBread());
        when(categoryJpaRepository.findById(UUID.fromString("913a5159-3717-4b9d-a290-0158d31ea8ab")))
                .thenReturn(Optional.of(MockCategoryUtil.mockBread()));

        // recipeIngredients deliberately left unset - a recipe without ingredients is valid input
        RecipeDTO toBeSaved = RecipeDTO.builder()
                .name("Hvedebrød")
                .description("Lækkert brød til morgenmaden")
                .instructions("Bland det hele sammen og bag det")
                .categoryId("913a5159-3717-4b9d-a290-0158d31ea8ab")
                .categoryName("Brød")
                .build();

        // When
        RecipeDTO savedDto = service.save(toBeSaved);

        // Then
        verify(recipeJpaRepository).save(recipeCaptor.capture());
        assertThat(recipeCaptor.getValue().getName()).isEqualTo("Hvedebrød");
        assertThat(recipeCaptor.getValue().getRecipeIngredients()).isNull();
        assertThat(savedDto).isNotNull();
    }

    @Test
    @DisplayName("Given a RecipeDTO with no Category, When saved, Then a bad request is reported")
    public void shouldRejectSaveWithoutCategory() {
        // Given - neither categoryId nor categoryName supplied
        RecipeDTO noCategory = RecipeDTO.builder()
                .name("Hvedebrød")
                .description("Lækkert brød til morgenmaden")
                .instructions("Bland det hele sammen og bag det")
                .build();

        // an absent category must stay absent, not become an empty CategoryDTO
        assertThat(noCategory.getCategory()).isNull();

        // When / Then
        assertThatThrownBy(() -> service.save(noCategory))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("requires the id of an existing category")
                .extracting("code", "httpStatus")
                .containsExactly(ApplicationErrorCodes.CATEGORY_IS_REQUIRED.getCode(), HttpStatus.BAD_REQUEST);
        verify(recipeJpaRepository, never()).save(any());
        verify(categoryJpaRepository, never()).save(any());
    }

    @Test
    @DisplayName("Given a Recipe and a new Ingredient, When adding a RecipeIngredient, Then it is added to the Recipe")
    public void shouldAddRecipeIngredient() {
        // Given
        RecipeIngredientDTO toAdd = RecipeIngredientDTO.builder()
                .recipeId(RECIPE_ID)
                .ingredientId(RUGMEL_ID)
                .unitId(GRAM_ID)
                .amount(new BigDecimal(200))
                .build();
        when(recipeJpaRepository.findById(UUID.fromString(RECIPE_ID))).thenReturn(Optional.of(mockSavedRecipeWheatBread()));
        when(ingredientJpaRepository.findById(UUID.fromString(RUGMEL_ID))).thenReturn(Optional.of(MockIngredientUtil.mockRugmel()));
        when(unitJpaRepository.findById(UUID.fromString(GRAM_ID))).thenReturn(Optional.of(MockUnitUtil.mockGram()));
        when(recipeIngredientJpaRepository.findByRecipeIdAndIngredientId(UUID.fromString(RECIPE_ID), UUID.fromString(RUGMEL_ID)))
                .thenReturn(Optional.empty());
        when(recipeJpaRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        RecipeDTO updated = service.addRecipeIngredient(toAdd);

        // Then
        verify(recipeJpaRepository).save(recipeCaptor.capture());
        assertThat(recipeCaptor.getValue().getRecipeIngredients()).hasSize(4);
        assertThat(updated.getRecipeIngredients()).extracting("ingredientName").contains("Rugmel");
    }

    @Test
    @DisplayName("Given an Ingredient already on the Recipe, When adding it again, Then a conflict is reported")
    public void shouldRejectDuplicateRecipeIngredient() {
        // Given
        when(recipeJpaRepository.findById(UUID.fromString(RECIPE_ID))).thenReturn(Optional.of(mockSavedRecipeWheatBread()));
        when(ingredientJpaRepository.findById(UUID.fromString(HVEDEMEL_ID))).thenReturn(Optional.of(MockIngredientUtil.mockHvedemel()));
        when(unitJpaRepository.findById(UUID.fromString(GRAM_ID))).thenReturn(Optional.of(MockUnitUtil.mockGram()));
        when(recipeIngredientJpaRepository.findByRecipeIdAndIngredientId(UUID.fromString(RECIPE_ID), UUID.fromString(HVEDEMEL_ID)))
                .thenReturn(Optional.of(MockRecipeIngredientUtil.mockSavedRecipeIngredientHvedemel()));

        // When / Then
        assertThatThrownBy(() -> service.addRecipeIngredient(MockRecipeIngredientUtil.mockRecipeIngredientDTOHvedemel()))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("is already part of recipe")
                .extracting("code", "httpStatus")
                .containsExactly(ApplicationErrorCodes.RECIPE_INGREDIENT_ALREADY_EXISTS.getCode(), HttpStatus.CONFLICT);
        verify(recipeJpaRepository, never()).save(any());
    }

    @Test
    @DisplayName("Given an unknown Recipe, When adding a RecipeIngredient, Then RECIPE_NOT_FOUND is reported")
    public void shouldRejectAddForUnknownRecipe() {
        // Given
        when(recipeJpaRepository.findById(UUID.fromString(RECIPE_ID))).thenReturn(Optional.empty());

        // When / Then
        assertThatThrownBy(() -> service.addRecipeIngredient(MockRecipeIngredientUtil.mockRecipeIngredientDTOHvedemel()))
                .isInstanceOf(ServiceException.class)
                .extracting("code", "httpStatus")
                .containsExactly(ApplicationErrorCodes.RECIPE_NOT_FOUND.getCode(), HttpStatus.NOT_FOUND);
    }

    @Test
    @DisplayName("Given a RecipeIngredient without a unit, When adding it, Then a bad request is reported")
    public void shouldRejectAddWithoutUnit() {
        // Given
        RecipeIngredientDTO noUnit = RecipeIngredientDTO.builder()
                .recipeId(RECIPE_ID)
                .ingredientId(RUGMEL_ID)
                .amount(new BigDecimal(200))
                .build();

        // When / Then
        assertThatThrownBy(() -> service.addRecipeIngredient(noUnit))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("requires both a unit id and an amount")
                .extracting("httpStatus")
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("Given an existing RecipeIngredient, When the amount is updated, Then the new amount is saved")
    public void shouldUpdateRecipeIngredient() {
        // Given
        RecipeIngredientDTO toUpdate = RecipeIngredientDTO.builder()
                .recipeId(RECIPE_ID)
                .ingredientId(HVEDEMEL_ID)
                .unitId(GRAM_ID)
                .amount(new BigDecimal(750))
                .build();
        when(recipeJpaRepository.findById(UUID.fromString(RECIPE_ID))).thenReturn(Optional.of(mockSavedRecipeWheatBread()));
        when(recipeIngredientJpaRepository.findByRecipeIdAndIngredientId(UUID.fromString(RECIPE_ID), UUID.fromString(HVEDEMEL_ID)))
                .thenReturn(Optional.of(MockRecipeIngredientUtil.mockSavedRecipeIngredientHvedemel()));
        when(unitJpaRepository.findById(UUID.fromString(GRAM_ID))).thenReturn(Optional.of(MockUnitUtil.mockGram()));

        // When
        service.updateRecipeIngredient(toUpdate);

        // Then
        verify(recipeIngredientJpaRepository).save(recipeIngredientCaptor.capture());
        assertThat(recipeIngredientCaptor.getValue().getAmount()).isEqualByComparingTo(new BigDecimal(750));
        assertThat(recipeIngredientCaptor.getValue().getUnit().getLabel()).isEqualTo("g");
    }

    @Test
    @DisplayName("Given an Ingredient not on the Recipe, When updating it, Then RECIPE_INGREDIENT_NOT_FOUND is reported")
    public void shouldRejectUpdateForUnknownRecipeIngredient() {
        // Given
        when(recipeJpaRepository.findById(UUID.fromString(RECIPE_ID))).thenReturn(Optional.of(mockSavedRecipeWheatBread()));
        when(recipeIngredientJpaRepository.findByRecipeIdAndIngredientId(UUID.fromString(RECIPE_ID), UUID.fromString(RUGMEL_ID)))
                .thenReturn(Optional.empty());
        RecipeIngredientDTO unknown = RecipeIngredientDTO.builder()
                .recipeId(RECIPE_ID)
                .ingredientId(RUGMEL_ID)
                .unitId(GRAM_ID)
                .amount(new BigDecimal(200))
                .build();

        // When / Then
        assertThatThrownBy(() -> service.updateRecipeIngredient(unknown))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("is not part of recipe")
                .extracting("code", "httpStatus")
                .containsExactly(ApplicationErrorCodes.RECIPE_INGREDIENT_NOT_FOUND.getCode(), HttpStatus.NOT_FOUND);
    }

    @Test
    @DisplayName("Given an existing RecipeIngredient, When deleted, Then it is removed from the Recipe")
    public void shouldDeleteRecipeIngredient() {
        // Given
        Recipe recipe = mockSavedRecipeWheatBread();
        // the persistence context hands back the same instance the Recipe already holds,
        // so take it from the recipe rather than building a detached copy
        RecipeIngredient managed = recipe.getRecipeIngredients().stream()
                .filter(ri -> ri.getIngredient().getId().toString().equals(HVEDEMEL_ID))
                .findFirst()
                .orElseThrow();
        when(recipeJpaRepository.findById(UUID.fromString(RECIPE_ID))).thenReturn(Optional.of(recipe));
        when(recipeIngredientJpaRepository.findByRecipeIdAndIngredientId(UUID.fromString(RECIPE_ID), UUID.fromString(HVEDEMEL_ID)))
                .thenReturn(Optional.of(managed));

        // When
        RecipeDTO updated = service.deleteRecipeIngredient(MockRecipeIngredientUtil.mockRecipeIngredientDTOHvedemel());

        // Then
        verify(recipeIngredientJpaRepository).delete(managed);
        assertThat(recipe.getRecipeIngredients()).hasSize(2);
        assertThat(updated.getRecipeIngredients()).extracting("ingredientName").doesNotContain("Hvedemel");
    }

    @Test
    @DisplayName("Given an Ingredient not on the Recipe, When deleting it, Then RECIPE_INGREDIENT_NOT_FOUND is reported")
    public void shouldRejectDeleteForUnknownRecipeIngredient() {
        // Given
        when(recipeJpaRepository.findById(UUID.fromString(RECIPE_ID))).thenReturn(Optional.of(mockSavedRecipeWheatBread()));
        when(recipeIngredientJpaRepository.findByRecipeIdAndIngredientId(UUID.fromString(RECIPE_ID), UUID.fromString(RUGMEL_ID)))
                .thenReturn(Optional.empty());
        RecipeIngredientDTO unknown = RecipeIngredientDTO.builder()
                .recipeId(RECIPE_ID)
                .ingredientId(RUGMEL_ID)
                .build();

        // When / Then
        assertThatThrownBy(() -> service.deleteRecipeIngredient(unknown))
                .isInstanceOf(ServiceException.class)
                .extracting("code", "httpStatus")
                .containsExactly(ApplicationErrorCodes.RECIPE_INGREDIENT_NOT_FOUND.getCode(), HttpStatus.NOT_FOUND);
        verify(recipeIngredientJpaRepository, never()).delete(any());
    }

    @Test
    @DisplayName("Given ratings are out of scope, When adding a RecipeRating, Then it fails loudly")
    public void shouldRejectAddRecipeRating() {
        assertThatThrownBy(() -> service.addRecipeRating(null))
                .isInstanceOf(UnsupportedOperationException.class)
                .hasMessageContaining("not supported in this version");
    }

    @Test
    void shouldFindAllByCategoryName() {
        // Given
        List<Recipe> recipeList = new ArrayList<>();
        recipeList.add(mockSavedRecipeWheatBread());
        recipeList.add(mockSavedRecipeRyeBread());
        when(recipeJpaRepository.findAllByCategoryName("Brød")).thenReturn(recipeList);

        // When
        List<RecipeDTO> recipeDTOS = service.findAllByCategoryName("Brød");

        // Then
        verify(recipeJpaRepository, times(1)).findAllByCategoryName(any());
        assertThat(recipeDTOS).isNotNull();
        assertThat(recipeDTOS).isNotEmpty();
        assertThat(recipeDTOS.size()).isEqualTo(2);
    }

    @Test
    void shouldNotFindAnyByCategoryName() {
        // Given
        when(recipeJpaRepository.findAllByCategoryName("Brød")).thenReturn(new ArrayList<>());

        // When
        List<RecipeDTO> recipeDTOS = service.findAllByCategoryName("Brød");

        // Then
        verify(recipeJpaRepository, times(1)).findAllByCategoryName(any());
        assertThat(recipeDTOS).isNotNull();
        assertThat(recipeDTOS).isEmpty();
    }

    @Test
    void shouldFindAllByNameContains() {
        // Given
        List<Recipe> recipeList = new ArrayList<>();
        recipeList.add(mockSavedRecipeWheatBread());
        recipeList.add(mockSavedRecipeRyeBread());
        when(recipeJpaRepository.findAllByNameContains("br")).thenReturn(recipeList);

        // When
        List<RecipeDTO> recipeDTOS = service.findAllByNameContains("br");

        // Then
        verify(recipeJpaRepository, times(1)).findAllByNameContains(any());
        assertThat(recipeDTOS).isNotNull();
        assertThat(recipeDTOS).isNotEmpty();
        assertThat(recipeDTOS.size()).isEqualTo(2);
    }

    @Test
    void shouldNotFindAnyByNameContains() {
        // Given
        when(recipeJpaRepository.findAllByNameContains("br")).thenReturn(new ArrayList<>());

        // When
        List<RecipeDTO> recipeDTOS = service.findAllByNameContains("br");

        // Then
        verify(recipeJpaRepository, times(1)).findAllByNameContains(any());
        assertThat(recipeDTOS).isNotNull();
        assertThat(recipeDTOS).isEmpty();
    }

    @Test
    void shouldDeleteRecipe() {
        // Given
        when(recipeJpaRepository.findById((mockSavedRecipeWheatBread().getId()))).thenReturn(Optional.of(mockSavedRecipeWheatBread()));
        // When
        boolean delete = service.delete(String.valueOf(mockSavedRecipeWheatBread().getId()));
        // Then
        assertThat(delete).isEqualTo(true);
        verify(recipeJpaRepository, times(1)).findById(mockSavedRecipeWheatBread().getId());
        verify(recipeJpaRepository, times(1)).delete(mockSavedRecipeWheatBread());
    }

    @Test
    void shouldNotDeleteRecipeNotFound() {
        // Given
        when(recipeJpaRepository.findById((mockSavedRecipeWheatBread().getId()))).thenReturn(Optional.empty());
        // When
        boolean delete = service.delete(String.valueOf(mockSavedRecipeWheatBread().getId()));
        // Then
        assertThat(delete).isEqualTo(false);
        verify(recipeJpaRepository, times(1)).findById(mockSavedRecipeWheatBread().getId());
        verify(recipeJpaRepository, times(0)).delete(mockSavedRecipeWheatBread());
    }

    @Test
    @DisplayName("Given existing Recipe, When updated, Then all editable fields are persisted and the saved Recipe is returned")
    void shouldUpdateRecipe() {
        // Given
        final String NEW_DESCRIPTION = "New Description";
        final String NEW_INSTRUCTIONS = "New Instructions";
        RecipeDTO toUpdate = mockSavedRecipeWheatBreadWithIngredientsAndRatingDTO();
        toUpdate.setRecipeRatings(null);   // ratings are rejected on write in this version
        toUpdate.setDescription(NEW_DESCRIPTION);
        toUpdate.setInstructions(NEW_INSTRUCTIONS);
        when(recipeJpaRepository.findById(UUID.fromString(toUpdate.getId()))).thenReturn(Optional.of(mockSavedRecipeWheatBread()));
        when(categoryJpaRepository.findById(UUID.fromString("913a5159-3717-4b9d-a290-0158d31ea8ab")))
                .thenReturn(Optional.of(MockCategoryUtil.mockBread()));
        // the three ingredients are already on this recipe; without these stubs the update silently
        // dropped all of them and this test still passed, asserting only the text fields
        when(recipeIngredientJpaRepository.findByRecipeIdAndIngredientId(UUID.fromString(RECIPE_ID), UUID.fromString("5f01d434-5a68-4359-9f2e-0a6793dce48d")))
                .thenReturn(Optional.of(MockRecipeIngredientUtil.mockSavedRecipeIngredientHvedemel()));
        when(recipeIngredientJpaRepository.findByRecipeIdAndIngredientId(UUID.fromString(RECIPE_ID), UUID.fromString("01a50907-8141-4dd1-acdf-c4384669c2b2")))
                .thenReturn(Optional.of(MockRecipeIngredientUtil.mockSavedRecipeIngredientSalt()));
        when(recipeIngredientJpaRepository.findByRecipeIdAndIngredientId(UUID.fromString(RECIPE_ID), UUID.fromString("1d150b3f-1a7c-4c08-8264-2910af5b9d25")))
                .thenReturn(Optional.of(MockRecipeIngredientUtil.mockSavedRecipeIngredientSurdej()));
        when(recipeJpaRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        RecipeDTO updated = service.update(toUpdate);

        //Then
        verify(recipeJpaRepository, times(1)).findById(mockSavedRecipeWheatBread().getId());
        verify(recipeJpaRepository).save(recipeCaptor.capture());
        Recipe capturedRecipe = recipeCaptor.getValue();
        assertThat(capturedRecipe.getDescription()).isEqualTo(NEW_DESCRIPTION);
        assertThat(capturedRecipe.getInstructions()).isEqualTo(NEW_INSTRUCTIONS);
        // the result must be mapped from what was persisted, not the caller's own object
        assertThat(updated).isNotSameAs(toUpdate);
        assertThat(updated.getDescription()).isEqualTo(NEW_DESCRIPTION);
        assertThat(updated.getInstructions()).isEqualTo(NEW_INSTRUCTIONS);
    }

    @Test
    @DisplayName("Given a RecipeDTO without an id, When updated, Then a bad request is reported")
    void shouldRejectUpdateWithoutId() {
        // Given
        RecipeDTO noId = RecipeDTO.builder().name("Hvedebrød").build();

        // When / Then
        assertThatThrownBy(() -> service.update(noId))
                .isInstanceOf(ServiceException.class)
                .extracting("code", "httpStatus")
                .containsExactly(ApplicationErrorCodes.RECIPE_ID_IS_NULL.getCode(), HttpStatus.BAD_REQUEST);
        verify(recipeJpaRepository, never()).save(any());
    }

    @Test
    @DisplayName("Given an unknown Recipe, When updated, Then RECIPE_NOT_FOUND is reported")
    void shouldRejectUpdateForUnknownRecipe() {
        // Given
        RecipeDTO toUpdate = mockSavedRecipeWheatBreadWithIngredientsAndRatingDTO();
        toUpdate.setRecipeRatings(null);   // ratings are rejected on write in this version
        when(recipeJpaRepository.findById(UUID.fromString(toUpdate.getId()))).thenReturn(Optional.empty());

        // When / Then
        assertThatThrownBy(() -> service.update(toUpdate))
                .isInstanceOf(ServiceException.class)
                .extracting("code", "httpStatus")
                .containsExactly(ApplicationErrorCodes.RECIPE_NOT_FOUND.getCode(), HttpStatus.NOT_FOUND);
        verify(recipeJpaRepository, never()).save(any());
    }


    @Test
    @DisplayName("Given RecipeDTO is null, When saving, Then a bad request is reported")
    public void shouldRejectSaveOfNullRecipe() {
        assertThatThrownBy(() -> service.save(null))
                .isInstanceOf(ServiceException.class)
                .extracting("code", "httpStatus")
                .containsExactly(ApplicationErrorCodes.RECIPE_DTO_IS_NULL.getCode(), HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("Given id is null, When deleting a Recipe, Then a bad request is reported")
    public void shouldRejectDeleteOfNullRecipeId() {
        assertThatThrownBy(() -> service.delete(null))
                .isInstanceOf(ServiceException.class)
                .extracting("code", "httpStatus")
                .containsExactly(ApplicationErrorCodes.RECIPE_ID_IS_NULL.getCode(), HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("Given a malformed id, When fetching a Recipe by id, Then a bad request is reported")
    public void shouldRejectMalformedRecipeIdOnFindById() {
        assertThatThrownBy(() -> service.findById("not-a-uuid"))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("is not a valid UUID")
                .extracting("code", "httpStatus")
                .containsExactly(ApplicationErrorCodes.RECIPE_ID_IS_NULL.getCode(), HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("Given a malformed id, When deleting a Recipe, Then a bad request is reported")
    public void shouldRejectMalformedRecipeIdOnDelete() {
        assertThatThrownBy(() -> service.delete("not-a-uuid"))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("is not a valid UUID")
                .extracting("code", "httpStatus")
                .containsExactly(ApplicationErrorCodes.RECIPE_ID_IS_NULL.getCode(), HttpStatus.BAD_REQUEST);
    }


    @Test
    @DisplayName("Given a Category id that does not exist, When saving a Recipe, Then CATEGORY_NOT_FOUND is reported")
    public void shouldRejectSaveWithUnknownCategory() {
        // Given
        when(categoryJpaRepository.findById(UUID.fromString("913a5159-3717-4b9d-a290-0158d31ea8ab")))
                .thenReturn(Optional.empty());

        // When / Then
        assertThatThrownBy(() -> service.save(mockRecipeWheatBreadWithIngredientsDTOToBeSaved()))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("Cannot find category with id")
                .extracting("code", "httpStatus")
                .containsExactly(ApplicationErrorCodes.CATEGORY_NOT_FOUND.getCode(), HttpStatus.NOT_FOUND);
        verify(recipeJpaRepository, never()).save(any());
    }

    @Test
    @DisplayName("Given a Category name but no id, When saving a Recipe, Then it is rejected rather than creating a Category")
    public void shouldNotCreateCategoryImplicitly() {
        // Given - a category named but not identified
        RecipeDTO namedCategoryOnly = RecipeDTO.builder()
                .name("Hvedebrød")
                .description("Lækkert brød til morgenmaden")
                .instructions("Bland det hele sammen og bag det")
                .categoryName("Et helt nyt navn")
                .build();

        // When / Then
        assertThatThrownBy(() -> service.save(namedCategoryOnly))
                .isInstanceOf(ServiceException.class)
                .extracting("code", "httpStatus")
                .containsExactly(ApplicationErrorCodes.CATEGORY_IS_REQUIRED.getCode(), HttpStatus.BAD_REQUEST);
        // saving a recipe must never be a back door for creating categories
        verify(categoryJpaRepository, never()).save(any());
        verify(recipeJpaRepository, never()).save(any());
    }

    @Test
    @DisplayName("Given a RecipeIngredient with no back-reference, When put in a HashSet, Then it does not throw")
    public void shouldNotThrowWhenRecipeIngredientBackReferenceIsUnset() {
        // Given - built straight from the builder, not yet attached to a Recipe
        RecipeIngredient detached = RecipeIngredient.builder()
                .ingredient(MockIngredientUtil.mockRugmel())
                .unit(MockUnitUtil.mockGram())
                .amount(new BigDecimal(200))
                .build();

        // When / Then
        assertThatCode(() -> new java.util.HashSet<>(java.util.List.of(detached)).contains(detached))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Given a RecipeDTO carrying ratings, When saved, Then it is rejected rather than dropping them")
    public void shouldRejectSaveCarryingRecipeRatings() {
        // Given - ratings are out of scope this version; accepting and discarding them would lose data
        RecipeDTO withRatings = mockRecipeWheatBreadWithIngredientsDTOToBeSaved();
        withRatings.setRecipeRatings(java.util.Set.of(
                dk.serik.recipes.dto.RecipeRatingDTO.builder().rating(5).description("Excellent").build()));

        // When / Then
        assertThatThrownBy(() -> service.save(withRatings))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("cannot be set through this endpoint")
                .extracting("code", "httpStatus")
                .containsExactly(ApplicationErrorCodes.RECIPE_RATING_NOT_SUPPORTED.getCode(), HttpStatus.BAD_REQUEST);
        verify(recipeJpaRepository, never()).save(any());
    }

    @Test
    @DisplayName("Given a RecipeDTO carrying ratings, When updated, Then it is rejected too")
    public void shouldRejectUpdateCarryingRecipeRatings() {
        // Given
        RecipeDTO withRatings = mockSavedRecipeWheatBreadWithIngredientsAndRatingDTO();

        // When / Then - this fixture already carries ratings
        assertThatThrownBy(() -> service.update(withRatings))
                .isInstanceOf(ServiceException.class)
                .extracting("code", "httpStatus")
                .containsExactly(ApplicationErrorCodes.RECIPE_RATING_NOT_SUPPORTED.getCode(), HttpStatus.BAD_REQUEST);
        verify(recipeJpaRepository, never()).save(any());
    }
}
