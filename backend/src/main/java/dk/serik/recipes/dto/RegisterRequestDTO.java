package dk.serik.recipes.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
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

	// @NotBlank, not @NotNull: @Email takes an empty string as valid.
	@NotBlank(message = "{dk.serik.models.email.notblank.message}")
	@Email(message = "{dk.serik.models.email.email.message}")
	@Size(max = 254, message = "{dk.serik.models.email.size.message}")
	private String email;

	@NotNull(message = "{dk.serik.models.register.password.notnull.message}")
	@Size(min = 8, message = "{dk.serik.models.register.password.size.message}")
	private String password;

	private String confirmPassword;

	@Builder
	@Jacksonized
	public RegisterRequestDTO(String username, String email, String password, String confirmPassword) {
		this.username = username;
		this.email = email;
		this.password = password;
		this.confirmPassword = confirmPassword;
	}

	@Override
	public String toString() {
		// neither password may reach a log
		return "RegisterRequestDTO{username='" + username + "'}";
	}
}
