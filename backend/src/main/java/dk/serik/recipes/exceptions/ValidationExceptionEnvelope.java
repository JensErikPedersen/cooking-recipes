package dk.serik.recipes.exceptions;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
public class ValidationExceptionEnvelope {

	private String message;

	private String objectName;

	public ValidationExceptionEnvelope() {
	}

	public ValidationExceptionEnvelope(String objectName, String message) {
		this.message = message;
		this.objectName = objectName;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public String getObjectName() {
		return objectName;
	}

	public void setObjectName(String field) {
		this.objectName = field;
	}
}
