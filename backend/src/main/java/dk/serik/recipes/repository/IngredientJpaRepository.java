package dk.serik.recipes.repository;

import dk.serik.recipes.model.Ingredient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface IngredientJpaRepository extends JpaRepository<Ingredient, UUID> {
	
	List<Ingredient> findAllByNameContains(String name);
	
	List<Ingredient> findAllByDescriptionContains(String name);
	
	Optional<Ingredient> findByName(String name);

}
