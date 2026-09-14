package dk.serik.recipes.exceptions;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ApplicationExceptionHandlerTest {

    private static final String LEAKY_MESSAGE =
            "could not execute statement [Unique index or primary key violation: \"PUBLIC.CONSTRAINT_INDEX_8 "
                    + "ON PUBLIC.RATING(RATING)\"; SQL statement: insert into rating (created,created_by) values (?,?)]";

    private final ApplicationExceptionHandler handler = new ApplicationExceptionHandler();

    @Test
    @DisplayName("Given an unhandled exception, When handled, Then its raw message is not returned to the client")
    public void shouldNotLeakInternalDetail() {
        // Given - When
        ExceptionEnvelope envelope = handler.handleGeneralException(new IllegalStateException(LEAKY_MESSAGE));

        // Then
        assertThat(envelope.getMessage()).doesNotContain("SQL", "PUBLIC.RATING", "insert into");
        assertThat(envelope.getErrorCode()).isEqualTo(ApplicationErrorCodes.UNHANDLED_EXCEPTION.getCode());
    }

    @Test
    @DisplayName("Given an unhandled exception, When handled, Then a reference is returned so the log can be found")
    public void shouldReturnACorrelationReference() {
        // Given - When
        ExceptionEnvelope envelope = handler.handleGeneralException(new IllegalStateException(LEAKY_MESSAGE));

        // Then
        assertThat(envelope.getDescription()).isNotBlank();
    }

    @Test
    @DisplayName("Given two unhandled exceptions, When handled, Then each carries its own reference")
    public void shouldReturnADistinctReferencePerFailure() {
        // Given - When
        ExceptionEnvelope first = handler.handleGeneralException(new IllegalStateException("one"));
        ExceptionEnvelope second = handler.handleGeneralException(new IllegalStateException("two"));

        // Then
        assertThat(first.getDescription()).isNotEqualTo(second.getDescription());
    }
}
