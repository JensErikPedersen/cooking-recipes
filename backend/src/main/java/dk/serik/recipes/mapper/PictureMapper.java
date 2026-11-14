package dk.serik.recipes.mapper;

import dk.serik.recipes.dto.IngredientDTO;
import dk.serik.recipes.dto.PictureDTO;
import dk.serik.recipes.dto.RecipeDTO;
import dk.serik.recipes.model.Ingredient;
import dk.serik.recipes.model.Picture;
import dk.serik.recipes.model.Recipe;

import java.util.List;
import java.util.Objects;

public class PictureMapper {
    public static PictureDTO from(Picture entity) {
        if (Objects.isNull(entity)) {
            return null;
        }
        return builder(entity).build();
    }

    /** A picture with where it is used: each recipe and ingredient by id and name. */
    public static PictureDTO from(Picture entity, List<Recipe> recipes, List<Ingredient> ingredients, boolean welcomePage) {
        return builder(entity)
                .recipes(recipes.stream()
                        .map(recipe -> RecipeDTO.builder().id(recipe.getId().toString()).name(recipe.getName()).build())
                        .toList())
                .ingredients(ingredients.stream()
                        .map(ingredient -> IngredientDTO.builder().id(ingredient.getId().toString()).name(ingredient.getName()).build())
                        .toList())
                .welcomePage(welcomePage)
                .build();
    }

    private static PictureDTO.PictureDTOBuilder builder(Picture entity) {
        return PictureDTO.builder()
                .id(entity.getId().toString())
                .name(entity.getName())
                .altText(entity.getAltText())
                .createdBy(entity.getCreatedBy())
                .created(entity.getCreated())
                .updatedBy(entity.getUpdatedBy())
                .updated(entity.getUpdated());
    }
}
