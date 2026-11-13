package dk.serik.recipes.model;

import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * The Welcome page's picture, or none. The table has exactly one row, created by
 * {@code db.changelog_1.8.xml}; nothing inserts or deletes one (docs/PLAN_V8.md decision 2).
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "welcome_page")
public class WelcomePage extends BaseIdentifierEntity {

	@ManyToOne
	@JoinColumn(name = "picture_id")
	private Picture picture;
}
