package dk.serik.recipes.exceptions;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

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
        ExceptionEnvelope envelope = handler.handleGeneralException(new IllegalStateException(LEAKY_MESSAGE)).getBody();

        // Then
        assertThat(envelope.getMessage()).doesNotContain("SQL", "PUBLIC.RATING", "insert into");
        assertThat(envelope.getErrorCode()).isEqualTo(ApplicationErrorCodes.UNHANDLED_EXCEPTION.getCode());
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
