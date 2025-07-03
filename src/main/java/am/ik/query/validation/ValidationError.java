package am.ik.query.validation;

import java.util.Objects;

/**
 * Represents a single validation error.
 *
 * @author Toshiaki Maki
 */
public record ValidationError(String message, String field, Object invalidValue) {

	public ValidationError {
		Objects.requireNonNull(message, "message must not be null");
	}

	/**
	 * Creates a validation error with just a message.
	 * @param message the error message
	 */
	public ValidationError(String message) {
		this(message, null, null);
	}

	/**
	 * Creates a validation error with message and field.
	 * @param message the error message
	 * @param field the field that failed validation
	 */
	public ValidationError(String message, String field) {
		this(message, field, null);
	}

	@Override
	public String toString() {
		if (field != null) {
			return field + ": " + message + (invalidValue != null ? " (was: " + invalidValue + ")" : "");
		}
		return message;
	}

}