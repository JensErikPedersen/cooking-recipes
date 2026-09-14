package dk.serik.recipes.repository;

import dk.serik.recipes.bean.Session;
import dk.serik.recipes.model.Category;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

//@ExtendWith(SpringExtension.class)
@DataJpaTest
@Slf4j
public class
CategoryJpaRepositoryIT {

	@Autowired
	private CategoryJpaRepository categoryRepository;
	
	@MockitoBean
	private Session session;
	
	@BeforeEach
	public void setupTest() {
		Mockito.when(session.getUserName()).thenReturn("Jens");
	}
	
	@ParameterizedTest
	@CsvSource(value={"Brød:Jens", "Kager:Majken"}, delimiter = ':')
	@DisplayName("Given existing Category, When Fetching Category by Name, Then Category is returned")
	public void shouldFindCategoryByName(String category, String createdBy) {
		Optional<Category> categoryEntity = categoryRepository.findByName(category);
		Assertions.assertTrue(categoryEntity.isPresent());
		Assertions.assertEquals(createdBy, categoryEntity.get().getCreatedBy());
	}
	
	
	@Test
	@DisplayName("Given new Category, When saved, Then Category is saved in Database")
	public void shouldSaveNewCategory() {
		OffsetDateTime now = OffsetDateTime.now();
		Category category = categoryRepository.saveAndFlush(buildWienerBreadEntity());
		assertThat(now).isCloseTo(category.getCreated(), within(0, ChronoUnit.SECONDS));
		assertThat("Jens").isEqualTo(category.getCreatedBy());
		Assertions.assertEquals("Wienerbrød", category.getName());
	}
	
	@Test
	@DisplayName("Given four Categories exist, When Fetching all, Then all four is returned")
	public void shouldReturnFourCategories() {
		List<Category> categoryEntities = categoryRepository.findAll();
		Assertions.assertFalse(categoryEntities.isEmpty());
		Assertions.assertEquals(4, categoryEntities.size());
	}
	
	@Test
	@DisplayName("Given existing Category, When category name is updated, Then name and updated info is updated")
	public void shouldUpdateNameAndUpdatedInfo() {
		Optional<Category> categoryEntity = categoryRepository.findByName("Brød");
		Assertions.assertTrue(categoryEntity.isPresent());
		categoryEntity.get().setName("Bread");
		OffsetDateTime now = OffsetDateTime.now();
		Category savedCategory = categoryRepository.saveAndFlush(categoryEntity.get());
		Assertions.assertEquals("Bread", savedCategory.getName());
		assertThat(now).isCloseTo(savedCategory.getUpdated(), within(0, ChronoUnit.SECONDS));
		assertThat("Jens").isEqualTo(savedCategory.getUpdatedBy());
	}
	
	@Test
	@DisplayName("Given existing categories containing 'er' in name, When finding all by name contains 'er', Then two categories is returned")
	public void shouldReturnTwoCategoriesWhenFetchByName() {
		List<Category> opCategories = categoryRepository.findAllByNameContains("er");
		Assertions.assertEquals(2, opCategories.size());
	}
	
	private Category buildWienerBreadEntity() {
		Category category = new Category();
		category.setName("Wienerbrød");
		return category;
	}
}
