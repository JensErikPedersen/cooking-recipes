package dk.serik.recipes.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

import java.time.OffsetDateTime;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Getter
public class RatingDTO extends BaseIdentityDTO {
    private Integer rating;

    private String description;

    @Builder
    public RatingDTO(String id, OffsetDateTime created, String createdBy, OffsetDateTime updated, String updatedBy, Integer rating, String description) {
        super(id, created, createdBy, updated, updatedBy);
        this.rating = rating;
        this.description = description;
    }

    @Override
    public String toString() {
        return "RatingDTO{" +
                "rating=" + rating +
                ", description='" + description + '\'' +
                ", id='" + getId() + '\'' +
                ", created=" + getCreated() +
                ", createdBy='" + getCreatedBy() + '\'' +
                ", updated=" + getUpdated() +
                ", updatedBy='" + getUpdatedBy() + '\'' +
                '}';
    }
}
