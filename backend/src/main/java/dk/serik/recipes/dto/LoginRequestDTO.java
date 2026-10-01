package dk.serik.recipes.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.jackson.Jacksonized;

/** Login credentials. A request body, not a stored row, so it does not extend BaseDTO. */
@Getter
public class LoginRequestDTO {

	@NotBlank(message = "{dk.serik.models.login.username.notblank.message}")
	@Size(max = 64, message = "{dk.serik.models.login.username.size.message}")
	private String username;

	@NotBlank(message = "{dk.serik.models.login.password.notblank.message}")
	@Size(max = 255, message = "{dk.serik.models.login.password.size.message}")
	private String password;

	@Builder
	@Jacksonized
	public LoginRequestDTO(String username, String password) {
		this.username = username;
		this.password = password;
	}

	@Override
	public String toString() {
		// the password must never reach a log
		return "LoginRequestDTO{username='" + username + "'}";
	}
}
