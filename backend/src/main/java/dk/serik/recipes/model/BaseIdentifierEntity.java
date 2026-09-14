package dk.serik.recipes.model;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;

import java.sql.Types;
import java.util.Objects;
import java.util.UUID;

@MappedSuperclass
@EntityListeners(BaseEntityListener.class)
public class BaseIdentifierEntity extends BaseEntity {

	@Setter(AccessLevel.NONE)
	@Getter(AccessLevel.PUBLIC)
	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	@JdbcTypeCode(value = Types.VARCHAR)
	@Column(length = 36, columnDefinition = "varchar(36)", updatable = false, nullable = false )
	protected UUID id;

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		BaseIdentifierEntity that = (BaseIdentifierEntity) o;
		if(id==null && that.getId()==null) return false;
		return Objects.equals(id, that.getId());
	}

	@Override
	public int hashCode() {
		return Objects.hash(id);
	}

	@Override
	public String toString() {
		return "BaseIdentifierEntity{" +
				"id='" + id + '\'' +
				", created=" + created +
				", createdBy='" + createdBy + '\'' +
				", updated=" + updated +
				", updatedBy='" + updatedBy + '\'' +
				'}';
	}
}
