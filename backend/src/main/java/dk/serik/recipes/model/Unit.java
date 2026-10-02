package dk.serik.recipes.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.*;

import java.util.Objects;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name="unit")
public class Unit extends BaseIdentifierEntity {
	
	// Mirrors the Liquibase schema, which is authoritative (ddl-auto=none): name is unique, label is not.
	@Column(nullable = false)
	private String label;
	
	@Column(nullable = false, unique = true)
	private String name;


	@Override
	public String toString() {
		return "Unit{" +
				"label='" + label + '\'' +
				", name='" + name + '\'' +
				", id=" + id +
				", created=" + created +
				", createdBy='" + createdBy + '\'' +
				", updated=" + updated +
				", updatedBy='" + updatedBy + '\'' +
				'}';
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof Unit unit)) return false;
		if (!super.equals(o)) return false;
		return Objects.equals(label, unit.label) && Objects.equals(name, unit.name);
	}

	@Override
	public int hashCode() {
		return Objects.hash(super.hashCode(), label, name);
	}
}
