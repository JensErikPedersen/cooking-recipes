package dk.serik.recipes.mapper;

import dk.serik.recipes.dto.RecipeDTO;
import dk.serik.recipes.mockutil.MockRecipeUtil;
import dk.serik.recipes.testutil.OffsetDateTimeProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

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
