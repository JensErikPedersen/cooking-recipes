package dk.serik.recipes.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.jackson.Jacksonized;

import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.Set;

@JsonInclude(Include.NON_NULL)
@Getter
@Setter
@ToString
public class RecipeDTO extends BaseIdentityDTO {

	@NotBlank(message = "{dk.serik.models.recipe.name.notblank.message}")
	@Size(max = 255, message = "{dk.serik.models.recipe.name.size.message}")
	private String name;

	private String description;

	private String instructions;

	private CategoryDTO category;

	private Set<RecipeIngredientDTO> recipeIngredients;

	private Set<RecipeRatingDTO> recipeRatings;

	private Set<TagDTO> tags;

	@Builder
	@Jacksonized
	public RecipeDTO(String id, OffsetDateTime created, String createdBy, OffsetDateTime updated, String updatedBy, String name, String description, String instructions, String categoryId, String categoryName, Set<RecipeIngredientDTO> recipeIngredients, Set<RecipeRatingDTO> recipeRatings, Set<TagDTO> tags) {
		super(id, created, createdBy, updated, updatedBy);
		this.name = name;
		this.description = description;
		this.instructions = instructions;
		// an absent category must stay absent: building an all-null CategoryDTO here would
		// serialise as "category":{} and read as "create a new category" further downstream
		this.category = (Objects.isNull(categoryId) && Objects.isNull(categoryName))
				? null
				: CategoryDTO.builder().id(categoryId).name(categoryName).build();
		this.recipeIngredients = recipeIngredients;
		this.recipeRatings = recipeRatings;
		this.tags = tags;
	}



	/**
	 * Hand-written builder method so the JSON contract is symmetric: {@code RecipeDTO} serialises a
	 * nested {@code category} object, and this lets it be read back in that same form. Lombok skips
	 * generating a builder setter when one is declared here.
	 */
	public static class RecipeDTOBuilder {
		public RecipeDTOBuilder category(CategoryDTO category) {
			if (Objects.nonNull(category)) {
				this.categoryId = category.getId();
				this.categoryName = category.getName();
			}
			return this;
		}
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof RecipeDTO recipeDTO)) return false;
		if (!super.equals(o)) return false;
        return Objects.equals(name, recipeDTO.name) && Objects.equals(description, recipeDTO.description) && Objects.equals(instructions, recipeDTO.instructions) && Objects.equals(category, recipeDTO.category);
	}

	@Override
	public int hashCode() {
		return Objects.hash(super.hashCode(), name, description, instructions, category);
	}
}
