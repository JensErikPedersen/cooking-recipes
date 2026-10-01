package dk.serik.recipes.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * An account that can sign in. Read-only in this application: the only row is created by
 * {@code AdminBootstrap} with plain SQL, and there is no user administration.
 * <p>
 * Named {@code AppUser} because {@code user} is a reserved word in several databases, and
 * {@code User} clashes with Spring Security's own class.
 */
@Entity
@Table(name = "app_user")
@Getter
@NoArgsConstructor
public class AppUser extends BaseIdentifierEntity {

	@Column(nullable = false, unique = true, length = 64)
	private String username;

	/** A hash carrying its algorithm id, e.g. {@code {bcrypt}...}. Never a plaintext password. */
	@Column(nullable = false)
	private String password;

	@Column(nullable = false)
	private boolean enabled;

	/** Comma-separated, e.g. {@code ROLE_USER,ROLE_ADMIN}. */
	@Column(nullable = false)
	private String roles;

	public List<String> roleList() {
		return List.of(roles.split(","));
	}

	@Override
	public String toString() {
		// no password, not even its length
		return "AppUser{id=" + id + ", username='" + username + "', enabled=" + enabled + ", roles='" + roles + "'}";
	}
}
