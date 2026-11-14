package dk.serik.recipes.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A picture's metadata. Its files, one per {@code PictureSize}, are on disk under the id, written by
 * {@code PictureStorage}; they never change once written (docs/PLAN_V8.md decision 3).
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "picture")
public class Picture extends BaseIdentifierEntity {

	@Column(nullable = false)
	private String name;

	@Column(nullable = false)
	private String altText;

	@Override
	public String toString() {
		return "Picture{" +
				"name='" + name + '\'' +
				", altText='" + altText + '\'' +
				", id=" + id +
				", created=" + created +
				", createdBy='" + createdBy + '\'' +
				", updated=" + updated +
				", updatedBy='" + updatedBy + '\'' +
				'}';
	}
}
