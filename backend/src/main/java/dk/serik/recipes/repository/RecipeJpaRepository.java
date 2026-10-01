package dk.serik.recipes.repository;

import dk.serik.recipes.model.Recipe;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface RecipeJpaRepository extends JpaRepository<Recipe, UUID> {
	
	List<Recipe> findAllByCategoryName(String categoryName);
	
	List<Recipe> findAllByNameContains(String name);

	long countByCategoryId(UUID categoryId);

}
