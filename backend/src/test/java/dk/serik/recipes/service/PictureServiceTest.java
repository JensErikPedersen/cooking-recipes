package dk.serik.recipes.service;

import dk.serik.recipes.bean.Session;
import dk.serik.recipes.dto.PictureDTO;
import dk.serik.recipes.exceptions.ApplicationErrorCodes;
import dk.serik.recipes.exceptions.ServiceException;
import dk.serik.recipes.model.Ingredient;
import dk.serik.recipes.model.Picture;
import dk.serik.recipes.model.Recipe;
import dk.serik.recipes.repository.IngredientJpaRepository;
import dk.serik.recipes.repository.PictureJpaRepository;
import dk.serik.recipes.repository.RecipeJpaRepository;
import dk.serik.recipes.repository.WelcomePageJpaRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;
import java.util.UUID;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PictureServiceTest {

    private static final UUID ID = UUID.fromString("0f4f78b9-b861-40a5-b4be-fa95c70ec50c");

    @Mock
    private PictureJpaRepository repository;

    @Mock
    private PictureStorage storage;

    @Mock
    private RecipeJpaRepository recipeRepository;

    @Mock
    private IngredientJpaRepository ingredientRepository;

    @Mock
    private WelcomePageJpaRepository welcomePageRepository;

    @Mock
    private Session session;

    @InjectMocks
    private PictureServiceImpl service;

    @Test
    @DisplayName("Given a picture on a recipe, an ingredient and the Welcome page, When found by id, Then it says where it is used")
    void shouldSayWhereItIsUsed() {
        when(repository.findById(ID)).thenReturn(Optional.of(picture()));
        uses(1, 1, true);

        PictureDTO found = service.findById(ID.toString()).orElseThrow();

        assertThat(found.getRecipes()).extracting("name").containsExactly("Recipe 0");
        assertThat(found.getIngredients()).extracting("name").containsExactly("Ingredient 0");
        assertThat(found.getWelcomePage()).isTrue();
    }

    @Test
    @DisplayName("Given an unused picture, When found by id, Then empty uses, not absent ones")
    void shouldSayItIsUnused() {
        when(repository.findById(ID)).thenReturn(Optional.of(picture()));
        uses(0, 0, false);

        PictureDTO found = service.findById(ID.toString()).orElseThrow();

        assertThat(found.getRecipes()).isEmpty();
        assertThat(found.getIngredients()).isEmpty();
        assertThat(found.getWelcomePage()).isFalse();
    }

    @Test
    @DisplayName("Given a new name and alt text, When updated, Then both are written and nothing else")
    void shouldUpdateNameAndAltText() {
        Picture picture = picture();
        when(repository.findById(ID)).thenReturn(Optional.of(picture));
        when(repository.saveAndFlush(picture)).thenReturn(picture);
        uses(0, 0, false);

        PictureDTO updated = service.update(PictureDTO.builder().id(ID.toString()).name("Ny").altText("Ny alt").build());

        assertThat(updated.getName()).isEqualTo("Ny");
        assertThat(updated.getAltText()).isEqualTo("Ny alt");
        verify(storage, never()).write(any(), any());
    }

    @Test
    @DisplayName("Given no such picture, When updated, Then 404")
    void shouldRefuseUpdatingAnUnknownPicture() {
        when(repository.findById(ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(PictureDTO.builder().id(ID.toString()).name("Ny").altText("Ny").build()))
                .isInstanceOf(ServiceException.class)
                .extracting("httpStatus", "code")
                .containsExactly(HttpStatus.NOT_FOUND, ApplicationErrorCodes.PICTURE_NOT_FOUND.getCode());
    }

    @ParameterizedTest(name = "{3}")
    @CsvSource(delimiter = '|', value = {
            "1 | 0 | false | Picture 'Rugbrød' is used by 1 recipe and cannot be deleted",
            "0 | 2 | false | Picture 'Rugbrød' is used by 2 ingredients and cannot be deleted",
            "0 | 0 | true  | Picture 'Rugbrød' is used by the Welcome page and cannot be deleted",
            "2 | 0 | true  | Picture 'Rugbrød' is used by 2 recipes and the Welcome page and cannot be deleted",
            "3 | 1 | true  | Picture 'Rugbrød' is used by 3 recipes, 1 ingredient and the Welcome page and cannot be deleted"})
    @DisplayName("Given a picture in use, When deleted, Then 409 naming its uses, and neither the row nor the files go")
    void shouldRefuseDeletingAPictureInUse(int recipes, int ingredients, boolean welcomePage, String message) {
        when(repository.findById(ID)).thenReturn(Optional.of(picture()));
        uses(recipes, ingredients, welcomePage);

        assertThatThrownBy(() -> service.delete(ID.toString()))
                .isInstanceOf(ServiceException.class)
                .hasMessage(message)
                .extracting("httpStatus", "code")
                .containsExactly(HttpStatus.CONFLICT, ApplicationErrorCodes.PICTURE_IN_USE.getCode());
        verify(repository, never()).delete(any());
        verify(storage, never()).delete(any());
    }

    @Test
    @DisplayName("Given an unused picture, When deleted, Then the row and the files go")
    void shouldDeleteAnUnusedPicture() {
        Picture picture = picture();
        when(repository.findById(ID)).thenReturn(Optional.of(picture));
        uses(0, 0, false);

        assertThat(service.delete(ID.toString())).isTrue();

        verify(repository).delete(picture);
        verify(storage).delete(ID);
    }

    private void uses(int recipes, int ingredients, boolean welcomePage) {
        when(recipeRepository.findAllByPictureIdOrderByName(ID)).thenReturn(IntStream.range(0, recipes).mapToObj(i -> {
            Recipe recipe = Recipe.builder().name("Recipe " + i).build();
            ReflectionTestUtils.setField(recipe, "id", UUID.randomUUID());
            return recipe;
        }).toList());
        when(ingredientRepository.findAllByPictureIdOrderByName(ID)).thenReturn(IntStream.range(0, ingredients).mapToObj(i -> {
            Ingredient ingredient = Ingredient.builder().name("Ingredient " + i).build();
            ReflectionTestUtils.setField(ingredient, "id", UUID.randomUUID());
            return ingredient;
        }).toList());
        when(welcomePageRepository.existsByPictureId(ID)).thenReturn(welcomePage);
    }

    private static Picture picture() {
        Picture picture = new Picture();
        picture.setName("Rugbrød");
        picture.setAltText("Et rugbrød");
        ReflectionTestUtils.setField(picture, "id", ID);
        return picture;
    }
}
