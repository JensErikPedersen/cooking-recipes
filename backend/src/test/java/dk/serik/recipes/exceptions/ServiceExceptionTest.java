package dk.serik.recipes.exceptions;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

public class ServiceExceptionTest {

    @Test
    @DisplayName("Given no http status, When ServiceException is built, Then it defaults to INTERNAL_SERVER_ERROR")
    public void shouldDefaultHttpStatusWhenOmitted() {
        // Given - When
        ServiceException exception = ServiceException.builder()
                .message("no status supplied")
                .code(ApplicationErrorCodes.UNHANDLED_EXCEPTION.getCode())
                .build();

        // Then
        assertThat(exception.getHttpStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Test
    @DisplayName("Given an http status, When ServiceException is built, Then that status is kept")
    public void shouldKeepSuppliedHttpStatus() {
        // Given - When
        ServiceException exception = ServiceException.builder()
                .message("not found")
                .code(ApplicationErrorCodes.RATING_NOT_FOUND.getCode())
                .httpStatus(HttpStatus.NOT_FOUND)
                .build();

        // Then
        assertThat(exception.getHttpStatus()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    @DisplayName("Given a ServiceException without status, When building a ResponseEntity, Then it does not throw")
    public void shouldBeUsableInResponseEntity() {
        // Given
        ServiceException exception = ServiceException.builder()
                .message("no status supplied")
                .code(ApplicationErrorCodes.UNHANDLED_EXCEPTION.getCode())
                .build();

        // When - Then: this is what ApplicationExceptionHandler does; a null status threw here
        assertThatCode(() -> new ResponseEntity<>(
                ExceptionEnvelope.builder().message(exception.getMessage()).build(),
                exception.getHttpStatus()))
                .doesNotThrowAnyException();
    }
}
