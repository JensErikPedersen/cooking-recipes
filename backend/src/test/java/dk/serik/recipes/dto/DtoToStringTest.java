package dk.serik.recipes.dto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * These strings surface in logs and in test failure messages, where a mislabelled field costs real
 * debugging time - the whole reason finding L7 exists. The assertions target the specific defects
 * rather than pinning the full format, so adding a field later does not break them.
 */
class DtoToStringTest {

    @Test
    @DisplayName("Given a TagDTO, When printed, Then its name is labelled name, not label")
    void tagDtoLabelsNameCorrectly() {
        String printed = TagDTO.builder().name("Mexi").build().toString();

        assertThat(printed).contains("name='Mexi'");
        assertThat(printed).doesNotContain("label=");
    }

    @Test
    @DisplayName("Given a UnitDTO, When printed, Then its name is labelled name, not description")
    void unitDtoLabelsNameCorrectly() {
        String printed = UnitDTO.builder().label("g").name("Gram").build().toString();

        assertThat(printed).contains("label='g'");
        assertThat(printed).contains("name='Gram'");
        assertThat(printed).doesNotContain("description=");
    }

    @Test
    @DisplayName("Given a RatingDTO, When printed, Then the field list does not open with a stray comma")
    void ratingDtoHasNoStrayLeadingComma() {
        String printed = RatingDTO.builder().rating(5).description("Fremragende").build().toString();

        assertThat(printed).doesNotContain("RatingDTO{,");
        assertThat(printed).contains("RatingDTO{rating=5");
    }
}
