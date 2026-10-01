package dk.serik.recipes.exceptions;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.context.request.WebRequest;

import java.util.Iterator;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@ControllerAdvice
public class ApplicationExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(ApplicationExceptionHandler.class);

    @ExceptionHandler(value = { ServiceException.class })
    @ResponseBody
    public ResponseEntity<ExceptionEnvelope> handleBusinessException(ServiceException ex, WebRequest request) {
        logger.info("Handler BusinessException: " + ex);
        ExceptionEnvelope exceptionEnvelope = ExceptionEnvelope.builder()
                .message(ex.getMessage())
                .description(ex.getDescription())
                .errorCode(ex.getCode())
                .build();
        // Same shape as a Bean Validation failure, so a client shows both on the field the same way.
        if (ex.getField() != null) {
            exceptionEnvelope.addValidationException(new ValidationExceptionEnvelope(ex.getField(), ex.getMessage()));
        }
        return new ResponseEntity<>(exceptionEnvelope, ex.getHttpStatus());
    }

    @ExceptionHandler(value = { Exception.class })
    @ResponseBody
    public ResponseEntity<ExceptionEnvelope> handleGeneralException(Exception ex) {
        // Spring MVC's own rejections - unknown path, unsupported method or media type and the
        // like - implement ErrorResponse and already carry the right status and a client-safe
        // detail. Without this they would all land below as a 500.
        if (ex instanceof ErrorResponse errorResponse) {
            logger.info("Request rejected: {}", ex.getMessage());
            return rejected(errorResponse.getStatusCode(), errorResponse.getBody().getDetail());
        }

        // The raw message of an unhandled exception routinely carries SQL, table and constraint
        // names. Log it server-side and hand the client only a reference to that log entry.
        String reference = UUID.randomUUID().toString();
        logger.error("Unhandled exception [reference={}]", reference, ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ExceptionEnvelope.builder()
                .message("An unexpected error occurred")
                .description("Reference: " + reference)
                .errorCode(ApplicationErrorCodes.UNHANDLED_EXCEPTION.getCode())
                .build());
    }

    // Not an ErrorResponse, so handled on its own. Its message names Java types; it is not echoed.
    @ExceptionHandler(value = { HttpMessageNotReadableException.class })
    @ResponseBody
    public ResponseEntity<ExceptionEnvelope> handleUnreadableBody(HttpMessageNotReadableException ex) {
        logger.info("Request rejected: {}", ex.getMessage());
        return rejected(HttpStatus.BAD_REQUEST, "The request body could not be read");
    }

    // A database constraint no service checks first, or a write that lost the race against one it
    // does. Raised at commit, after the service has returned. The message names tables and
    // constraints, so it is logged and not echoed.
    @ExceptionHandler(value = { DataIntegrityViolationException.class })
    @ResponseBody
    public ResponseEntity<ExceptionEnvelope> handleDataIntegrityViolation(DataIntegrityViolationException ex) {
        logger.warn("Data integrity violation: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ExceptionEnvelope.builder()
                .message("The change conflicts with existing data")
                .errorCode(ApplicationErrorCodes.DATA_CONFLICT.getCode())
                .build());
    }

    private ResponseEntity<ExceptionEnvelope> rejected(HttpStatusCode status, String message) {
        return ResponseEntity.status(status).body(ExceptionEnvelope.builder()
                .message(message)
                .errorCode(ApplicationErrorCodes.REQUEST_REJECTED.getCode())
                .build());
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(value = { ConstraintViolationException.class })
    @ResponseBody
    public ExceptionEnvelope handleConstraintViolationException(ConstraintViolationException ex) {
        logger.info("handle ConstraintViolationException: " + ex);
        ExceptionEnvelope exceptionEnvelope = ExceptionEnvelope.builder()
                .errorCode(ApplicationErrorCodes.VALIDATION_EXCEPTION.getCode())
                .message("Validation Exception")
                .description("One or more fields, parameters or type do not pass violation criteria")
                .build();
        Set<ConstraintViolation<?>> violations = ex.getConstraintViolations();
        processFieldErrors(exceptionEnvelope, violations);

        return exceptionEnvelope;
    }

    private void processFieldErrors(ExceptionEnvelope exceptionEnvelope, Set<ConstraintViolation<?>> violations) {
        for (ConstraintViolation<?> violation : violations) {
            ValidationExceptionEnvelope vError = new ValidationExceptionEnvelope();
            if(Objects.nonNull(violation.getPropertyPath())) {
                String fieldName = findFieldViolated(violation.getPropertyPath());
                if(Objects.nonNull(fieldName)) {
                    vError.setObjectName(fieldName);
                } else {
                    vError.setObjectName("Unknown field");
                }
            } else {
                vError.setObjectName(null);
            }

            vError.setMessage(violation.getMessage());
            exceptionEnvelope.addValidationException(vError);
        }
    }


    // utilities

    private String findFieldViolated(Path p) {
        Iterator<Path.Node> it = p.iterator();
        Path.Node n = null;
        while (it.hasNext()) {
            n = it.next();
        }

        if(Objects.nonNull(n)) {
            logger.info("returning: " + n.getName());
            return n.getName();
        } else {
            return null;
        }

    }
}
