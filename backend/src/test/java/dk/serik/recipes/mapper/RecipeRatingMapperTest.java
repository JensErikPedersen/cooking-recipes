package dk.serik.recipes.mapper;

import dk.serik.recipes.dto.RecipeRatingDTO;
import dk.serik.recipes.mockutil.MockRecipeRatingUtil;
import dk.serik.recipes.mockutil.MockRecipeUtil;
import dk.serik.recipes.model.Recipe;
import dk.serik.recipes.model.RecipeRating;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pure mapping behaviour - no Spring context needed. The database-backed case that maps a
 * persisted graph lives in {@code RecipeRatingMapperIT}.
 */
public class RecipeRatingMapperTest {

    @Test
    @DisplayName("Given Entity is null, When mapped to DTO, Then DTO is Null")
    public void passMapperFromNullEntityToNullDto() {
        assertThat(RecipeRatingMapper.from(null)).isNull();
    }

    @Test
    @DisplayName("Given a RecipeRating attached to a Recipe, When mapped, Then rating and both ids are carried over")
    public void shouldMapRatingAndIds() {
        // Given - the mapper reads through both back-references, so the rating must be attached
        Recipe recipe = MockRecipeUtil.mockSavedRecipeWheatBread();
        RecipeRating recipeRating = MockRecipeRatingUtil.mockRecipeRating5();
        recipe.addRecipeRating(recipeRating);

        // When
        RecipeRatingDTO dto = RecipeRatingMapper.from(recipeRating);

        // Then
        assertThat(dto.getRating()).isEqualTo(5);
        assertThat(dto.getDescription()).isEqualTo("Excellent!");
        assertThat(dto.getRecipeId()).isEqualTo(recipe.getId().toString());
    }
}
