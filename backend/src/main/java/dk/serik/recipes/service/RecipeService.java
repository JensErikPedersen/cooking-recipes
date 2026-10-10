package dk.serik.recipes.service;

import dk.serik.recipes.dto.RecipeDTO;
import dk.serik.recipes.dto.RecipeRatingDTO;

import java.util.List;
import java.util.Optional;

public interface RecipeService {
    List<RecipeDTO> findAllByCategoryName(String categoryName);

    List<RecipeDTO> findAllByNameContains(String name);

    List<RecipeDTO> findAll();

    Optional<RecipeDTO> findById(String id);

    RecipeDTO save(RecipeDTO recipeDTO);

    boolean delete(String id);

    RecipeDTO update(RecipeDTO dto);

    RecipeDTO addRecipeRating(RecipeRatingDTO dto);

}
