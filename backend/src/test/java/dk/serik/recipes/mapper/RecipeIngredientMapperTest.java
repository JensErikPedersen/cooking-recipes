package dk.serik.recipes.mapper;

import dk.serik.recipes.dto.RecipeIngredientDTO;
import dk.serik.recipes.mockutil.MockRecipeUtil;
import dk.serik.recipes.model.Recipe;
import dk.serik.recipes.model.RecipeIngredient;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pure mapping behaviour - no Spring context needed. The database-backed case lives in
 * {@code RecipientIngredientMapperIT}, which also covers the BigDecimal scale that only a
 * real DECIMAL round-trip produces.
 */
public class RecipeIngredientMapperTest {

    @Test
    @DisplayName("Given RecipeIngredient Entity is Null, When Mapping to DTO, Then DTO is Null")
    public void shouldReturnDTONull() {
        assertThat(RecipeIngredientMapper.from(null)).isNull();
    }

    @Test
    @DisplayName("Given a RecipeIngredient on a Recipe, When mapped, Then ingredient, unit and recipe ids are carried over")
    public void shouldMapIngredientUnitAndRecipe() {
        // Given - taken from the Recipe so the back-reference the mapper reads is set
        Recipe recipe = MockRecipeUtil.mockSavedRecipeWheatBread();
        RecipeIngredient recipeIngredient = recipe.getRecipeIngredients().iterator().next();

        // When
        RecipeIngredientDTO dto = RecipeIngredientMapper.from(recipeIngredient);

        // Then
        assertThat(dto.getRecipeId()).isEqualTo(recipe.getId().toString());
        assertThat(dto.getIngredientId()).isEqualTo(recipeIngredient.getIngredient().getId().toString());
        assertThat(dto.getIngredientName()).isEqualTo(recipeIngredient.getIngredient().getName());
        assertThat(dto.getUnitLabel()).isEqualTo(recipeIngredient.getUnit().getLabel());
        assertThat(dto.getAmount()).isEqualByComparingTo(recipeIngredient.getAmount());
    }
}
