package dk.serik.recipes.exceptions;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import org.springframework.http.HttpStatus;

import java.io.Serializable;
import java.util.List;
import java.util.Objects;

@Getter
public class ServiceException extends RuntimeException implements Serializable {
    private String message;
    private int code;
    private String description;
    private HttpStatus httpStatus;
    /**
     * The request fields at fault, reported as validation errors on them; empty if none. Built from
     * {@code field}, one field carrying the exception's own message, or from {@code fieldErrors},
     * each with a message of its own.
     */
    private List<ValidationExceptionEnvelope> fieldErrors;

    @Builder
    public ServiceException(String message, int code, String description, HttpStatus httpStatus, String field,
                            List<ValidationExceptionEnvelope> fieldErrors) {
        super(message);
        this.message = message;
        this.code = code;
        this.description = description;
        // never leave this null: ResponseEntity rejects a null status, which would turn
        // an intended 4xx into an opaque 500 in ApplicationExceptionHandler
        this.httpStatus = Objects.requireNonNullElse(httpStatus, HttpStatus.INTERNAL_SERVER_ERROR);
        this.fieldErrors = Objects.nonNull(field)
                ? List.of(new ValidationExceptionEnvelope(field, message))
                : Objects.requireNonNullElse(fieldErrors, List.of());
    }

    /**
     * A rejected argument is always a client error. Every null-argument guard in the service
     * layer goes through here so the status cannot drift between services.
     */
    public static ServiceException badRequest(ApplicationErrorCodes code, String message) {
        return ServiceException.builder()
                .message(message)
                .code(code.getCode())
                .httpStatus(HttpStatus.BAD_REQUEST)
                .build();
    }

    @Override
    public String toString() {
        return "ServiceException{" +
                "message='" + message + '\'' +
                ", code=" + code +
                ", description='" + description + '\'' +
                ", httpStatus=" + httpStatus +
                ", fieldErrors=" + fieldErrors.stream().map(ValidationExceptionEnvelope::getObjectName).toList() +
                '}';
    }
}
