package dk.serik.recipes.mapper;

import dk.serik.recipes.dto.RecipeDTO;
import dk.serik.recipes.dto.RecipeIngredientDTO;
import dk.serik.recipes.mockutil.MockRecipeUtil;
import dk.serik.recipes.mockutil.MockUnitUtil;
import dk.serik.recipes.model.Ingredient;
import dk.serik.recipes.model.Recipe;
import dk.serik.recipes.model.RecipeIngredient;
import dk.serik.recipes.testutil.OffsetDateTimeProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
public class RecipeMapperTest {

    @Test
    @DisplayName("Given Recipe with Ratings and Ingredient, When mapped to DTO, Then success")
    public void shouldMapToDTOWithIngredientsAndRatings() {
        RecipeDTO dto = RecipeMapper.from(MockRecipeUtil.mockSavedRecipeWheatBreadWithRatings());
        assertThat(dto.getName()).isEqualTo("Hvedebrød");
        assertThat(dto.getCategory().getName()).isEqualTo("Brød");
        assertThat(dto.getTags()).isNull();
        assertThat(dto.getRecipeRatings().size()).isEqualTo(2);
        assertThat(dto.getRecipeIngredients().size()).isEqualTo(3);
        assertThat(dto.getCreatedBy()).isEqualTo("Jens");
        assertThat(dto.getUpdated()).isEqualTo(OffsetDateTimeProvider.provideIsoFullFormat("2023-01-25T14:25:15"));
    }

    @Test
    @DisplayName("Given Recipe with Ingredients, When mapped to DTO, Then success")
    public void shouldMapToDTOWithIngredients() {
        RecipeDTO dto = RecipeMapper.from(MockRecipeUtil.mockSavedRecipeWheatBread());
        dto.setRecipeRatings(null);
        assertThat(dto.getName()).isEqualTo("Hvedebrød");
        assertThat(dto.getCategory().getName()).isEqualTo("Brød");
        assertThat(dto.getTags()).isNull();
        assertThat(dto.getRecipeRatings()).isNull();
        assertThat(dto.getRecipeIngredients().size()).isEqualTo(3);
    }

    @Test
    @DisplayName("Given a recipe's lines, When mapped to DTO, Then they are a list in Danish order of ingredient name")
    public void shouldListIngredientLinesInDanishOrder() {
        Recipe recipe = MockRecipeUtil.mockSavedRecipeWheatBread();   // Hvedemel, Havsalt, Surdej
        recipe.addRecipeIngredient(line("Åkande"));
        recipe.addRecipeIngredient(line("Øl"));
        recipe.addRecipeIngredient(line("Ærter"));
        recipe.addRecipeIngredient(line("Agurk"));

        RecipeDTO dto = RecipeMapper.from(recipe);

        // Plain String order would put Å first of the three: it is U+00C5, before Æ and Ø.
        assertThat(dto.getRecipeIngredients()).extracting(RecipeIngredientDTO::getIngredientName)
                .containsExactly("Agurk", "Havsalt", "Hvedemel", "Surdej", "Ærter", "Øl", "Åkande");
    }

    private static RecipeIngredient line(String ingredientName) {
        Ingredient ingredient = Ingredient.builder().name(ingredientName).build();
        ReflectionTestUtils.setField(ingredient, "id", UUID.randomUUID());
        return RecipeIngredient.builder()
                .ingredient(ingredient)
                .unit(MockUnitUtil.mockGram())
                .amount(BigDecimal.ONE)
                .build();
    }

    @Test
    @DisplayName("Given Recipe, When mapped to DTO, Then success")
    public void shouldMapToDTO() {
        RecipeDTO dto = RecipeMapper.from(MockRecipeUtil.mockSavedRecipeWheatBread());
        dto.setRecipeRatings(null);
        dto.setRecipeIngredients(null);
        assertThat(dto.getName()).isEqualTo("Hvedebrød");
        assertThat(dto.getCategory().getName()).isEqualTo("Brød");
        assertThat(dto.getTags()).isNull();
        assertThat(dto.getRecipeRatings()).isNull();
        assertThat(dto.getRecipeIngredients()).isNull();
    }


    @Test
    @DisplayName("Given Recipe Entity is Null, When Mapping to DTO, Then DTO is Null")
    public void shouldReturnDTONull() {
        assertThat(RecipeMapper.from(null)).isNull();
    }
}
