package dk.serik.recipes.config;

import dk.serik.recipes.model.AppUser;
import dk.serik.recipes.repository.AppUserWriter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Creates the first account from {@code APP_ADMIN_USERNAME} / {@code APP_ADMIN_PASSWORD}, and only
 * while {@code app_user} is empty - so it never overwrites an existing account, and there is no
 * default credential anywhere in the repository. Without both values it creates nothing.
 * <p>
 * The insert is {@link AppUserWriter}'s, which registration uses too.
 */
@Component
@Slf4j
public class AdminBootstrap implements ApplicationRunner {

	private final JdbcTemplate jdbcTemplate;

	private final AppUserWriter appUserWriter;

	private final String username;

	private final String password;

	public AdminBootstrap(JdbcTemplate jdbcTemplate, AppUserWriter appUserWriter,
						  @Value("${app.admin.username:}") String username,
						  @Value("${app.admin.password:}") String password) {
		this.jdbcTemplate = jdbcTemplate;
		this.appUserWriter = appUserWriter;
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
		// No address: the admin adds one on the account page, like anyone else (docs/PLAN_V6.md decision 1).
		appUserWriter.insert(username, null, password, AppUser.ROLE_USER + "," + AppUser.ROLE_ADMIN, "bootstrap");
		log.info("Bootstrapped account '{}'", username); // never the password, nor its hash
	}
}
