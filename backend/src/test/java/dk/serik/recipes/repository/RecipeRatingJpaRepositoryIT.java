package dk.serik.recipes.repository;

import dk.serik.recipes.bean.Session;
import dk.serik.recipes.model.Rating;
import dk.serik.recipes.model.Recipe;
import dk.serik.recipes.model.RecipeRating;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.jdbc.Sql;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

@DataJpaTest
@Slf4j
public class RecipeRatingJpaRepositoryIT {
    @Autowired
    private RecipeRatingJpaRepository recipeRatingJpaRepository;
    @Autowired
    private RecipeJpaRepository recipeJpaRepository;
    @Autowired
    private RatingJpaRepository ratingJpaRepository;
    @MockitoBean
    private Session session;
    @BeforeEach
    public void setupTest() {
        Mockito.when(session.getUserName()).thenReturn("Jens");
    }

    @Test
    @DisplayName("Given Recipe Oelandshvede and Ratings exist, When fetching all Ratings 5 for Recipe Oelandshvede, Then 3 is fetched")
    @Sql({"/db/test-data/insert_recipes.sql", "/db/test-data/insert_recipe_ratings.sql"})
    public void givenRecipeAndRatingsExist_WhenFetchingAllRatings5Then3IsFetched() {
        //When
        List<RecipeRating> recipeRatings = recipeRatingJpaRepository.findAllByRecipeIdAndRatingId(UUID.fromString("06309a26-9ef8-43d2-82a0-f88e0be094e0"), UUID.fromString("26f09c94-79ec-439c-9776-d826efad187e"));

        //Then
        assertThat(recipeRatings.size()).isEqualTo(3);
    }

    @Test
    @DisplayName("Given Recipe Oelandshvedebread exist, When fetching all Ratings for Recipe Oelandshvedebread, Then 6 is fetched")
    @Sql({"/db/test-data/insert_recipes.sql", "/db/test-data/insert_recipe_ratings.sql"})
    public void givenRecipeExist_WhenFetchingAllRatingsThen6IsFetched() {
        //When
        List<RecipeRating> recipeRatings = recipeRatingJpaRepository.findAllByRecipeId(UUID.fromString("06309a26-9ef8-43d2-82a0-f88e0be094e0")); // all ratings for Oelandshvedebread

        //Then
        assertThat(recipeRatings.size()).isEqualTo(6);
    }

    @Test
    @DisplayName("Given Ratings exist, When fetching all 5 Ratings, Then 3 is fetched")
    @Sql({"/db/test-data/insert_recipes.sql", "/db/test-data/insert_recipe_ratings.sql"})
    public void givenRatingsExist_WhenFetchingAllRatings5Then3IsFetched() {
        //When
        List<RecipeRating> recipeRatings = recipeRatingJpaRepository.findAllByRatingId(UUID.fromString("26f09c94-79ec-439c-9776-d826efad187e")); // all 5 ratings

        //Then
        assertThat(recipeRatings.size()).isEqualTo(3);
    }


    @Test
    @DisplayName("Given Rating on Recipe Oelandshvedemel, When deleting one Rating, Then only one Rating is deleted")
    @Sql({"/db/test-data/insert_recipes.sql", "/db/test-data/insert_recipe_ratings.sql"})
    public void givenRatingExist_WhenDeleted_ThenOnlyOneIsDeleted() {
        // Given
        Optional<RecipeRating> recipeRating = recipeRatingJpaRepository.findById(UUID.fromString("2dce3031-9ead-454c-9987-8f548ccaa70b"));
        assertThat(recipeRating.isPresent()).isTrue();
        List<RecipeRating> allRecipeRatings = recipeRatingJpaRepository.findAll();
        assertThat(allRecipeRatings.size()).isEqualTo(10);

        // When
        recipeRatingJpaRepository.delete(recipeRating.get());

        // Then
        recipeRating = recipeRatingJpaRepository.findById(UUID.fromString("2dce3031-9ead-454c-9987-8f548ccaa70b"));
        assertThat(recipeRating.isPresent()).isFalse();
        allRecipeRatings = recipeRatingJpaRepository.findAll();
        assertThat(allRecipeRatings.size()).isEqualTo(9);
    }

    @Test
    @DisplayName("Given Recipe Oelandshvedemel, When adding Rating of 5, Then 4 Ratings of 5 exists")
    @Sql({"/db/test-data/insert_recipes.sql", "/db/test-data/insert_recipe_ratings.sql"})
    public void givenRecipeExist_WhenAddingNewRating5_Then4RatingsOf5Exists() {
        // Given
        Optional<Recipe> recipe = recipeJpaRepository.findById(UUID.fromString("06309a26-9ef8-43d2-82a0-f88e0be094e0")); // Oelandhvedebread
        assertThat(recipe.isPresent()).isTrue();

        Optional<Rating> rating = ratingJpaRepository.findById(UUID.fromString("26f09c94-79ec-439c-9776-d826efad187e")); // rating 5
        assertThat(rating.isPresent()).isTrue();

        RecipeRating recipeRating = new RecipeRating();
        recipeRating.setRecipe(recipe.get());
        recipeRating.setRating(rating.get());
        recipeRating.setCreatedBy(session.getUserName());
        recipeRating.setDescription("Særklasse");

        // When
        OffsetDateTime now = OffsetDateTime.now();
        RecipeRating savedRecipeRating = recipeRatingJpaRepository.saveAndFlush(recipeRating);

        // Then
        assertThat(now).isCloseTo(savedRecipeRating.getCreated(), within(0, ChronoUnit.SECONDS));
        assertThat(savedRecipeRating.getCreatedBy()).isEqualTo(session.getUserName());
        assertThat(savedRecipeRating.getDescription()).isEqualTo("Særklasse");

        List<RecipeRating> recipeRatings = recipeRatingJpaRepository.findAllByRecipeIdAndRatingId(UUID.fromString("06309a26-9ef8-43d2-82a0-f88e0be094e0"), UUID.fromString("26f09c94-79ec-439c-9776-d826efad187e"));

        //Then
        assertThat(recipeRatings.size()).isEqualTo(4);
    }

    @Test
    @DisplayName("Given Recipe Oelandshvedemel, When updating existing Rating of 5 to 4, Then 2 Ratings of 5 exists")
    @Sql({"/db/test-data/insert_recipes.sql", "/db/test-data/insert_recipe_ratings.sql"})
    public void givenRecipeExist_WhenUpdatingRating5To4_Then2RatingsOf5Exists() {
        //Given
        List<RecipeRating> recipeRatings = recipeRatingJpaRepository.findAllByRecipeIdAndRatingId(UUID.fromString("06309a26-9ef8-43d2-82a0-f88e0be094e0"), UUID.fromString("26f09c94-79ec-439c-9776-d826efad187e"));
        assertThat(recipeRatings.size()).isEqualTo(3);

        Optional<Rating> rating = ratingJpaRepository.findById(UUID.fromString("62149cc5-73e4-45ef-8ac2-4d725815d5cf")); // rating 4
        assertThat(rating.isPresent()).isTrue();

        //When
        RecipeRating recipeRating = recipeRatings.get(0); // pick random first
        recipeRating.setRating(rating.get());
        OffsetDateTime now = OffsetDateTime.now();
        RecipeRating saveAndFlush = recipeRatingJpaRepository.saveAndFlush(recipeRating);
        assertThat(saveAndFlush.getUpdatedBy()).isEqualTo(session.getUserName());
        assertThat(now).isCloseTo(saveAndFlush.getUpdated(), within(0, ChronoUnit.SECONDS));

        //Then
        recipeRatings = recipeRatingJpaRepository.findAllByRecipeIdAndRatingId(UUID.fromString("06309a26-9ef8-43d2-82a0-f88e0be094e0"), UUID.fromString("26f09c94-79ec-439c-9776-d826efad187e"));
        assertThat(recipeRatings.size()).isEqualTo(2);


    }
}
