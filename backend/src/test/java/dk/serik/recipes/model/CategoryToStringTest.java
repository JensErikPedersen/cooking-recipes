package dk.serik.recipes.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@code Category} was the only entity omitting the audit fields from its printed form, so a logged
 * Category silently told you less than a logged Ingredient, Unit or Tag.
 */
class CategoryToStringTest {

    @Test
    @DisplayName("Given a Category, When printed, Then it includes the audit fields its siblings print")
    void categoryPrintsAuditFields() {
        String printed = Category.builder().name("Dessert").description("Sødt").build().toString();

        assertThat(printed).contains("created=");
        assertThat(printed).contains("createdBy=");
        assertThat(printed).contains("updated=");
        assertThat(printed).contains("updatedBy=");
    }
}
