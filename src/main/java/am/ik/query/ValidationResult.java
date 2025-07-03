package am.ik.query;

import java.util.List;
import java.util.Objects;

/**
 * Result of query validation.
 *
 * @author Toshiaki Maki
 */
public record ValidationResult(boolean isValid, List<ValidationError> errors) {

	public ValidationResult {
		Objects.requireNonNull(errors, "errors must not be null");
		errors = List.copyOf(errors);
	}

	/**
	 * Creates a valid result with no errors.
	 * @return a valid result
	 */
	public static ValidationResult valid() {
		return new ValidationResult(true, List.of());
	}

	/**
	 * Creates an invalid result with the given errors.
	 * @param errors the validation errors
	 * @return an invalid result
	 */
	public static ValidationResult invalid(List<ValidationError> errors) {
		if (errors.isEmpty()) {
			throw new IllegalArgumentException("Invalid result must have at least one error");
		}
		return new ValidationResult(false, errors);
	}

	/**
	 * Creates an invalid result with a single error.
	 * @param error the validation error
	 * @return an invalid result
	 */
	public static ValidationResult invalid(ValidationError error) {
		return invalid(List.of(error));
	}

	/**
	 * Creates an invalid result with a single error message.
	 * @param message the error message
	 * @return an invalid result
	 */
	public static ValidationResult invalid(String message) {
		return invalid(new ValidationError(message));
	}

	/**
	 * Throws QueryValidationException if validation failed.
	 * @throws QueryValidationException if not valid
	 */
	public void throwIfInvalid() {
		if (!isValid) {
			throw new QueryValidationException(this);
		}
	}

	/**
	 * Combines this result with another result.
	 * @param other the other result
	 * @return combined result
	 */
	public ValidationResult combine(ValidationResult other) {
		if (this.isValid && other.isValid) {
			return valid();
		}

		List<ValidationError> combinedErrors = new java.util.ArrayList<>();
		combinedErrors.addAll(this.errors);
		combinedErrors.addAll(other.errors);

		return invalid(combinedErrors);
	}

}