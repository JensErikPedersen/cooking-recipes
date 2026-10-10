package dk.serik.recipes.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(Include.NON_NULL)
public class BaseDTO {

	// private, not public: @Data already generates the accessors, and public fields here made every
	// subclass writable regardless of what that subclass declared - a leaf DTO carrying only
	// @Getter was still fully mutable through these.
	// No @JsonFormat: Jackson writes ISO-8601 with the offset, which a client can place in its own
	// time zone and the API can read back. A pattern without an offset did neither.
	private OffsetDateTime created;

	private String createdBy;

	private OffsetDateTime updated;

	private String updatedBy;
}
