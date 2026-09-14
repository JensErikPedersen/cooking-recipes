package dk.serik.recipes.exceptions;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.List;
import java.util.Objects;

@Order(Ordered.HIGHEST_PRECEDENCE)
@ControllerAdvice
public class MethodArgumentNotValidExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(MethodArgumentNotValidExceptionHandler.class);

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ResponseBody
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ExceptionEnvelope methodArgumentNotValidException(MethodArgumentNotValidException ex) {
        logger.info("handle MethodArgumentNotValidException: {}", ex.getMessage());
        ExceptionEnvelope exceptionEnvelope =ExceptionEnvelope.builder()
                .errorCode(ApplicationErrorCodes.VALIDATION_EXCEPTION.getCode())
                .message("Validation Exception")
                .description("One or more fields, parameters or type do not pass violation criteria")
                .build();
        BindingResult result = ex.getBindingResult();
        List<FieldError> fieldErrors = result.getFieldErrors();

        processFieldErrors(exceptionEnvelope, fieldErrors);

        return exceptionEnvelope;
    }

    private void processFieldErrors(ExceptionEnvelope exceptionEnvelope, List<FieldError> fieldErrors) {
        for (FieldError violation : fieldErrors) {
            ValidationExceptionEnvelope vError = new ValidationExceptionEnvelope();
            // FieldError.getField() is never null, so the nested null-checks this replaced
            // could not run
            vError.setObjectName(violation.getField());
            vError.setMessage(violation.getDefaultMessage());
            exceptionEnvelope.addValidationException(vError);
        }
    }
}
