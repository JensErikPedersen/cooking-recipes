package dk.serik.recipes.repository;

import dk.serik.recipes.bean.Session;
import dk.serik.recipes.model.*;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.jdbc.Sql;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

@DataJpaTest
@Slf4j
public class RecipeIngredientJpaRepositoryIT {
	
	@Autowired
	private RecipeIngredientJpaRepository recipeIngredientJpaRepository;
	
	@Autowired
	private IngredientJpaRepository ingredientJpaRepository;
	
	@Autowired
	private UnitJpaRepository unitJpaRepository;
	
	@Autowired
	private RecipeJpaRepository recipeJpaRepository;
	
	@Autowired
	private CategoryJpaRepository categoryJpaRepository;

	@MockitoBean
	private Session session;
	
	@BeforeEach
	public void setupTest() {
		Mockito.when(session.getUserName()).thenReturn("Jens");
	}
	
	
	@Test
	@Sql("/db/test-data/insert_recipes.sql")
	@DisplayName("Given Recipe and Unit, When Ingredient is Added to Recipe, Then Ok")
	public void givenRecipeAndUnit_WhenIngredientIsAdded_ThenOk() {
		// Given
		Optional<Unit> opUnit = unitJpaRepository.findById(UUID.fromString("b9cef3df-4bb5-49ab-8bde-5848d1363bce"));  // deciliter
		Assertions.assertTrue(opUnit.isPresent());
		Optional<Ingredient> opIngredient = ingredientJpaRepository.findById(UUID.fromString("381e5cd5-0a5d-48d2-b69c-71516254937e"));  // surdej
		Assertions.assertTrue(opIngredient.isPresent());
		Optional<Recipe> opRecipe = recipeJpaRepository.findById(UUID.fromString("5d22c394-b5ce-48c3-8199-72ccc92c737c"));  // Fuldkorns hvedebrød
		Assertions.assertTrue(opRecipe.isPresent());
		Optional<Category> opCategory = categoryJpaRepository.findById(UUID.fromString("14d4c0b0-46ea-498d-a3a5-56060a3d7a7c"));  // Brød
		Assertions.assertTrue(opCategory.isPresent());
		opRecipe.get().setCategory(opCategory.get());

		//When
		RecipeIngredient recipeIngredient = new RecipeIngredient();
		recipeIngredient.setAmount(new BigDecimal(1));
		recipeIngredient.setUnit(opUnit.get());
		recipeIngredient.setIngredient(opIngredient.get());
		recipeIngredient.setRecipe(opRecipe.get());
		log.info("RecipeIngredientEntity to be persisted: {}", recipeIngredient);
		OffsetDateTime now = OffsetDateTime.now();
		RecipeIngredient saveAndFlush = recipeIngredientJpaRepository.saveAndFlush(recipeIngredient);

		// Then
		log.info("Saved RecipeIngredient: {}", saveAndFlush);
		Assertions.assertEquals(UUID.fromString("5d22c394-b5ce-48c3-8199-72ccc92c737c"), saveAndFlush.getRecipe().getId());
		assertThat(now).isCloseTo(saveAndFlush.getCreated(), within(0, ChronoUnit.SECONDS));
		assertThat("Jens").isEqualTo(saveAndFlush.getCreatedBy());
	}
	

	@Test	
	@Sql({"/db/test-data/insert_recipes.sql", "/db/test-data/insert_recipe_ingredients.sql"})
	@DisplayName("Given Recipe with 6 ingredients, When Fetching All RecipeIngredient, Then All 6 is fetched")
	public void givenRecipeIngredients_WhenFetchingRecipe_ThenAllIngredientsFetched() {
		// When
		List<RecipeIngredient> allIngr = recipeIngredientJpaRepository.findAllByRecipeId(UUID.fromString("5d22c394-b5ce-48c3-8199-72ccc92c737c")); // Fuldkorns hvedebrød

		// Then
		Assertions.assertEquals(6, allIngr.size());
	}
	
	@Test	
	@Sql({"/db/test-data/insert_recipes.sql", "/db/test-data/insert_recipe_ingredients.sql"})
	@DisplayName("Given Recipe with Ingredient, When Fetching Recipe and Ingredient by Ids, Then RecipeIngredient is Fetched")
	public void givenRecipeIngredients_WhenFetchingRecipeIngredients_ThenRecipeIngredientIsFetched() {
		// When
		Optional<RecipeIngredient> recIngrOp = recipeIngredientJpaRepository.findByRecipeIdAndIngredientId(UUID.fromString("ce07075c-38b4-4b52-831c-5a9ce105e4af"), UUID.fromString("713ff039-25f2-471f-a1a4-ab8fc9efc8b0")); // Hvedebrød med Rugmel -> Gær

		//Then
		Assertions.assertTrue(recIngrOp.isPresent());
		Assertions.assertEquals("Hvedebrød med Rugmel", recIngrOp.get().getRecipe().getName());
		Assertions.assertEquals("Gær", recIngrOp.get().getIngredient().getName());
	}
	
	@Test	
	@Sql({"/db/test-data/insert_recipes.sql", "/db/test-data/insert_recipe_ingredients.sql"})
	@DisplayName("Given Recipes with Ingredient Hvedemel, When Fetching Ingredient Hvedemel, Then 2 RecipeIngredients is Fetched")
	public void givenRecipeIngredients_WhenFetchingIngredients_ThenAllIngredientsFetched() {
		// When
		List<RecipeIngredient> recIngr = recipeIngredientJpaRepository.findAllByIngredientId(UUID.fromString("549ab6e6-f2d8-4ab3-8ba8-6bc7af82f2fb")); // Hvedemel

		// Then
		Assertions.assertEquals(2, recIngr.size());
	}
	
	@Test
	@Sql({"/db/test-data/insert_recipes.sql", "/db/test-data/insert_recipe_ingredients.sql"})
	@DisplayName("Given Recipes with Ingredient Salt, When Deleting Ingredient Salt, Then only RecipeIngredient Salt is Fetched")
	public void givenRecipeWithIngredients_WhenIngredientIsDeleted_ThenNoOtherEntitiesIsDeleted() {
		// Given
		Optional<RecipeIngredient> recIngrOp = recipeIngredientJpaRepository.findByRecipeIdAndIngredientId(UUID.fromString("ce07075c-38b4-4b52-831c-5a9ce105e4af"), UUID.fromString("e0aa2252-c5f1-4c87-b42c-9dd10486f366")); // Hvedebrød med Rugmel -> Salt
		Assertions.assertTrue(recIngrOp.isPresent());
		UUID unitId = recIngrOp.get().getUnit().getId();

		// When
		recipeIngredientJpaRepository.delete(recIngrOp.get());
		List<RecipeIngredient> allIngr = recipeIngredientJpaRepository.findAllByRecipeId(UUID.fromString("ce07075c-38b4-4b52-831c-5a9ce105e4af")); // Fuldkorns hvedebrød
		Assertions.assertEquals(5, allIngr.size());

		// Then
		// check all related entities exist
		Optional<Ingredient> ingrOp = ingredientJpaRepository.findById(UUID.fromString("e0aa2252-c5f1-4c87-b42c-9dd10486f366")); // Salt
		Assertions.assertTrue(ingrOp.isPresent());
		
		Optional<Unit> unitOp = unitJpaRepository.findById(unitId);
		Assertions.assertTrue(unitOp.isPresent());
	}
	
	@Test
	@Sql({"/db/test-data/insert_recipes.sql", "/db/test-data/insert_recipe_ingredients.sql"})
	@DisplayName("Given Recipes with Ingredient Salt, When updating Ingredient Salt amount to 15, Then RecipeIngredient Salt amount is set to 15")
	public void givenRecipeWithIngredients_WhenAmountIsUpdated_ThenOk() {
		// Given
		Optional<RecipeIngredient> recIngrOp = recipeIngredientJpaRepository.findByRecipeIdAndIngredientId(UUID.fromString("ce07075c-38b4-4b52-831c-5a9ce105e4af"), UUID.fromString("e0aa2252-c5f1-4c87-b42c-9dd10486f366")); // Hvedebrød med Rugmel -> Salt
		Assertions.assertTrue(recIngrOp.isPresent());
		Assertions.assertEquals(new BigDecimal(16).intValue(), recIngrOp.get().getAmount().intValue());
		Assertions.assertEquals("Majken", recIngrOp.get().getUpdatedBy());

		// When
		recIngrOp.get().setAmount(new BigDecimal(15));
		OffsetDateTime now = OffsetDateTime.now();
		RecipeIngredient saveAndFlush = recipeIngredientJpaRepository.saveAndFlush(recIngrOp.get());

		// Then
		Assertions.assertEquals(new BigDecimal(15).intValue(), saveAndFlush.getAmount().intValue());
		Assertions.assertEquals("Jens", saveAndFlush.getUpdatedBy());	
		assertThat(now).isCloseTo(saveAndFlush.getUpdated(), within(0, ChronoUnit.SECONDS));		
	}
}
