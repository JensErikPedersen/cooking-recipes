package dk.serik.recipes.service;

import org.springframework.http.HttpStatus;
import dk.serik.recipes.exceptions.ApplicationErrorCodes;
import dk.serik.recipes.bean.Session;
import dk.serik.recipes.dto.CategoryDTO;
import dk.serik.recipes.exceptions.ServiceException;
import dk.serik.recipes.mockutil.MockCategoryUtil;
import dk.serik.recipes.model.Category;
import dk.serik.recipes.repository.CategoryJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CategoryServiceTest {
	
	@InjectMocks
	private CategoryServiceImpl service;
	
	@Mock
	private CategoryJpaRepository repository;

	@Mock
	private Session session;
	
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
	@DisplayName("Given No Categories When FindAll Then list size is Zero")
	public void givenNoCategories_WhenFindAll_ThenListSizeIsZero() {
		when(repository.findAll()).thenReturn(new ArrayList<>());
		assertThat(service.findAll()).isEmpty();
		verify(repository, times(1)).findAll();
	}
	
	@Test
	@DisplayName("Given one Category, When FindAll, Then list size is One")
	public void givenOneCategory_WhenFindAll_ThenListSizeIsOne() throws Exception{

		// given
		List<Category> all = new ArrayList<>();
		all.add(MockCategoryUtil.dessertToBeSaved());

		// when
		when(repository.findAll()).thenReturn(all);
		List<CategoryDTO> findAll = service.findAll();

		// then
		assertThat(findAll).isNotEmpty();
		assertThat(findAll.size()).isEqualTo(1);
		verify(repository, times(1)).findAll();
	}

	
	@Test
	@DisplayName("Given No Categories When FindById Then none is found")
	public void shouldReturnNone() {
		when(repository.findById(UUID.fromString("29af0d97-b11c-4ca4-82f5-e938a1234c25"))).thenReturn(Optional.empty());
		assertThat(service.findById("29af0d97-b11c-4ca4-82f5-e938a1234c25")).isEmpty();
		verify(repository).findById(any());
	}
	
	@Test
	@DisplayName("Given Known Category, When FindById, Then one is found")
	public void shouldReturnCategoryWhenFindById() {
		when(repository.findById(UUID.fromString("29af0d97-b11c-4ca4-82f5-e938a1234c25"))).thenReturn(Optional.of(MockCategoryUtil.dessertToBeSaved()));
		assertThat(service.findById("29af0d97-b11c-4ca4-82f5-e938a1234c25")).isPresent();
		verify(repository).findById(any());
	}
	
	@Test
	@DisplayName("Given Known Category, When FindByName, Then none is found")
	public void shouldReturnNoneWhenFindByName() {
		lenient().when(repository.findByName("Vand")).thenReturn(Optional.of(MockCategoryUtil.dessertToBeSaved()));  // name: Water
		assertThat(service.findByName("Brød")).isEmpty();
		verify(repository).findByName("Brød");
	}
	
	@Test
	@DisplayName("Given Known Category When FindByName Then one is found")
	public void shouldReturnOneWhenFindByName() {
		//Given
		when(repository.findByName("Dessert")).thenReturn(Optional.of(MockCategoryUtil.dessertToBeSaved()));  // name: Dessert

		//When
		assertThat(service.findByName("Dessert")).isPresent();

		//Then
		verify(repository).findByName(any());
	}

	@Test
	@DisplayName("Given Valid Category DTO When saving Category Then valid category DTO is returned")
	public void shoukdSaveCategory() {
		// Given
		when(repository.save(any())).thenReturn(MockCategoryUtil.mockDessert());

		//When
		CategoryDTO savedDto = service.save(MockCategoryUtil.mockToBeSavedDessertDTO());

		//Then
		assertThat(savedDto).isNotNull();
		assertThat(savedDto.getId()).isNotNull();
		assertThat(savedDto.getName()).isEqualTo("Dessert");
		assertThat(savedDto.getCreatedBy()).isEqualTo("Jens");
		verify(repository, times(1)).save(any());
	}

	@Test
	@DisplayName("Given Valid Category, When updating Description, Then Description is saved ok")
	public void shouldUpdateDescriptionOk() {
		// Given
		when(repository.findById(UUID.fromString("913a5159-3717-4b9d-a290-0158d31ea8aa"))).thenReturn(Optional.of(MockCategoryUtil.mockDessert()));
		when(repository.save(any())).thenReturn(MockCategoryUtil.mockUpdatedDessert());
		//When
		CategoryDTO dto = service.update(MockCategoryUtil.mockToBeUpdatedDessertDTO());

		// Then
		verify(repository, Mockito.times(1)).findById(any());
		verify(repository, Mockito.times(1)).save(any());
		assertThat(dto).isNotNull();
		assertThat(dto.getDescription()).isEqualTo(MockCategoryUtil.mockToBeUpdatedDessertDTO().getDescription());
	}

	@Test
	@DisplayName("Given Category do Not Exist, When Updating Description, Then Exception is thrown")
	public void shouldThrowExceptionWhenUpdatingCategoryThatNotExist() {
		// Given
		when(repository.findById(UUID.fromString("913a5159-3717-4b9d-a290-0158d31ea8aa"))).thenReturn(Optional.empty());

		// When
		assertThatThrownBy(() -> service.update(MockCategoryUtil.mockToBeUpdatedDessertDTO()))
				.isInstanceOf(ServiceException.class)
				.hasMessageContaining("Category with id 913a5159-3717-4b9d-a290-0158d31ea8aa could not be found");
	}

	@Test
	@DisplayName("Given existing Category, When deleted, Then removed from database")
	public void shouldBeDeletedOk() {
		// Given
		when(repository.findById(UUID.fromString("913a5159-3717-4b9d-a290-0158d31ea8aa"))).thenReturn(Optional.of(MockCategoryUtil.mockDessert()));

		// When
		boolean delete = service.delete("913a5159-3717-4b9d-a290-0158d31ea8aa");

		// Then
		assertThat(delete).isTrue();
		verify(repository, Mockito.times(1)).delete(MockCategoryUtil.mockDessert());
		verify(repository, Mockito.times(1)).findById(UUID.fromString("913a5159-3717-4b9d-a290-0158d31ea8aa"));

	}

	@Test
	@DisplayName("Given not existing Category, When deleted, Then false is returned")
	public void shouldNotBeDeleted() {
		// Given
		when(repository.findById(UUID.fromString("913a5159-3717-4b9d-a290-0158d31ea8aa"))).thenReturn(Optional.empty());

		// When
		boolean delete = service.delete("913a5159-3717-4b9d-a290-0158d31ea8aa");

		// Then
		assertThat(delete).isFalse();
		verify(repository, Mockito.times(1)).findById(UUID.fromString("913a5159-3717-4b9d-a290-0158d31ea8aa"));

	}


    @Test
    @DisplayName("Given CategoryDTO is null, When saving, Then a bad request is reported")
    public void shouldRejectSaveOfNullCategory() {
        assertThatThrownBy(() -> service.save(null))
                .isInstanceOf(ServiceException.class)
                .extracting("code", "httpStatus")
                .containsExactly(ApplicationErrorCodes.CATEGORY_DTO_IS_NULL.getCode(), HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("Given CategoryDTO is null, When updating, Then a bad request is reported")
    public void shouldRejectUpdateOfNullCategory() {
        assertThatThrownBy(() -> service.update(null))
                .isInstanceOf(ServiceException.class)
                .extracting("code", "httpStatus")
                .containsExactly(ApplicationErrorCodes.CATEGORY_ID_IS_NULL.getCode(), HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("Given CategoryDTO has no id, When updating, Then a bad request is reported")
    public void shouldRejectUpdateOfCategoryWithoutId() {
        assertThatThrownBy(() -> service.update(CategoryDTO.builder().build()))
                .isInstanceOf(ServiceException.class)
                .extracting("code", "httpStatus")
                .containsExactly(ApplicationErrorCodes.CATEGORY_ID_IS_NULL.getCode(), HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("Given id is null, When deleting, Then a bad request is reported")
    public void shouldRejectDeleteOfNullCategoryId() {
        assertThatThrownBy(() -> service.delete(null))
                .isInstanceOf(ServiceException.class)
                .extracting("code", "httpStatus")
                .containsExactly(ApplicationErrorCodes.CATEGORY_ID_IS_NULL.getCode(), HttpStatus.BAD_REQUEST);
    }


    @Test
    @DisplayName("Given a malformed id, When fetching a Category by id, Then a bad request is reported")
    public void shouldRejectMalformedCategoryIdOnFindById() {
        assertThatThrownBy(() -> service.findById("not-a-uuid"))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("is not a valid UUID")
                .extracting("code", "httpStatus")
                .containsExactly(ApplicationErrorCodes.CATEGORY_ID_IS_NULL.getCode(), HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("Given a malformed id, When deleting a Category, Then a bad request is reported")
    public void shouldRejectMalformedCategoryIdOnDelete() {
        assertThatThrownBy(() -> service.delete("not-a-uuid"))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("is not a valid UUID")
                .extracting("code", "httpStatus")
                .containsExactly(ApplicationErrorCodes.CATEGORY_ID_IS_NULL.getCode(), HttpStatus.BAD_REQUEST);
    }

}
