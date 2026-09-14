package dk.serik.recipes.mapper;

import dk.serik.recipes.dto.RecipeRatingDTO;
import dk.serik.recipes.model.RecipeRating;

import java.util.Objects;


public class RecipeRatingMapper {
    public static RecipeRatingDTO from(RecipeRating entity) {
        if(Objects.isNull(entity)) {
            return null;
        }
        RecipeRatingDTO dto = RecipeRatingDTO.builder()
                .id(entity.getId().toString())
                .rating(entity.getRating().getRating())
                .ratingId(entity.getRating().getId().toString())
                .description(entity.getDescription())
                .recipeId(entity.getRecipe().getId().toString())
                .createdBy(entity.getCreatedBy())
                .created(entity.getCreated())
                .updated(entity.getUpdated())
                .updatedBy(entity.getUpdatedBy())
                .build();
        return dto;
    }
}

