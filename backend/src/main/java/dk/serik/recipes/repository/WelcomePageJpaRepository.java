package dk.serik.recipes.repository;

import dk.serik.recipes.model.WelcomePage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface WelcomePageJpaRepository extends JpaRepository<WelcomePage, UUID> {

	boolean existsByPictureId(UUID pictureId);
}
