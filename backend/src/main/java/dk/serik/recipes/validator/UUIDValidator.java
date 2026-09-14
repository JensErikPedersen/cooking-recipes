package dk.serik.recipes.validator;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.Objects;

public class UUIDValidator implements ConstraintValidator<UUID, String> {
    @Override
    public void initialize(UUID constraintAnnotation) {
        ConstraintValidator.super.initialize(constraintAnnotation);
    }

    @Override
    public boolean isValid(String s, ConstraintValidatorContext context) {
        if(Objects.isNull(s)) {
            return false;
        }
        try {
            java.util.UUID uuid = java.util.UUID.fromString(s);
            return true;
        } catch (Exception e) {
            return false;
        }

    }
}
