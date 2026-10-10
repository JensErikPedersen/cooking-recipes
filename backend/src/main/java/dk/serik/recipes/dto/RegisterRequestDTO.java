package dk.serik.recipes.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.jackson.Jacksonized;

/**
 * A new account. A request body, not a stored row, so it does not extend BaseDTO.
 * <p>
 * The rules a value can be checked against alone are here. The password's upper limit is in bytes,
 * which Bean Validation cannot count, and the confirmation is compared with the password: both are
 * checked in {@code AuthServiceImpl.register}.
 */
@Getter
public class RegisterRequestDTO {

	@NotNull(message = "{dk.serik.models.register.username.notnull.message}")
	@Size(min = 3, max = 64, message = "{dk.serik.models.register.username.size.message}")
	@Pattern(regexp = "[\\p{L}0-9._-]*", message = "{dk.serik.models.register.username.pattern.message}")
	private String username;

	@NotNull(message = "{dk.serik.models.register.password.notnull.message}")
	@Size(min = 8, message = "{dk.serik.models.register.password.size.message}")
	private String password;

	private String confirmPassword;

	@Builder
	@Jacksonized
	public RegisterRequestDTO(String username, String password, String confirmPassword) {
		this.username = username;
		this.password = password;
		this.confirmPassword = confirmPassword;
	}

	@Override
	public String toString() {
		// neither password may reach a log
		return "RegisterRequestDTO{username='" + username + "'}";
	}
}
