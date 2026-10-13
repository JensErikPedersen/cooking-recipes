package dk.serik.recipes.repository;

import lombok.AllArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Repository;

import java.util.UUID;

/**
 * Creates accounts, for {@code AdminBootstrap} and for registration alike, changes passwords - every
 * password is encoded here and nowhere else - and email addresses, and enables, disables and sets
 * the roles of accounts.
 * <p>
 * Plain SQL, not {@link AppUserJpaRepository}: persisting {@code AppUser} through JPA fires
 * {@code BaseEntityListener}, which writes {@code created_by} from the request-scoped
 * {@code Session} - and neither caller has a signed-in user. Startup is not a request at all, and a
 * registration is an anonymous one, which would be recorded as {@code anonymous}. A change of
 * password has a signed-in user, but comes here as well, to keep the encoding in this one class.
 */
@Repository
@AllArgsConstructor
public class AppUserWriter {

	private final JdbcTemplate jdbcTemplate;

	private final PasswordEncoder passwordEncoder;

	/**
	 * Stores the password as a hash only; the plaintext goes no further than this method. The email
	 * may be {@code null}: the bootstrapped admin has none.
	 */
	public void insert(String username, String email, String password, String roles, String createdBy) {
		jdbcTemplate.update("INSERT INTO app_user (id, username, email, password, enabled, roles, created_by) VALUES (?, ?, ?, ?, ?, ?, ?)",
				UUID.randomUUID().toString(), username, email, passwordEncoder.encode(password), true, roles, createdBy);
	}

	/** Enables or disables the account and sets its roles, comma-separated. */
	public void updateAccess(UUID id, boolean enabled, String roles, String updatedBy) {
		jdbcTemplate.update("UPDATE app_user SET enabled = ?, roles = ?, updated = CURRENT_TIMESTAMP, updated_by = ? WHERE id = ?",
				enabled, roles, updatedBy, id.toString());
	}

	/** Replaces the account's email address. */
	public void updateEmail(String username, String email, String updatedBy) {
		jdbcTemplate.update("UPDATE app_user SET email = ?, updated = CURRENT_TIMESTAMP, updated_by = ? WHERE username = ?",
				email, updatedBy, username);
	}

	/** Replaces the account's password with a hash of the new one. */
	public void updatePassword(String username, String password, String updatedBy) {
		jdbcTemplate.update("UPDATE app_user SET password = ?, updated = CURRENT_TIMESTAMP, updated_by = ? WHERE username = ?",
				passwordEncoder.encode(password), updatedBy, username);
	}
}
