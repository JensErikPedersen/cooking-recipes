package dk.serik.recipes.repository;

import dk.serik.recipes.model.Picture;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface PictureJpaRepository extends JpaRepository<Picture, UUID> {
}
