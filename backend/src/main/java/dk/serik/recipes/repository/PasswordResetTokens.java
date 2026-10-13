package dk.serik.recipes.repository;

import lombok.AllArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;

/**
 * The tokens of password resets, one per account at most. A token is stored as its SHA-256 hash
 * only - hashed here and nowhere else - so the table never holds one that works.
 * <p>
 * Plain SQL: the table has no entity, and no audit columns for {@code BaseEntityListener} to fill.
 */
@Repository
@AllArgsConstructor
public class PasswordResetTokens {

	/** How long a token works. */
	public static final Duration VALID_FOR = Duration.ofMinutes(30);

	private static final SecureRandom RANDOM = new SecureRandom();

	private final JdbcTemplate jdbcTemplate;

	/** A new token for the account, replacing any earlier one. Returned once, for the mail. */
	public String create(String appUserId) {
		byte[] bytes = new byte[32];
		RANDOM.nextBytes(bytes);
		String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
		// Requests for one account take turns, from this lock on its row to the commit. Without it, two
		// at once both found no token to delete and both inserted one, and the second answered a 409 -
		// telling the address has an account. Portable SQL: the tests' H2 is not in MySQL mode.
		jdbcTemplate.queryForObject("SELECT id FROM app_user WHERE id = ? FOR UPDATE", String.class, appUserId);
		jdbcTemplate.update("DELETE FROM password_reset_token WHERE app_user_id = ?", appUserId);
		jdbcTemplate.update("INSERT INTO password_reset_token (app_user_id, token_hash, expires) VALUES (?, ?, ?)",
				appUserId, hash(token), Timestamp.from(Instant.now().plus(VALID_FOR)));
		return token;
	}

	/** The account the token resets, while it has not expired. */
	public Optional<String> appUserIdOf(String token) {
		return jdbcTemplate.queryForList("SELECT app_user_id FROM password_reset_token WHERE token_hash = ? AND expires > ?",
				String.class, hash(token), Timestamp.from(Instant.now())).stream().findFirst();
	}

	/**
	 * Spends the token. False if it was already gone: of two requests with one token, only the one
	 * whose delete counts may go on.
	 */
	public boolean spend(String token) {
		return jdbcTemplate.update("DELETE FROM password_reset_token WHERE token_hash = ?", hash(token)) == 1;
	}

	private static String hash(String token) {
		try {
			return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8)));
		} catch (NoSuchAlgorithmException ex) {
			throw new IllegalStateException("Every Java platform has SHA-256", ex);
		}
	}
}
