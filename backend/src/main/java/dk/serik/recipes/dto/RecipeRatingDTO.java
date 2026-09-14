package dk.serik.recipes.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

import java.time.OffsetDateTime;
import java.util.Objects;


@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RecipeRatingDTO extends BaseIdentityDTO {
	private String recipeId;
	private String ratingId;
	private Integer rating;
	private String description;

	@Builder
	public RecipeRatingDTO(String id, OffsetDateTime created, String createdBy, OffsetDateTime updated, String updatedBy, String recipeId, String ratingId, Integer rating, String description) {
		super(id, created, createdBy, updated, updatedBy);
		this.recipeId = recipeId;
		this.rating = rating;
		this.ratingId = ratingId;
		this.description = description;
	}

	@Override
	public String toString() {
		return "RecipeRatingDTO{" +
				"recipeId='" + recipeId + '\'' +
				", ratingId='" + ratingId + '\'' +
				", rating=" + rating +
				", description='" + description + '\'' +
				", created=" + getCreated() +
				", createdBy='" + getCreatedBy() + '\'' +
				", updated=" + getUpdated() +
				", updatedBy='" + getUpdatedBy() + '\'' +
				'}';
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof RecipeRatingDTO that)) return false;
		if (!super.equals(o)) return false;
		return Objects.equals(recipeId, that.recipeId) && Objects.equals(ratingId, that.ratingId) && Objects.equals(rating, that.rating) && Objects.equals(description, that.description);
	}

	@Override
	public int hashCode() {
		return Objects.hash(super.hashCode(), recipeId, ratingId, rating, description);
	}
}
