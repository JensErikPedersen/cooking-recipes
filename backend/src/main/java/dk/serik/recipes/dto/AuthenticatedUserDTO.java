package dk.serik.recipes.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.extern.jackson.Jacksonized;

import java.util.List;

/**
 * Who the caller is: returned by login, registration and the current-user endpoint. No id, no
 * password. The email address is {@code null} on an account that has not added one.
 */
@Getter
public class AuthenticatedUserDTO {

	private String username;

	private String email;

	private List<String> roles;

	@Builder
	@Jacksonized
	public AuthenticatedUserDTO(String username, String email, List<String> roles) {
		this.username = username;
		this.email = email;
		this.roles = roles;
	}

	@Override
	public String toString() {
		return "AuthenticatedUserDTO{username='" + username + "', email='" + email + "', roles=" + roles + '}';
	}
}
