package dk.serik.recipes.dto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Guards the DTO hierarchy against public instance fields.
 * <p>
 * The base classes once declared {@code id}, {@code created}, {@code createdBy}, {@code updated} and
 * {@code updatedBy} as {@code public}. Leaf DTOs that carefully declared only {@code @Getter} were
 * therefore still writable through the inherited fields, so their apparent immutability was an
 * illusion - and no amount of care in a subclass could restore it.
 * <p>
 * This has to be a reflection test: the defect is a compile-time property, so there is no runtime
 * behaviour to assert. It is kept rather than deleted because the mistake is easy to make again.
 */
class DtoEncapsulationTest {

    private static final List<Class<?>> DTO_TYPES = List.of(
            BaseDTO.class, BaseIdentityDTO.class, CategoryDTO.class, IngredientDTO.class,
            RatingDTO.class, RecipeDTO.class, RecipeIngredientDTO.class, RecipeRatingDTO.class,
            TagDTO.class, UnitDTO.class);

    @Test
    @DisplayName("Given the DTO hierarchy, When its fields are inspected, Then no instance field is public")
    void noDtoDeclaresAPublicInstanceField() {
        // Given / When
        List<String> offenders = new ArrayList<>();
        for (Class<?> type : DTO_TYPES) {
            for (Field field : type.getDeclaredFields()) {
                if (Modifier.isPublic(field.getModifiers()) && !Modifier.isStatic(field.getModifiers())) {
                    offenders.add(type.getSimpleName() + "." + field.getName());
                }
            }
        }

        // Then
        assertThat(offenders)
                .as("public fields bypass the generated accessors and make every subclass mutable")
                .isEmpty();
    }
}
