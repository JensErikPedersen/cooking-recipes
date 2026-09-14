package dk.serik.recipes.model;

import jakarta.persistence.*;
import lombok.*;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "recipe")
@Builder //TODO: update to use builder
public class Recipe extends BaseIdentifierEntity {
	
	@Column(nullable = false, unique = true)
	private String name;
	
	private String description;
	
	private String instructions;
	
	@ManyToOne()
	@JoinColumn(name="category_id", nullable=false)
	private Category category;
	
	@OneToMany(mappedBy= "recipe", cascade = CascadeType.ALL)
	private Set<RecipeIngredient> recipeIngredients;

	@OneToMany(mappedBy= "recipe", fetch= FetchType.LAZY, cascade = CascadeType.ALL)
	private Set<RecipeRating> recipeRatings;

	@ManyToMany
	@JoinTable(
			name = "recipe_tag",
			joinColumns = @JoinColumn(name = "recipe_id"),
			inverseJoinColumns = @JoinColumn(name = "tag_id")
	)
	private Set<Tag> tags;

	public Recipe addRecipeIngredient(RecipeIngredient recipeIngredient) {
		if(Objects.isNull(recipeIngredients)) {
			recipeIngredients = new HashSet<>();
		}
		recipeIngredient.setRecipe(this);
		recipeIngredients.add(recipeIngredient);
		return this;
	}

	public Recipe addRecipeRating(RecipeRating recipeRating) {
		if(Objects.isNull(this.recipeRatings)) {
			this.recipeRatings = new HashSet<>();
		}
		recipeRating.setRecipe(this);
		this.recipeRatings.add(recipeRating);

		return this;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("Recipe{");
		sb.append("name='" + name + '\'');
		sb.append(	", description='" + description + '\'');
		sb.append(", instructions='" + instructions + '\'');
		sb.append(	", category=" + category);
		if(Objects.nonNull(recipeIngredients)) recipeIngredients.forEach(ri -> sb.append("Ingredient: " + ri.getIngredient().getName() + ", Amount: " + ri.getAmount() + " " + ri.getUnit().getLabel() + ", "));
		if(Objects.nonNull(recipeRatings)) recipeRatings.forEach(rr -> sb.append("Rating: " + rr.getRating().getRating()));
		if(Objects.nonNull(tags)) tags.forEach(rt -> sb.append("Tag: " + rt.getName()));
		sb.append(", id='" + id + '\'');
		sb.append(", created=" + created);
		sb.append(", createdBy='" + createdBy + '\'');
		sb.append(", updated=" + updated );
		sb.append(", updatedBy='" + updatedBy + '\'');
		sb.append('}');
		return sb.toString();
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof Recipe recipe)) return false;
		if (!super.equals(o)) return false;
        return Objects.equals(name, recipe.name) && Objects.equals(description, recipe.description) && Objects.equals(instructions, recipe.instructions) && Objects.equals(category, recipe.category);
	}

	@Override
	public int hashCode() {
		return Objects.hash(super.hashCode(), name, description, instructions, category);
	}
}
