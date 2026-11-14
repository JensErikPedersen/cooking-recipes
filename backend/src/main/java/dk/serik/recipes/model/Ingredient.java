package dk.serik.recipes.model;

import jakarta.persistence.Column;
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
@Table(name = "ingredient")
public class Ingredient extends BaseIdentifierEntity {
	
	@Column(nullable = false, unique = true)
	private String name;
	
	private String description;

	// Optional, and shared as a recipe's is (docs/PLAN_V8.md decision 2).
	@ManyToOne
	@JoinColumn(name = "picture_id")
	private Picture picture;

	@Override
	public String toString() {
		return "Ingredient{" +
				"name='" + name + '\'' +
				", description='" + description + '\'' +
				", id=" + id +
				", created=" + created +
				", createdBy='" + createdBy + '\'' +
				", updated=" + updated +
				", updatedBy='" + updatedBy + '\'' +
				'}';
	}

}
