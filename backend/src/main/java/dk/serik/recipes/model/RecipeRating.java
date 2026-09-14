package dk.serik.recipes.model;

import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.*;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "recipe_rating")
public class RecipeRating extends BaseIdentifierEntity {

	@ManyToOne
    @JoinColumn(name = "recipe_id", nullable = false )
	private Recipe recipe;
	
	@ManyToOne
    @JoinColumn(name = "rating_id", nullable = false)
	private Rating rating;
	
	private String description;
	
}
