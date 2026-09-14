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
public class UnitDTO extends BaseIdentityDTO {

    // sizes mirror the unit table: label VARCHAR(25), name VARCHAR(64)
    @NotBlank(message = "{dk.serik.models.unit.label.notblank.message}")
    @Size(max = 25, message = "{dk.serik.models.unit.label.size.message}")
    private String label;

    @NotBlank(message = "{dk.serik.models.unit.name.notblank.message}")
    @Size(max = 64, message = "{dk.serik.models.unit.name.size.message}")
    private String name;

    @Builder
    @Jacksonized
    public UnitDTO(String id, OffsetDateTime created, String createdBy, OffsetDateTime updated, String updatedBy, String label, String name) {
        super(id, created, createdBy, updated, updatedBy);
        this.label = label;
        this.name = name;
    }

    @Override
    public String toString() {
        return "UnitDTO{" +
                "label='" + label + '\'' +
                ", name='" + name + '\'' +
                ", id='" + getId() + '\'' +
                ", created=" + getCreated() +
                ", createdBy='" + getCreatedBy() + '\'' +
                ", updated=" + getUpdated() +
                ", updatedBy='" + getUpdatedBy() + '\'' +
                '}';
    }


}
