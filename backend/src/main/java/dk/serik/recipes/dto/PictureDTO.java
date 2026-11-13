package dk.serik.recipes.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.jackson.Jacksonized;

import java.time.OffsetDateTime;

/**
 * A picture as the API returns it: the metadata only. Its files are at
 * {@code /api/v1/pictures/{id}/small}, {@code /medium} and {@code /large}.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Getter
public class PictureDTO extends BaseIdentityDTO {

    private String name;

    private String altText;

    @Builder
    @Jacksonized
    public PictureDTO(String id, OffsetDateTime created, String createdBy, OffsetDateTime updated, String updatedBy,
                      String name, String altText) {
        super(id, created, createdBy, updated, updatedBy);
        this.name = name;
        this.altText = altText;
    }

    @Override
    public String toString() {
        return "PictureDTO{" +
                "name='" + name + '\'' +
                ", altText='" + altText + '\'' +
                ", id='" + getId() + '\'' +
                ", created=" + getCreated() +
                ", createdBy='" + getCreatedBy() + '\'' +
                ", updated=" + getUpdated() +
                ", updatedBy='" + getUpdatedBy() + '\'' +
                '}';
    }
}
