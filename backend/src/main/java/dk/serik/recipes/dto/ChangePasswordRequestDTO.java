package dk.serik.recipes.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.jackson.Jacksonized;

/**
 * A signed-in user's new password. A request body, not a stored row, so it does not extend BaseDTO.
 * <p>
 * As in {@link RegisterRequestDTO}, the rules a value can be checked against alone are here; the
 * current password, the upper limit in bytes and the confirmation are checked in
 * {@code AuthServiceImpl.changePassword}.
 */
@Getter
public class ChangePasswordRequestDTO {

	@NotNull(message = "{dk.serik.models.password.current.notnull.message}")
	private String currentPassword;

	@NotNull(message = "{dk.serik.models.password.new.notnull.message}")
	@Size(min = 8, message = "{dk.serik.models.password.new.size.message}")
	private String newPassword;

	private String confirmPassword;

	@Builder
	@Jacksonized
	public ChangePasswordRequestDTO(String currentPassword, String newPassword, String confirmPassword) {
		this.currentPassword = currentPassword;
		this.newPassword = newPassword;
		this.confirmPassword = confirmPassword;
	}

	@Override
	public String toString() {
		// no password may reach a log
		return "ChangePasswordRequestDTO{}";
	}
}
