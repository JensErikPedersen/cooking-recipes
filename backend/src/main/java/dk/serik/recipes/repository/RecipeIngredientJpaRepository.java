package dk.serik.recipes.repository;

import dk.serik.recipes.model.RecipeIngredient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RecipeIngredientJpaRepository extends JpaRepository<RecipeIngredient, UUID> {

	Optional<RecipeIngredient> findByRecipeIdAndIngredientId(UUID recipeId, UUID ingredientId);
	
	List<RecipeIngredient> findAllByRecipeId(UUID recipeId);
	
	List<RecipeIngredient> findAllByIngredientId(UUID ingredientId);

	// The key is (recipe, ingredient), so each row is a different recipe.
	long countByIngredientId(UUID ingredientId);

	// Recipes, not lines: one recipe can use a unit on several ingredient lines. No derived query
	// counts distinct recipes, hence the JPQL.
	@Query("select count(distinct ri.recipe.id) from RecipeIngredient ri where ri.unit.id = :unitId")
	long countRecipesByUnitId(@Param("unitId") UUID unitId);
}
