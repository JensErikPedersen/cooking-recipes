package dk.serik.recipes.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.jackson.Jacksonized;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * A picture as the API returns it: the metadata only. Its files are at
 * {@code /api/v1/pictures/{id}/small}, {@code /medium} and {@code /large}.
 * <p>
 * A single picture, read or updated, also says where it is used: the recipes and ingredients, by
 * id and name, and whether the Welcome page shows it. The list and a picture nested in a recipe or
 * an ingredient leave those out. Nested, only its {@code id} is read on a write.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Getter
public class PictureDTO extends BaseIdentityDTO {

    @NotBlank(message = "{dk.serik.models.picture.name.notblank.message}")
    @Size(max = 255, message = "{dk.serik.models.picture.name.size.message}")
    private String name;

    @NotBlank(message = "{dk.serik.models.picture.alttext.notblank.message}")
    @Size(max = 255, message = "{dk.serik.models.picture.alttext.size.message}")
    private String altText;

    private List<RecipeDTO> recipes;

    private List<IngredientDTO> ingredients;

    private Boolean welcomePage;

    @Builder
    @Jacksonized
    public PictureDTO(String id, OffsetDateTime created, String createdBy, OffsetDateTime updated, String updatedBy,
                      String name, String altText, List<RecipeDTO> recipes, List<IngredientDTO> ingredients,
                      Boolean welcomePage) {
        super(id, created, createdBy, updated, updatedBy);
        this.name = name;
        this.altText = altText;
        this.recipes = recipes;
        this.ingredients = ingredients;
        this.welcomePage = welcomePage;
    }

    @Override
    public String toString() {
        return "PictureDTO{" +
                "name='" + name + '\'' +
                ", altText='" + altText + '\'' +
                ", recipes=" + recipes +
                ", ingredients=" + ingredients +
                ", welcomePage=" + welcomePage +
                ", id='" + getId() + '\'' +
                ", created=" + getCreated() +
                ", createdBy='" + getCreatedBy() + '\'' +
                ", updated=" + getUpdated() +
                ", updatedBy='" + getUpdatedBy() + '\'' +
                '}';
    }
}
