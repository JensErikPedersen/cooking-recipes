package dk.serik.recipes.validator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class UUIDValidatorTest {

    private UUIDValidator validator;

    @BeforeEach
    public void initTest() {
        validator = new UUIDValidator();
    }

    @Test
    @DisplayName("Given valid UUID, When validated, Then pass")
    public void shouldPassValidation() {
        String uuid = "7c89ec02-63b9-4d68-9720-c22396fca1c7";
        assertThat(validator.isValid(uuid, null)).isTrue();
    }

    @Test
    @DisplayName("Given valid UUID with mixed case, When validated, Then pass")
    public void shouldPassValidationWithMixedcase() {
        String uuid = "7C89Ec02-63b9-4d68-9720-c22396fca1c7";
        assertThat(validator.isValid(uuid, null)).isTrue();
    }

    @Test
    @DisplayName("Given invalid UUID, When validated, Then fail")
    public void shouldFailValidation() {
        String uuid = "7c89ec02-63b9-4d68-9720-";
        assertThat(validator.isValid(uuid, null)).isFalse();
    }

    @Test
    @DisplayName("Given UUID null, When validated, Then fail")
    public void shouldFailValidationWhenNull() {
        String uuid = null;
        assertThat(validator.isValid(uuid, null)).isFalse();
    }

    @Test
    @DisplayName("Given UUID empty, When validated, Then fail")
    public void shouldFailValidationWhenEmpty() {
        String uuid = "";
        assertThat(validator.isValid(uuid, null)).isFalse();
    }

}
