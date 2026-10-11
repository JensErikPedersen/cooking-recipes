package dk.serik.recipes.repository;

import dk.serik.recipes.model.Recipe;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RecipeJpaRepository extends JpaRepository<Recipe, UUID> {

	/** Partial match, ignoring case: MySQL's collation does, H2 in the tests does not unless asked. */
	List<Recipe> findAllByNameContainingIgnoreCase(String name);

	Optional<Recipe> findByName(String name);

	long countByCategoryId(UUID categoryId);

	// recipe_tag's key is (recipe_id, tag_id), so no recipe is counted twice.
	long countByTagsId(UUID tagId);

}
