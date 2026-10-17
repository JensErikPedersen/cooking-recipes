package dk.serik.recipes.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.jackson.Jacksonized;

/** A request to reset a forgotten password: the account's email address. */
@Getter
public class PasswordResetRequestDTO {

	@NotBlank(message = "{dk.serik.models.email.notblank.message}")
	@Email(message = "{dk.serik.models.email.email.message}")
	@Size(max = 254, message = "{dk.serik.models.email.size.message}")
	private String email;

	@Builder
	@Jacksonized
	public PasswordResetRequestDTO(String email) {
		this.email = email;
	}
}
