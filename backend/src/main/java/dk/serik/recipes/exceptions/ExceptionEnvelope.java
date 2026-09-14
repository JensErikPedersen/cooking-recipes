package dk.serik.recipes.exceptions;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;
import lombok.Builder;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
@Getter
public class ExceptionEnvelope {

	// one shared mapper: building one per call is wasteful on what is already an error path,
	// and Jackson 3 mappers are immutable and thread-safe
	private static final JsonMapper MAPPER = JsonMapper.builder().build();

	private int errorCode;

	private String message;

	private String description;

	private List<ValidationExceptionEnvelope> validationExceptions;
	@Builder
	public ExceptionEnvelope(int errorCode, String message, String description, List<ValidationExceptionEnvelope> validationExceptions) {
		this.errorCode = errorCode;
		this.message = message;
		this.description = description;
		this.validationExceptions = validationExceptions;
	}

	public void addValidationException(ValidationExceptionEnvelope ve) {
		if (validationExceptions == null) {
			validationExceptions = new ArrayList<>();
		}
		this.validationExceptions.add(ve);
	}



	// For Unit Testing Purpose
	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		ExceptionEnvelope that = (ExceptionEnvelope) o;
		return errorCode == that.errorCode && Objects.equals(message, that.message) && Objects.equals(description, that.description) && Objects.equals(validationExceptions, that.validationExceptions);
	}

	@Override
	public int hashCode() {
		return Objects.hash(errorCode, message, description, validationExceptions);
	}

	@Override
	public String toString() {
		try {
			return MAPPER.writeValueAsString(this);
		} catch (JacksonException e) {
			return "AppExceptionEnvelope [errorCode=" + errorCode + ", message=" + message + ", description=" + description + ", validationExceptions="
					+ validationExceptions + "]";
		}

	}
}
