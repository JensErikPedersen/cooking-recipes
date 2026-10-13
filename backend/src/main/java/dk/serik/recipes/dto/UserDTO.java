package dk.serik.recipes.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.jackson.Jacksonized;

import java.time.OffsetDateTime;
import java.util.List;

/** An account, as an admin sees it on the users page. Never its password, not even a hash. */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Getter
public class UserDTO extends BaseIdentityDTO {

	private String username;

	private boolean enabled;

	private List<String> roles;

	@Builder
	@Jacksonized
	public UserDTO(String id, OffsetDateTime created, String createdBy, OffsetDateTime updated, String updatedBy,
				   String username, boolean enabled, List<String> roles) {
		super(id, created, createdBy, updated, updatedBy);
		this.username = username;
		this.enabled = enabled;
		this.roles = roles;
	}

	@Override
	public String toString() {
		return "UserDTO{id='" + getId() + "', username='" + username + "', enabled=" + enabled + ", roles=" + roles + '}';
	}
}
