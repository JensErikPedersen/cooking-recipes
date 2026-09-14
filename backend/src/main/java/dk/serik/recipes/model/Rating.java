package dk.serik.recipes.model;

import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name="rating")
public class Rating extends BaseIdentifierEntity {
	
	@OneToMany(mappedBy = "rating")
	private Set<RecipeRating> recipeRatings;
		
	private Integer rating;
	
	private String description;

	public Rating addRecipeRating(RecipeRating recipeRating) {
		if(Objects.isNull(this.recipeRatings)) {
			this.recipeRatings = new HashSet<>();
		}
		recipeRating.setRating(this);
		this.recipeRatings.add(recipeRating);
		return this;
	}

}
