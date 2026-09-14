package dk.serik.recipes.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@IdClass(RecipeIngredientPK.class)
@Table(name = "recipe_ingredient")
public class RecipeIngredient extends BaseEntity {
	@Id
	@ManyToOne
	@JoinColumn(name="recipe_id", nullable=false)  
	private Recipe recipe;

	@Id
	@ManyToOne
	@JoinColumn(name="ingredient_id", nullable = false)
	private Ingredient ingredient;

	@Column(nullable=false)
	private BigDecimal amount;

	@ManyToOne
	@JoinColumn(name="unit_id", nullable=false)
	private Unit unit;
  	
	// Identity is the composite key (recipe, ingredient). Both are read defensively: a
	// RecipeIngredient may legitimately exist with its back-reference unset before it is
	// attached to a Recipe, and entering a HashSet in that state must not throw.
	private static UUID idOf(BaseIdentifierEntity entity) {
		return Objects.isNull(entity) ? null : entity.getId();
	}

	@Override
	public int hashCode() {
		return Objects.hash(idOf(ingredient), idOf(recipe));
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		RecipeIngredient other = (RecipeIngredient) obj;
		return Objects.equals(idOf(ingredient), idOf(other.ingredient))
				&& Objects.equals(idOf(recipe), idOf(other.recipe));
	}


}
