package dk.serik.recipes.repository;

import dk.serik.recipes.bean.Session;
import dk.serik.recipes.model.Ingredient;
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

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

@DataJpaTest
@Slf4j
public class IngredientJpaRepositoryIT {
	
	@Autowired
	private IngredientJpaRepository repository;
	
	@MockitoBean
	private Session session;
	
	@BeforeEach
	public void setupTest() {
		Mockito.when(session.getUserName()).thenReturn("Majken");
	}
	
	@Test
	public void givenExistingIngredients_WhenFetchingAll_ThenReturnEight() {
		List<Ingredient> ingredients = repository.findAll();
		Assertions.assertFalse(ingredients.isEmpty());
		Assertions.assertEquals(9, ingredients.size());
	}
	
	@Test
	public void givenExistingIngredientWater_WhenFetchingByName_ThenOk() {
		Optional<Ingredient> opIngredient = repository.findByName("Vand");
		Assertions.assertTrue(opIngredient.isPresent());
		Assertions.assertEquals("Almindelig postevand fra hanen", opIngredient.get().getDescription());
	}
	
	@Test
	@DisplayName("Given existing Ingredients Flour, When fetching by Name Contains, Then three is returned")
	public void shouldReturnThreeIngredientsByName() {
		List<Ingredient> opIngredients = repository.findAllByNameContains("mel");
		Assertions.assertEquals(3, opIngredients.size());
	}

	@Test
	@DisplayName("Given Existing Ingredients 'KimAndSkaldele', When Fetching by Description Contains, Then three is returned")
	public void shouldReturnThreeIngredientsByDescription() {
		List<Ingredient> opIngredients = repository.findAllByDescriptionContains("skaldele og kim");
		Assertions.assertEquals(3, opIngredients.size());
	}
	
	@Test
	public void givenValidIngredient_WhenSaved_ThenOk() {		
		OffsetDateTime now = OffsetDateTime.now();
		Ingredient saveAndFlush = repository.saveAndFlush(buildIngredientEntity());
		Assertions.assertEquals("Hakket oksekød", saveAndFlush.getName());
		assertThat(now).isCloseTo(saveAndFlush.getCreated(), within(0, ChronoUnit.SECONDS));
		assertThat("Majken").isEqualTo(saveAndFlush.getCreatedBy());
		List<Ingredient> ingredients = repository.findAll();
		Assertions.assertFalse(ingredients.isEmpty());
		Assertions.assertEquals(10, ingredients.size());
	}
	
	@Test
	public void givenExistingIngredient_WhenDeleted_ThenOk() {
		Optional<Ingredient> opIngredient = repository.findById(UUID.fromString("e00aec55-2eb7-4eeb-a594-0bbb948f09c1"));
		Assertions.assertTrue(opIngredient.isPresent());
		repository.delete(opIngredient.get());
		opIngredient = repository.findById(UUID.fromString("e00aec55-2eb7-4eeb-a594-0bbb948f09c1"));
		Assertions.assertFalse(opIngredient.isPresent());
		
		List<Ingredient> ingredients = repository.findAll();
		Assertions.assertFalse(ingredients.isEmpty());
		Assertions.assertEquals(8, ingredients.size());
	}
	
	@Test
	public void givenExistingIngredient_WhenUpdateDescription_ThenOk() {
		Optional<Ingredient> opIngredient = repository.findById(UUID.fromString("9d2b8e95-e897-470a-b1dd-0cf03fb01465"));
		Assertions.assertTrue(opIngredient.isPresent());
		opIngredient.get().setDescription("Bare rugmel");
		Ingredient saveAndFlush = repository.saveAndFlush(opIngredient.get());
		Assertions.assertEquals("Majken", saveAndFlush.getUpdatedBy());
		Assertions.assertEquals("Bare rugmel", saveAndFlush.getDescription());
	}
	
	private Ingredient buildIngredientEntity() {
		Ingredient ingredient = new Ingredient();
		ingredient.setName("Hakket oksekød");
		ingredient.setDescription("Hakket oksekød fedt 4-7%");
		return ingredient;
	}
	
}
