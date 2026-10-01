package dk.serik.recipes.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Creates the first account from {@code APP_ADMIN_USERNAME} / {@code APP_ADMIN_PASSWORD}, and only
 * while {@code app_user} is empty - so it never overwrites an existing account, and there is no
 * default credential anywhere in the repository. Without both values it creates nothing.
 * <p>
 * Plain SQL, not the repository: persisting {@code AppUser} through JPA fires
 * {@code BaseEntityListener}, which reads the request-scoped {@code Session} bean, and startup is
 * not a request.
 */
@Component
@Slf4j
public class AdminBootstrap implements ApplicationRunner {

	private final JdbcTemplate jdbcTemplate;

	private final PasswordEncoder passwordEncoder;

	private final String username;

	private final String password;

	public AdminBootstrap(JdbcTemplate jdbcTemplate, PasswordEncoder passwordEncoder,
						  @Value("${app.admin.username:}") String username,
						  @Value("${app.admin.password:}") String password) {
		this.jdbcTemplate = jdbcTemplate;
		this.passwordEncoder = passwordEncoder;
		this.username = username;
		this.password = password;
	}

	@Override
	public void run(ApplicationArguments args) {
		if (username.isBlank() || password.isBlank()) {
			log.info("app.admin.username/password not set - no account bootstrapped");
			return;
		}
		Integer accounts = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM app_user", Integer.class);
		if (accounts > 0) {
			log.info("app_user already has {} account(s) - no account bootstrapped", accounts);
			return;
		}
		jdbcTemplate.update("INSERT INTO app_user (id, username, password, enabled, roles, created_by) VALUES (?, ?, ?, ?, ?, ?)",
				UUID.randomUUID().toString(), username, passwordEncoder.encode(password), true, "ROLE_USER,ROLE_ADMIN", "bootstrap");
		log.info("Bootstrapped account '{}'", username); // never the password, nor its hash
	}
}
