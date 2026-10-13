package dk.serik.recipes.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.jackson.Jacksonized;

/**
 * What an admin may change about another account: whether it can sign in, and whether it is an
 * admin. A request body, not a stored row, so it does not extend BaseDTO. Both are required, so a
 * request says what it wants rather than leaving one to a default.
 */
@Getter
public class UpdateUserRequestDTO {

	@NotNull(message = "{dk.serik.models.user.enabled.notnull.message}")
	private Boolean enabled;

	@NotNull(message = "{dk.serik.models.user.admin.notnull.message}")
	private Boolean admin;

	@Builder
	@Jacksonized
	public UpdateUserRequestDTO(Boolean enabled, Boolean admin) {
		this.enabled = enabled;
		this.admin = admin;
	}

	@Override
	public String toString() {
		return "UpdateUserRequestDTO{enabled=" + enabled + ", admin=" + admin + '}';
	}
}
