package dk.serik.recipes.repository;

import dk.serik.recipes.model.RecipeIngredient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RecipeIngredientJpaRepository extends JpaRepository<RecipeIngredient, UUID> {

	Optional<RecipeIngredient> findByRecipeIdAndIngredientId(UUID recipeId, UUID ingredientId);
	
	List<RecipeIngredient> findAllByRecipeId(UUID recipeId);
	
	List<RecipeIngredient> findAllByIngredientId(UUID ingredientId);
}
