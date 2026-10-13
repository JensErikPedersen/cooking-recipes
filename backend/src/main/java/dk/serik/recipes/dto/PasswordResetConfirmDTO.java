package dk.serik.recipes.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.jackson.Jacksonized;

/**
 * A new password, with the token from the reset mail. As in {@link RegisterRequestDTO}, the rules a
 * value can be checked against alone are here; the token, the upper limit in bytes and the
 * confirmation are checked in {@code AuthServiceImpl.confirmPasswordReset}.
 */
@Getter
public class PasswordResetConfirmDTO {

	@NotBlank(message = "{dk.serik.models.reset.token.notblank.message}")
	private String token;

	@NotNull(message = "{dk.serik.models.password.new.notnull.message}")
	@Size(min = 8, message = "{dk.serik.models.password.new.size.message}")
	private String newPassword;

	private String confirmPassword;

	@Builder
	@Jacksonized
	public PasswordResetConfirmDTO(String token, String newPassword, String confirmPassword) {
		this.token = token;
		this.newPassword = newPassword;
		this.confirmPassword = confirmPassword;
	}

	@Override
	public String toString() {
		// neither the token nor a password may reach a log
		return "PasswordResetConfirmDTO{}";
	}
}
