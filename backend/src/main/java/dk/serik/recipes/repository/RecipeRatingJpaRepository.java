package dk.serik.recipes.repository;

import dk.serik.recipes.model.RecipeRating;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RecipeRatingJpaRepository extends JpaRepository<RecipeRating, UUID> {
    List<RecipeRating> findAllByRecipeIdAndRatingId(UUID recipeId, UUID ratingId);

    List<RecipeRating> findAllByRecipeId(UUID recipeId);

    List<RecipeRating> findAllByRatingId(UUID ratingId);

}
