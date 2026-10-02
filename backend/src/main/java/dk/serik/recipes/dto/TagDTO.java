package dk.serik.recipes.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.jackson.Jacksonized;

import java.time.OffsetDateTime;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Getter
public class TagDTO extends BaseIdentityDTO {

    // tag.name is VARCHAR(255) and unique, like the other lookups' names
    @NotBlank(message = "{dk.serik.models.tag.name.notblank.message}")
    @Size(max = 255, message = "{dk.serik.models.tag.name.size.message}")
    private String name;

    @Builder
    @Jacksonized
    public TagDTO(String id, OffsetDateTime created, String createdBy, OffsetDateTime updated, String updatedBy, String name) {
        super(id, created, createdBy, updated, updatedBy);
        this.name = name;
    }

    @Override
    public String toString() {
        return "TagDTO{" +
                "name='" + name + '\'' +
                ", id='" + getId() + '\'' +
                ", created=" + getCreated() +
                ", createdBy='" + getCreatedBy() + '\'' +
                ", updated=" + getUpdated() +
                ", updatedBy='" + getUpdatedBy() + '\'' +
                '}';
    }


}
