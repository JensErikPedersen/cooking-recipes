package dk.serik.recipes.service;

import org.springframework.http.HttpStatus;
import dk.serik.recipes.exceptions.ApplicationErrorCodes;
import dk.serik.recipes.bean.Session;
import dk.serik.recipes.dto.IngredientDTO;
import dk.serik.recipes.exceptions.ServiceException;
import dk.serik.recipes.mockutil.MockIngredientUtil;
import dk.serik.recipes.model.Ingredient;
import dk.serik.recipes.repository.IngredientJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
public class IngredientServiceTest {
	
	@InjectMocks
	private IngredientServiceImpl service;
	
	@Mock
	private IngredientJpaRepository repository;

	@Mock
	private Session session;

	@Captor
	ArgumentCaptor<Ingredient> ingredientCaptor;
	@BeforeEach
	public void setupTest() {
		lenient().when(session.getUserName()).thenReturn("Jens");
	}

	@Test
	@DisplayName("Context is checked ok")
	public void contextIsOk() {
		assertThat(service).isNotNull();
		assertThat(repository).isNotNull();
	}

	@Test
	@DisplayName("Given 2 ingredients, When fetching all, Then 2 is returned")
	public void shouldFindAllIngredients() {
		// Given
		when(repository.findAll()).thenReturn(mockList());

		// When
		List<IngredientDTO> expected = service.findAll();

		// Then
		assertThat(expected).isNotEmpty();
		assertThat(expected.size()).isEqualTo(2);
		assertThat(expected).containsExactlyElementsOf(mockDTOList());
		verify(repository, times(1)).findAll();
	}

	@Test
	@DisplayName("Given no ingredients, When fetching all, Then none is returned")
	public void shouldNotReturnAny() {
		// Given
		when(repository.findAll()).thenReturn(new ArrayList<>());

		// When
		List<IngredientDTO> expected = service.findAll();

		// Then
		assertThat(expected).isEmpty();
		verify(repository, times(1)).findAll();
	}

	@Test
	@DisplayName("Given existing Ingredient, When fetching by Id, Then Ingredient is returned")
	public void shouldReturnIngredientById() {
		// Given
		when(repository.findById(UUID.fromString("01a50907-8141-4dd1-acdf-c4384669c2b2")))
				.thenReturn(Optional.of(MockIngredientUtil.mockHavsalt()));

		// When
		Optional<IngredientDTO> ingredientDTO = service.findById("01a50907-8141-4dd1-acdf-c4384669c2b2");

		// Then
		verify(repository, times(1)).findById(any());
		assertThat(ingredientDTO).isPresent();
		assertThat(ingredientDTO.get().getName()).isEqualTo("Havsalt");
	}

	@Test
	@DisplayName("Given no Ingredient, When fetching by Id, Then no Ingredient is returned")
	public void shouldReturnNothingById() {
		// Given
		when(repository.findById(UUID.fromString("01a50907-8141-4dd1-acdf-c4384669c2b2")))
				.thenReturn(Optional.empty());

		// When
		Optional<IngredientDTO> ingredientDTO = service.findById("01a50907-8141-4dd1-acdf-c4384669c2b2");

		// Then
		verify(repository, times(1)).findById(any());
		assertThat(ingredientDTO).isEmpty();
	}

	@Test
	@DisplayName("Given existing Ingredient, When fetching by invalid Id, Then a bad request is reported")
	public void shouldReturnExceptionWhenFetchingByInvalidId() {
		// a malformed id is a client error, not a raw IllegalArgumentException escaping the service
		assertThatThrownBy(() -> service.findById("not-valid-uuid"))
				.isInstanceOf(ServiceException.class)
				.hasMessageContaining("is not a valid UUID")
				.extracting("httpStatus")
				.isEqualTo(HttpStatus.BAD_REQUEST);
	}

	@Test
	@DisplayName("Given existing Ingredient named 'Havsalt', When fetching by Name, Then Ingredient Havsalt is returned")
	public void shouldReturnIngredientByName() {
		// Given
		when(repository.findByName("Havsalt")).thenReturn(Optional.of(MockIngredientUtil.mockHavsalt()));
		// When
		Optional<IngredientDTO> ingredientDTO = service.findByName("Havsalt");

		//Then
		verify(repository, times(1)).findByName(any());
		assertThat(ingredientDTO.isPresent()).isTrue();
		assertThat(ingredientDTO.get().getName()).isEqualTo("Havsalt");
	}

	@Test
	@DisplayName("Given no Ingredient with Name 'Havsalt', When Fetching by name 'Havsalt', Then no Ingredient is returned")
	public void shouldNotReturnIngredientByName() {
		// Given
		when(repository.findByName("Havsalt")).thenReturn(Optional.empty());

		//When
		Optional<IngredientDTO> ingredientDTO = service.findByName("Havsalt");

		// Then
		assertThat(ingredientDTO.isEmpty()).isTrue();
	}

	@Test
	@DisplayName("Given existing Ingredient, When fetching by name null, Then Optional Empty is Returned")
	public void shouldReturnOptionalEmpty() {
		// When
		Optional<IngredientDTO> ingredientDTO = service.findByName(null);

		// Then
		assertThat(ingredientDTO.isEmpty()).isTrue();
	}

	@Test
	@DisplayName("Given existing ingredient 'Havsalt' and 'Groft salt', When searching by 'salt', Then both Ingredients containing partial name is returned")
	public void shouldReturnBothIngredientsByPartialName() {
		// Given
		given(repository.findAllByNameContains("s")).willReturn(mockList());

		//When
		List<IngredientDTO> ingredientDTOS = service.findAllByNameContains("s");

		// Then
		then(repository).should(times(1)).findAllByNameContains(any());
		assertThat(ingredientDTOS.size()).isEqualTo(2);
		assertThat(ingredientDTOS).containsExactlyElementsOf(mockDTOList());
	}

	@Test
	@DisplayName("Given existing Ingredients exist, When searching for partial name not found in any, Then Empty is returned")
	public void shouldReturnEmpty() {
		// Given
		given(repository.findAllByNameContains("mel")).willReturn(List.of());

		// When
		List<IngredientDTO> ingredientDTOS = service.findAllByNameContains("mel");

		// Then
		then(repository).should(times(1)).findAllByNameContains("mel");
		assertThat(ingredientDTOS.isEmpty()).isTrue();
	}

	@Test
	@DisplayName("Given existing Ingredients, When searching with null, Then Empty is returned")
	public void shouldReturnEmptyWhenSearchingWithNull() {
		// Given
		given(repository.findAllByNameContains(null)).willReturn(List.of());

		// When
		List<IngredientDTO> ingredientDTOS = service.findAllByNameContains(null);

		// Then
		then(repository).should(times(1)).findAllByNameContains(null);
		assertThat(ingredientDTOS.isEmpty()).isTrue();
	}

	@Test
	@DisplayName("Given valid new Ingredient, When saved, Then new Ingredient is returned")
	public void shouldBeSavedAndReturned() {
		// Given
		given(repository.save(any())).willReturn(MockIngredientUtil.mockHavsalt());

		// When
		IngredientDTO savedDto = service.save(MockIngredientUtil.mockNewHavsaltDTO());

		// Then
		then(repository).should(times(1)).save(any());
		assertThat(savedDto).isNotNull();
		assertThat(savedDto.getId()).isEqualTo("01a50907-8141-4dd1-acdf-c4384669c2b2");
		assertThat(savedDto.getCreatedBy()).isEqualTo("Jens");
	}

	@Test
	@DisplayName("Given Ingredient is Null, When saved, Then a ServiceException is thrown")
	public void shouldThrowExceptionWhenIngredientIsNull() {
		assertThrows(ServiceException.class, () -> {
			service.save(null);
		}, "Ingredient is Null");
	}

	@Test
	@DisplayName("Given existing Ingredient, When deleted, Then true is returned")
	public void shouldReturnTrueWhenDeleted() {
		// Given
		given(repository.findById(UUID.fromString("381e5cd5-0a5d-48d2-b69c-71516254937e"))).willReturn(Optional.of(MockIngredientUtil.mockHvedemel()));

		// When
		boolean isDeleted = service.delete("381e5cd5-0a5d-48d2-b69c-71516254937e");

		// Then
		then(repository).should(times(1)).findById(any());
		assertThat(isDeleted).isTrue();
		then(repository).should(times(1)).delete(MockIngredientUtil.mockHvedemel());
	}

	@Test
	@DisplayName("Given Ingredient not found, When deleted, Then false is returned")
	public void shouldReturnFalseWhenDeleteUnknown() {
		// Given
		given(repository.findById(UUID.fromString("381e5cd5-0a5d-48d2-b69c-71516254937e"))).willReturn(Optional.empty());

		// When
		boolean isDeleted = service.delete("381e5cd5-0a5d-48d2-b69c-71516254937e");

		// Then
		then(repository).should(times(1)).findById(UUID.fromString("381e5cd5-0a5d-48d2-b69c-71516254937e"));
		assertThat(isDeleted).isFalse();
	}

	@Test
	@DisplayName("Given id is null, When deleted, Then service exception is thrown")
	public void shouldThrowExceptionWhenIdIsNull() {
		assertThrows(ServiceException.class, () -> {
			service.delete(null);
		}, "Ingredient Id is Null");
	}

	@Test
	@DisplayName("Given existing Ingredient, When updating description, Then return updated Ingredient")
	public void shouldUpdateDescriptionAndReturnUpdated() {
		// Given
		given(repository.findById(UUID.fromString("01a50907-8141-4dd1-acdf-c4384669c2b2"))).willReturn(Optional.of(MockIngredientUtil.mockHavsalt()));
		given(repository.save(any())).willReturn(MockIngredientUtil.mockHavsaltUpdated());
		// When
		IngredientDTO updated = service.update(MockIngredientUtil.mockHavsaltDTOToUpdate());
		// Then
		then(repository).should(times(1)).findById(UUID.fromString("01a50907-8141-4dd1-acdf-c4384669c2b2"));
		assertThat(updated).isNotNull();
		then(repository).should().save(ingredientCaptor.capture());
		assertThat(ingredientCaptor.getValue().getDescription()).isEqualTo(MockIngredientUtil.mockHavsaltDTOToUpdate().getDescription());
		assertThat(ingredientCaptor.getValue().getId().toString()).isEqualTo(MockIngredientUtil.mockHavsaltDTOToUpdate().getId());
		assertThat(updated.getDescription()).isEqualTo("Havsalt fra middelhavet i store flager");
	}

	@Test
	@DisplayName("Given Ingredient do not exist, When updating description, Then throw exception")
	public void shouldThrowExceptionWhenUpdatingNonExistingIngredient() {
		// Given
		given(repository.findById(UUID.fromString("5f01d434-5a68-4359-9f2e-0a6793dce48d"))).willReturn(Optional.empty());
		//When / Then
		assertThrows(ServiceException.class, () -> {
			service.update(MockIngredientUtil.mockHvedemelDTO());
		}, "Could not update Ingredient with id 5f01d434-5a68-4359-9f2e-0a6793dce48d since it was not found");
	}

	private List<Ingredient> mockList() {
		List<Ingredient> mocks = new ArrayList<>();
		mocks.add(MockIngredientUtil.mockHvedemel());
		mocks.add(MockIngredientUtil.mockHavsalt());
		return mocks;
	}


	private List<IngredientDTO> mockDTOList() {
		List<IngredientDTO> mocks = new ArrayList<>();
		mocks.add(MockIngredientUtil.mockHvedemelDTO());
		mocks.add(MockIngredientUtil.mockHavsaltDTO());
		return mocks;
	}

    @Test
    @DisplayName("Given IngredientDTO is null, When saving, Then a bad request is reported")
    public void shouldRejectSaveOfNullIngredient() {
        assertThatThrownBy(() -> service.save(null))
                .isInstanceOf(ServiceException.class)
                .extracting("code", "httpStatus")
                .containsExactly(ApplicationErrorCodes.INGREDIENT_DTO_IS_NULL.getCode(), HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("Given IngredientDTO is null, When updating, Then a bad request is reported")
    public void shouldRejectUpdateOfNullIngredient() {
        assertThatThrownBy(() -> service.update(null))
                .isInstanceOf(ServiceException.class)
                .extracting("code", "httpStatus")
                .containsExactly(ApplicationErrorCodes.INGREDIENT_ID_IS_NULL.getCode(), HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("Given IngredientDTO has no id, When updating, Then a bad request is reported")
    public void shouldRejectUpdateOfIngredientWithoutId() {
        assertThatThrownBy(() -> service.update(IngredientDTO.builder().build()))
                .isInstanceOf(ServiceException.class)
                .extracting("code", "httpStatus")
                .containsExactly(ApplicationErrorCodes.INGREDIENT_ID_IS_NULL.getCode(), HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("Given id is null, When deleting, Then a bad request is reported")
    public void shouldRejectDeleteOfNullIngredientId() {
        assertThatThrownBy(() -> service.delete(null))
                .isInstanceOf(ServiceException.class)
                .extracting("code", "httpStatus")
                .containsExactly(ApplicationErrorCodes.INGREDIENT_ID_IS_NULL.getCode(), HttpStatus.BAD_REQUEST);
    }


    @Test
    @DisplayName("Given a malformed id, When fetching a Ingredient by id, Then a bad request is reported")
    public void shouldRejectMalformedIngredientIdOnFindById() {
        assertThatThrownBy(() -> service.findById("not-a-uuid"))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("is not a valid UUID")
                .extracting("code", "httpStatus")
                .containsExactly(ApplicationErrorCodes.INGREDIENT_ID_IS_NULL.getCode(), HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("Given a malformed id, When deleting a Ingredient, Then a bad request is reported")
    public void shouldRejectMalformedIngredientIdOnDelete() {
        assertThatThrownBy(() -> service.delete("not-a-uuid"))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("is not a valid UUID")
                .extracting("code", "httpStatus")
                .containsExactly(ApplicationErrorCodes.INGREDIENT_ID_IS_NULL.getCode(), HttpStatus.BAD_REQUEST);
    }

}
