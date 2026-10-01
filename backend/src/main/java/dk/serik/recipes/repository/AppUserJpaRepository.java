package dk.serik.recipes.repository;

import dk.serik.recipes.model.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface AppUserJpaRepository extends JpaRepository<AppUser, UUID> {

	Optional<AppUser> findByUsername(String username);
}
