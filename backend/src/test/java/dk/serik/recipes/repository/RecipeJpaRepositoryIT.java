package dk.serik.recipes.repository;

import dk.serik.recipes.bean.Session;
import dk.serik.recipes.model.*;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

@DataJpaTest
@Slf4j
public class
RecipeJpaRepositoryIT {

	@Autowired
	private RecipeJpaRepository recipeJpaRepository;
	
	@Autowired
	private CategoryJpaRepository categoryRepository;
	
	@Autowired
	private RecipeIngredientJpaRepository recipeIngredientJpaRepository;

	@Autowired
	private IngredientJpaRepository ingredientJpaRepository;

	@Autowired
	private UnitJpaRepository unitJpaRepository;

	@Autowired
	private RecipeRatingJpaRepository recipeRatingJpaRepository;

	@Autowired
	private RatingJpaRepository ratingJpaRepository;
	
	@MockitoBean
	private Session session;
	
	@BeforeEach
	public void setupTest() {
		Mockito.when(session.getUserName()).thenReturn("Majken");
	}
	
	@Test
	@Sql("/db/test-data/insert_recipes.sql")
	@DisplayName("Given six existing Recipes, When fetching all, Then six is returned")
	public void shouldReturnSixRecipes() {
		//When
		List<Recipe> recipes = recipeJpaRepository.findAll();

		//Then
		Assertions.assertFalse(recipes.isEmpty());
		Assertions.assertEquals(7, recipes.size());
	}
	
	@Test
	@Sql("/db/test-data/insert_recipes.sql")
	@DisplayName("Given existing Recipe with known id, When fetching Recipe by id, Then recipe is fetched ok")
	public void shouldReturnRecipeByKnownId() {
		//When
		Optional<Recipe> reOptional = recipeJpaRepository.findById(UUID.fromString("4ea753a3-07ff-47ce-82bb-9dcc1aa25477"));

		//Then
		Assertions.assertTrue(reOptional.isPresent());
		Assertions.assertEquals("Stegt flæsk med persillesovs og kogte kartofler", reOptional.get().getName());
		Assertions.assertEquals("En klassisk menu til herremiddagen", reOptional.get().getDescription());
	}
	
	@Test
	@Sql("/db/test-data/insert_recipes.sql")
	@DisplayName("Given three recipes with category 'Hovedret', When all Recipes by category 'Hovedret' is fetched, Then three is returned")
	public void shouldReturnThreeRecipesByCategoryMainDish() {
		//When
		List<Recipe> recipes = recipeJpaRepository.findAllByCategoryName("Hovedret");

		//Then
		Assertions.assertFalse(recipes.isEmpty());
		Assertions.assertEquals(3, recipes.size());
	}
	
	@Test
	@Sql("/db/test-data/insert_recipes.sql")
	@DisplayName("Given new valid Recipe with category 'kager', When saved to database, Then Recipe is saved ok and one more recipe added to category 'Kager'")
	public void shouldSaveNewRecipeWithCategoryCake() {
		//Given
		OffsetDateTime now = OffsetDateTime.now();
		Recipe recipe = buildRecipeEntity();
		Optional<Unit> opUnit = unitJpaRepository.findById(UUID.fromString("b9cef3df-4bb5-49ab-8bde-5848d1363bce"));  // deciliter
		Assertions.assertTrue(opUnit.isPresent());
		Optional<Ingredient> opIngredient = ingredientJpaRepository.findById(UUID.fromString("e00aec55-2eb7-4eeb-a594-0bbb948f09c1"));  // water
		Assertions.assertTrue(opIngredient.isPresent());

		// When
		RecipeIngredient recipeIngredient = RecipeIngredient.builder().amount(new BigDecimal(4))
						.unit(opUnit.get()).ingredient(opIngredient.get())
						.recipe(recipe).build();
		log.info("RecipeIngredientEntity to be persisted: {}", recipeIngredient);
		recipe.addRecipeIngredient(recipeIngredient);
		Recipe savedRecipe = recipeJpaRepository.saveAndFlush(recipe);

		// Then
		Assertions.assertEquals("Gulerodskage", savedRecipe.getName());
		assertThat("Majken").isEqualTo( savedRecipe.getCreatedBy());
		assertThat(now).isCloseTo(savedRecipe.getCreated(), within(0, ChronoUnit.SECONDS));
		assertThat(savedRecipe.getRecipeIngredients().size()).isEqualTo(1);
		
//		Assertions.assertEquals("Majken", recipeEntity.getCreatedBy());
		List<Recipe> recipes = recipeJpaRepository.findAllByCategoryName("Kager");
		Assertions.assertFalse(recipes.isEmpty());
		Assertions.assertEquals(2, recipes.size());

	}
		
	
	@Test
	@Sql("/db/test-data/insert_recipes.sql")
	@DisplayName("Given existing Recipe, When deleting Recipe, Then Recipe is removed")
	public void shouldDeleteRecipe() {
		// Given
		Optional<Recipe> reOptional = recipeJpaRepository.findById(UUID.fromString("4ea753a3-07ff-47ce-82bb-9dcc1aa25477"));
		Assertions.assertTrue(reOptional.isPresent());

		//When
		recipeJpaRepository.delete(reOptional.get());

		//Then
		reOptional = recipeJpaRepository.findById(UUID.fromString("4ea753a3-07ff-47ce-82bb-9dcc1aa25477"));
		Assertions.assertFalse(reOptional.isPresent());
		List<Recipe> recipes = recipeJpaRepository.findAll();
		Assertions.assertFalse(recipes.isEmpty());
		Assertions.assertEquals(6, recipes.size());
	}
	
	@Test
	@Sql("/db/test-data/insert_recipes.sql")
	@DisplayName("Given Recipe exist, When changing Name, Then all updated and updatedBy is changed")
	public void shouldUpdateNameOnRecipe() {
		// Given
		Optional<Recipe> reOptional = recipeJpaRepository.findById(UUID.fromString("195f5356-2230-4122-9a23-a266151f865c"));
		Assertions.assertTrue(reOptional.isPresent());

		// When
		reOptional.get().setName("Kylling og bacon rykker!");
		OffsetDateTime now = OffsetDateTime.now();
		Recipe saveAndFlush = recipeJpaRepository.saveAndFlush(reOptional.get());

		// Then
		Assertions.assertEquals("Kylling og bacon rykker!", saveAndFlush.getName());
		assertThat(now).isCloseTo(saveAndFlush.getUpdated(), within(0, ChronoUnit.SECONDS));
		assertThat("Majken").isEqualTo(saveAndFlush.getUpdatedBy());
	}
	
	@Test
	@Sql("/db/test-data/insert_recipes.sql")
	@DisplayName("Given Two Breads exists, When Fetching All By name contains 'brød', Then two are fetched")
	public void shouldReturnTwoRecipesWithNameContainsBread() {
		// When
		List<Recipe> opRecipes = recipeJpaRepository.findAllByNameContains("brød");

		// Then
		Assertions.assertTrue(!opRecipes.isEmpty());
		Assertions.assertEquals(3, opRecipes.size());
	}
	
	@Test
	@Sql({"/db/test-data/insert_recipes.sql", "/db/test-data/insert_recipe_ingredients.sql"})
	@DisplayName("Given Recipe with 6 Ingredients, When Recipe is Fetched, Then Recipe and All 6 RecipeIngredients are fetched")
	public void shouldReturnRecipeWithSixIngredients() {
		// When
		Optional<Recipe> reOptional = recipeJpaRepository.findById(UUID.fromString("ce07075c-38b4-4b52-831c-5a9ce105e4af"));  // Hvedebrød med Rugmel
		Assertions.assertTrue(reOptional.isPresent());
		Set<RecipeIngredient> recipeIngredients = reOptional.get().getRecipeIngredients();

		// Then
		Assertions.assertTrue(!recipeIngredients.isEmpty());
		Assertions.assertEquals(6, recipeIngredients.size());
	}
	
	
	@Test
	@Sql({"/db/test-data/insert_recipes.sql", "/db/test-data/insert_recipe_ingredients.sql"})
	@DisplayName("Given Recipe with Ingredient, When Recipe is Deleted, Then Recipe and All RecipeIngredients are deleted")
	public void shouldDeleteRecipeAndAllRecipeIngredients() {
		// Given
		Optional<Recipe> reOptional = recipeJpaRepository.findById(UUID.fromString("5d22c394-b5ce-48c3-8199-72ccc92c737c"));  // Fuldkorns hvedebrød
		Assertions.assertTrue(reOptional.isPresent());

		// When
		recipeJpaRepository.delete(reOptional.get());
		reOptional = recipeJpaRepository.findById(UUID.fromString("5d22c394-b5ce-48c3-8199-72ccc92c737c"));

		// Then
		Assertions.assertTrue(reOptional.isEmpty());
		
		// check for clean-up of recipeingrediententities
		List<RecipeIngredient> reIngOpList = recipeIngredientJpaRepository.findAllByRecipeId(UUID.fromString("5d22c394-b5ce-48c3-8199-72ccc92c737c"));
//		log.info("Recipe Ingredients size: {}", reIngOpList.size());
		Assertions.assertEquals(0,  reIngOpList.size());
		
		List<Recipe> recipes = recipeJpaRepository.findAll();
		Assertions.assertFalse(recipes.isEmpty());
		Assertions.assertEquals(6, recipes.size());
	}

	@Test
	@Sql({"/db/test-data/insert_recipes.sql", "/db/test-data/insert_recipe_ingredients.sql"})
	@DisplayName("Given Recipe with 6 Ingredients, When one RecipeIngredient is deleted and Recipe is saved, Then Recipe contains only 5 RecipeIngredients")
	public void shouldDeleteOneIngredientFromRecipe() {
		// Given
		Optional<Recipe> recipeOptional = recipeJpaRepository.findById(UUID.fromString("ce07075c-38b4-4b52-831c-5a9ce105e4af"));  // Hvedebrød med Rugmel
		Assertions.assertTrue(recipeOptional.isPresent());
		Set<RecipeIngredient> recipeIngredients = recipeOptional.get().getRecipeIngredients();
		Assertions.assertTrue(!recipeIngredients.isEmpty());
		Assertions.assertEquals(6, recipeIngredients.size());

		// When
		Optional<RecipeIngredient> recipeIngredient = recipeOptional.get().getRecipeIngredients().stream().findFirst();
		recipeOptional.get().getRecipeIngredients().remove(recipeIngredient.get());
		Recipe savedRecipe = recipeJpaRepository.saveAndFlush(recipeOptional.get());

		// Then
		assertThat(savedRecipe.getRecipeIngredients().size()).isEqualTo(5);
	}

	@Test
	@Sql({"/db/test-data/insert_recipes.sql", "/db/test-data/insert_recipe_ingredients.sql"})
	@DisplayName("Given Recipe with 6 Ingredients, When one RecipeIngredient is added and Recipe is saved, Then Recipe contains 7 RecipeIngredients")
	public void shouldAddOneIngredientToRecipe() {
		// Given
		Optional<Recipe> recipeOptional = recipeJpaRepository.findById(UUID.fromString("ce07075c-38b4-4b52-831c-5a9ce105e4af"));  // Hvedebrød med Rugmel
		Assertions.assertTrue(recipeOptional.isPresent());
		Set<RecipeIngredient> recipeIngredients = recipeOptional.get().getRecipeIngredients();
		Assertions.assertTrue(!recipeIngredients.isEmpty());
		Assertions.assertEquals(6, recipeIngredients.size());

		Optional<Unit> opUnit = unitJpaRepository.findById(UUID.fromString("c5173731-3a7e-498c-84b1-b2d3abe68cef"));  // gram
		Assertions.assertTrue(opUnit.isPresent());
		Optional<Ingredient> opIngredient = ingredientJpaRepository.findById(UUID.fromString("86a792c6-8432-470f-8ad1-b91a1499e91a"));  // Mozarella
		Assertions.assertTrue(opIngredient.isPresent());

		// When
		RecipeIngredient recipeIngredient = RecipeIngredient.builder().amount(new BigDecimal(50))
				.unit(opUnit.get()).ingredient(opIngredient.get())
				.recipe(recipeOptional.get()).build();
		log.info("RecipeIngredientEntity to be persisted: {}", recipeIngredient);
		recipeOptional.get().addRecipeIngredient(recipeIngredient);
		Recipe savedRecipe = recipeJpaRepository.saveAndFlush(recipeOptional.get());

		// Then
		assertThat(savedRecipe.getRecipeIngredients().size()).isEqualTo(7);
	}

	@Test
	@Sql({"/db/test-data/insert_recipes.sql", "/db/test-data/insert_recipe_ingredients.sql"})
	@DisplayName("Given Recipe with 6 Ingredients, When one RecipeIngredient is updated and Recipe is saved, Then Recipe contains the updated RecipeIngredient")
	public void shouldUpdateOneIngredientInRecipe() {
		// Given
		Optional<Recipe> recipeOptional = recipeJpaRepository.findById(UUID.fromString("ce07075c-38b4-4b52-831c-5a9ce105e4af"));  // Hvedebrød med Rugmel
		Assertions.assertTrue(recipeOptional.isPresent());
		Set<RecipeIngredient> recipeIngredients = recipeOptional.get().getRecipeIngredients();
		Assertions.assertTrue(!recipeIngredients.isEmpty());
		Assertions.assertEquals(6, recipeIngredients.size());

		// When
		Optional<RecipeIngredient> recipeIngredient = recipeIngredients.stream().findFirst(); // vand
		log.info("Recipe Ingredient: {}", recipeIngredient);
		recipeIngredient.get().setAmount(new BigDecimal(5)); // 5 dl
		Recipe savedRecipe = recipeJpaRepository.saveAndFlush(recipeOptional.get());

		// Then
		assertThat(savedRecipe.getRecipeIngredients().stream().filter( ri -> ri.getIngredient().getName().equals("Vand")).findFirst().get().getAmount()).isEqualTo(new BigDecimal(5));
		assertThat(savedRecipe.getRecipeIngredients().size()).isEqualTo(6);
	}

	@Test
	@Sql({"/db/test-data/insert_recipes.sql", "/db/test-data/insert_recipe_ratings.sql"})
	@DisplayName("Given new RecipeRating is added, When Recipe is saved, Then RecipeRating is also saved")
	public void shouldSaveRecipeRating() {
		// Given
		Optional<Recipe> recipeOptional = recipeJpaRepository.findById(UUID.fromString("06309a26-9ef8-43d2-82a0-f88e0be094e0"));  // Oelandshvede
		Assertions.assertTrue(recipeOptional.isPresent());
		List<RecipeRating> recipeRatings = recipeRatingJpaRepository.findAllByRecipeId(UUID.fromString("06309a26-9ef8-43d2-82a0-f88e0be094e0")); // all ratings for Oelandshvedebread
		assertThat(recipeRatings.size()).isEqualTo(6);

		Optional<Rating> rating = ratingJpaRepository.findById(UUID.fromString("26f09c94-79ec-439c-9776-d826efad187e")); // rating 5
		assertThat(rating.isPresent()).isTrue();

		// When
		RecipeRating recipeRating = new RecipeRating();
		recipeRating.setRecipe(recipeOptional.get());
		recipeRating.setRating(rating.get());
		recipeRating.setCreatedBy(session.getUserName());
		recipeRating.setDescription("Særklasse");
		recipeOptional.get().addRecipeRating(recipeRating);
		Recipe savedRecipe = recipeJpaRepository.saveAndFlush(recipeOptional.get());

		// Then
		assertThat(savedRecipe.getRecipeRatings().size()).isEqualTo(7);

	}

	private Recipe buildRecipeEntity() {
		Recipe recipe = new Recipe();
		recipe.setCategory(buildCategoryEntityKage());
		recipe.setName("Gulerodskage");
		recipe.setDescription("Lækker kage med lidt sundt indhold og creme på toppen");
		recipe.setInstructions("1. Hæld mælk op i røreskålen...");
		return recipe;
	}
	
	private Category buildCategoryEntityKage() {
		Optional<Category> categoryKage = categoryRepository.findByName("Kager");
		return categoryKage.get();
	} 
}
