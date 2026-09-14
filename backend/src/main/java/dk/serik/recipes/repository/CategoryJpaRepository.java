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
	
	List<Category> findAllByNameContains(String name);

}
