package dk.serik.recipes.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.jackson.Jacksonized;

/**
 * A signed-in user's new email address. A request body, not a stored row, so it does not extend BaseDTO.
 * <p>
 * The address takes registration's rules, {@link RegisterRequestDTO}'s; the current password is
 * checked in {@code AuthServiceImpl.changeEmail}: the address is the way into the account.
 */
@Getter
public class ChangeEmailRequestDTO {

	@NotNull(message = "{dk.serik.models.password.current.notnull.message}")
	private String currentPassword;

	@NotBlank(message = "{dk.serik.models.email.notblank.message}")
	@Email(message = "{dk.serik.models.email.email.message}")
	@Size(max = 254, message = "{dk.serik.models.email.size.message}")
	private String email;

	@Builder
	@Jacksonized
	public ChangeEmailRequestDTO(String currentPassword, String email) {
		this.currentPassword = currentPassword;
		this.email = email;
	}

	@Override
	public String toString() {
		// the password may not reach a log
		return "ChangeEmailRequestDTO{email='" + email + "'}";
	}
}
