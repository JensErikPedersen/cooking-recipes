package dk.serik.recipes.repository;

import dk.serik.recipes.model.Tag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TagJpaRepository extends JpaRepository<Tag, UUID> {
    Optional<Tag> findByName(String name);

    /** Partial match, ignoring case: MySQL's collation does, H2 in the tests does not unless asked. */
    List<Tag> findAllByNameContainingIgnoreCase(String name);
}
