package dk.serik.recipes.exceptions;

import org.hibernate.exception.ConstraintViolationException.ConstraintKind;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.sql.SQLException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

public class ApplicationExceptionHandlerTest {

    private static final String LEAKY_MESSAGE =
            "could not execute statement [Unique index or primary key violation: \"PUBLIC.CONSTRAINT_INDEX_8 "
                    + "ON PUBLIC.RATING(RATING)\"; SQL statement: insert into rating (created,created_by) values (?,?)]";

    private final ApplicationExceptionHandler handler = new ApplicationExceptionHandler();

    @Test
    @DisplayName("Given an unhandled exception, When handled, Then its raw message is not returned to the client")
    public void shouldNotLeakInternalDetail() {
        // Given - When
        ExceptionEnvelope envelope = handler.handleGeneralException(new IllegalStateException(LEAKY_MESSAGE)).getBody();

        // Then
        assertThat(envelope.getMessage()).doesNotContain("SQL", "PUBLIC.RATING", "insert into");
        assertThat(envelope.getErrorCode()).isEqualTo(ApplicationErrorCodes.UNHANDLED_EXCEPTION.getCode());
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(value = ConstraintKind.class, names = {"UNIQUE", "FOREIGN_KEY"})
    @DisplayName("Given a unique or foreign key violation, When handled, Then 409 without the raw message")
    public void shouldReturnConflictForUniqueAndForeignKey(ConstraintKind kind) {
        // Given - When
        ResponseEntity<ExceptionEnvelope> response = handler.handleDataIntegrityViolation(violation(kind));

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody().getErrorCode()).isEqualTo(ApplicationErrorCodes.DATA_CONFLICT.getCode());
        assertThat(response.getBody().getMessage()).doesNotContain("SQL", "PUBLIC.RATING", "insert into");
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(value = ConstraintKind.class, names = {"NOT_NULL", "CHECK", "OTHER"})
    @DisplayName("Given a NOT NULL, CHECK or unrecognised violation, When handled, Then 500 like any server fault, without the raw message")
    public void shouldReturnServerErrorForOtherKinds(ConstraintKind kind) {
        // Given - When
        ResponseEntity<ExceptionEnvelope> response = handler.handleDataIntegrityViolation(violation(kind));

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody().getErrorCode()).isEqualTo(ApplicationErrorCodes.UNHANDLED_EXCEPTION.getCode());
        assertThat(response.getBody().getMessage()).doesNotContain("SQL", "PUBLIC.RATING", "insert into");
    }

    @Test
    @DisplayName("Given a duplicate key from plain JDBC, which carries no Hibernate kind, When handled, Then 409")
    public void shouldReturnConflictForDuplicateKey() {
        // Given - When
        ResponseEntity<ExceptionEnvelope> response = handler.handleDataIntegrityViolation(new DuplicateKeyException(LEAKY_MESSAGE));

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    @DisplayName("Given a violation that says nothing of its kind, When handled, Then 500 - a 409 must be earned")
    public void shouldReturnServerErrorForUnknownViolation() {
        // Given - When
        ResponseEntity<ExceptionEnvelope> response = handler.handleDataIntegrityViolation(new DataIntegrityViolationException(LEAKY_MESSAGE));

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    /** What Spring hands the handler for a violation raised through Hibernate: the kind is on the cause. */
    private static DataIntegrityViolationException violation(ConstraintKind kind) {
        return new DataIntegrityViolationException(LEAKY_MESSAGE, new org.hibernate.exception.ConstraintViolationException(
                LEAKY_MESSAGE, new SQLException(LEAKY_MESSAGE), kind, "constraint"));
    }

    @Test
    @DisplayName("Given a ServiceException naming a field, When handled, Then the field is reported as a validation error")
    public void shouldReportServiceExceptionFieldAsValidationError() {
        // Given - When
        ExceptionEnvelope envelope = handler.handleBusinessException(ServiceException.builder()
                .message("A category named 'Dessert' already exists")
                .code(ApplicationErrorCodes.CATEGORY_ALREADY_EXISTS.getCode())
                .httpStatus(HttpStatus.CONFLICT)
                .field("name")
                .build(), null).getBody();

        // Then
        assertThat(envelope.getValidationExceptions())
                .extracting("objectName", "message")
                .containsExactly(tuple("name", "A category named 'Dessert' already exists"));
    }

    @Test
    @DisplayName("Given a ServiceException naming several fields, When handled, Then each is reported, in order")
    public void shouldReportEveryServiceExceptionFieldError() {
        // Given - When
        ExceptionEnvelope envelope = handler.handleBusinessException(ServiceException.builder()
                .message("One or more ingredient lines are invalid")
                .code(ApplicationErrorCodes.RECIPE_INGREDIENTS_INVALID.getCode())
                .httpStatus(HttpStatus.BAD_REQUEST)
                .fieldErrors(List.of(
                        new ValidationExceptionEnvelope("recipeIngredients[0].amount", "Needs an amount above zero."),
                        new ValidationExceptionEnvelope("recipeIngredients[2].unitId", "Choose a unit.")))
                .build(), null).getBody();

        // Then
        assertThat(envelope.getValidationExceptions())
                .extracting("objectName", "message")
                .containsExactly(
                        tuple("recipeIngredients[0].amount", "Needs an amount above zero."),
                        tuple("recipeIngredients[2].unitId", "Choose a unit."));
    }

    @Test
    @DisplayName("Given a ServiceException without a field, When handled, Then no validation errors are reported")
    public void shouldReportNoValidationErrorWithoutField() {
        // Given - When
        ExceptionEnvelope envelope = handler.handleBusinessException(ServiceException.builder()
                .message("Category with id x could not be found")
                .code(ApplicationErrorCodes.CATEGORY_NOT_FOUND.getCode())
                .httpStatus(HttpStatus.NOT_FOUND)
                .build(), null).getBody();

        // Then
        assertThat(envelope.getValidationExceptions()).isNull();
    }

    @Test
    @DisplayName("Given an unhandled exception, When handled, Then the status is 500")
    public void shouldReturn500ForUnhandledException() {
        // Given - When
        ResponseEntity<ExceptionEnvelope> response = handler.handleGeneralException(new IllegalStateException("boom"));

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Test
    @DisplayName("Given an unhandled exception, When handled, Then a reference is returned so the log can be found")
    public void shouldReturnACorrelationReference() {
        // Given - When
        ExceptionEnvelope envelope = handler.handleGeneralException(new IllegalStateException(LEAKY_MESSAGE)).getBody();

        // Then
        assertThat(envelope.getDescription()).isNotBlank();
    }

    @Test
    @DisplayName("Given two unhandled exceptions, When handled, Then each carries its own reference")
    public void shouldReturnADistinctReferencePerFailure() {
        // Given - When
        ExceptionEnvelope first = handler.handleGeneralException(new IllegalStateException("one")).getBody();
        ExceptionEnvelope second = handler.handleGeneralException(new IllegalStateException("two")).getBody();

        // Then
        assertThat(first.getDescription()).isNotEqualTo(second.getDescription());
    }
}
