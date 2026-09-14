package dk.serik.recipes.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.jackson.Jacksonized;

import java.time.OffsetDateTime;

@JsonInclude(Include.NON_NULL)
@Getter
public class CategoryDTO extends BaseIdentityDTO {

	@NotBlank(message = "{dk.serik.models.category.name.notblank.message}")
	@Size(max = 64, message = "{dk.serik.models.category.name.size.message}")
	private String name;

	@Size(max = 255, message = "{dk.serik.models.category.description.size.message}")
	private String description;

	@Builder
	@Jacksonized
	public CategoryDTO(String id, OffsetDateTime created, String createdBy, OffsetDateTime updated, String updatedBy, String name, String description) {
		super(id, created, createdBy, updated, updatedBy);
		this.name = name;
		this.description = description;
	}


	@Override
	public String toString() {
		return "CategoryDTO{" +
				"id='" + getId() + '\'' +
				", created=" + getCreated() +
				", updated=" + getUpdated() +
				", updatedBy='" + getUpdatedBy() + '\'' +
				", createdBy='" + getCreatedBy() + '\'' +
				", name='" + name + '\'' +
				", description='" + description + '\'' +
				'}';
	}



}
