package dk.serik.recipes.exceptions;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import org.springframework.http.HttpStatus;

import java.io.Serializable;
import java.util.Objects;

@Getter
public class ServiceException extends RuntimeException implements Serializable {
    private String message;
    private int code;
    private String description;
    private HttpStatus httpStatus;

    @Builder
    public ServiceException(String message, int code, String description, HttpStatus httpStatus) {
        super(message);
        this.message = message;
        this.code = code;
        this.description = description;
        // never leave this null: ResponseEntity rejects a null status, which would turn
        // an intended 4xx into an opaque 500 in ApplicationExceptionHandler
        this.httpStatus = Objects.requireNonNullElse(httpStatus, HttpStatus.INTERNAL_SERVER_ERROR);
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
                '}';
    }
}
