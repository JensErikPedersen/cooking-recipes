package dk.serik.recipes.repository;

import lombok.AllArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Repository;

import java.util.UUID;

/**
 * Creates accounts, for {@code AdminBootstrap} and for registration alike.
 * <p>
 * Plain SQL, not {@link AppUserJpaRepository}: persisting {@code AppUser} through JPA fires
 * {@code BaseEntityListener}, which writes {@code created_by} from the request-scoped
 * {@code Session} - and neither caller has a signed-in user. Startup is not a request at all, and a
 * registration is an anonymous one, which would be recorded as {@code anonymous}.
 */
@Repository
@AllArgsConstructor
public class AppUserWriter {

	private final JdbcTemplate jdbcTemplate;

	private final PasswordEncoder passwordEncoder;

	/** Stores the password as a hash only; the plaintext goes no further than this method. */
	public void insert(String username, String password, String roles, String createdBy) {
		jdbcTemplate.update("INSERT INTO app_user (id, username, password, enabled, roles, created_by) VALUES (?, ?, ?, ?, ?, ?)",
				UUID.randomUUID().toString(), username, passwordEncoder.encode(password), true, roles, createdBy);
	}
}
