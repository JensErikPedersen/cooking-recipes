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
public class IngredientDTO extends BaseIdentityDTO {

    @NotBlank(message = "{dk.serik.models.ingredient.name.notblank.message}")
    @Size(max = 64, message = "{dk.serik.models.ingredient.name.size.message}")
    private String name;

    @Size(max = 255, message = "{dk.serik.models.ingredient.description.size.message}")
    private String description;

    @Builder
    @Jacksonized
    public IngredientDTO(String id, OffsetDateTime created, String createdBy, OffsetDateTime updated, String updatedBy, String name, String description) {
        super(id, created, createdBy, updated, updatedBy);
        this.name = name;
        this.description = description;
    }

    @Override
    public String toString() {
        return "IngredientDTO{" +
                "name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", id='" + getId() + '\'' +
                ", created=" + getCreated() +
                ", createdBy='" + getCreatedBy() + '\'' +
                ", updated=" + getUpdated() +
                ", updatedBy='" + getUpdatedBy() + '\'' +
                '}';
    }
}
