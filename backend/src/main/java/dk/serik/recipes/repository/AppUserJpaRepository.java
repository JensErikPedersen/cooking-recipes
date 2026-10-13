package dk.serik.recipes.repository;

import dk.serik.recipes.model.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface AppUserJpaRepository extends JpaRepository<AppUser, UUID> {

	Optional<AppUser> findByUsername(String username);

	/** Regardless of case, whatever the database's collation: H2 in the tests tells case apart. */
	boolean existsByUsernameIgnoreCase(String username);

	/** Regardless of case, as {@link #existsByUsernameIgnoreCase}. */
	boolean existsByEmailIgnoreCase(String email);
}
