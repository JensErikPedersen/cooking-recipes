package dk.serik.recipes.repository;

import dk.serik.recipes.model.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CategoryJpaRepository extends JpaRepository<Category, UUID>{
	Optional<Category> findByName(String name);

	/** Partial match, ignoring case: MySQL's collation does, H2 in the tests does not unless asked. */
	List<Category> findAllByNameContainingIgnoreCase(String name);

}
