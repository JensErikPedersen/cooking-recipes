package dk.serik.recipes.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.extern.jackson.Jacksonized;

import java.util.List;

/** Who the caller is: returned by login and by the current-user endpoint. No id, no password. */
@Getter
public class AuthenticatedUserDTO {

	private String username;

	private List<String> roles;

	@Builder
	@Jacksonized
	public AuthenticatedUserDTO(String username, List<String> roles) {
		this.username = username;
		this.roles = roles;
	}

	@Override
	public String toString() {
		return "AuthenticatedUserDTO{username='" + username + "', roles=" + roles + '}';
	}
}
