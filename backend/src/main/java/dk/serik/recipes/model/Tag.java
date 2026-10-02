package dk.serik.recipes.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "tag")
public class Tag extends BaseIdentifierEntity {
	
	// Unique, as in the Liquibase schema, which is authoritative (ddl-auto=none).
	@Column(nullable = false, unique = true)
	private String name;

	@Override
	public String toString() {
		return "Tag{" +
				"name='" + name + '\'' +
				", id=" + id +
				", created=" + created +
				", createdBy='" + createdBy + '\'' +
				", updated=" + updated +
				", updatedBy='" + updatedBy + '\'' +
				'}';
	}
}
