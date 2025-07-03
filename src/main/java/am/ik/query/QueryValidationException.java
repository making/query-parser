package am.ik.query;

import java.util.List;

/**
 * Exception thrown when query validation fails.
 *
 * @author Toshiaki Maki
 */
public class QueryValidationException extends RuntimeException {

	private final ValidationResult validationResult;

	public QueryValidationException(ValidationResult validationResult) {
		super(buildMessage(validationResult));
		this.validationResult = validationResult;
	}

	public QueryValidationException(String message) {
		super(message);
		this.validationResult = ValidationResult.invalid(List.of(new ValidationError(message)));
	}

	/**
	 * Gets the validation result that caused this exception.
	 * @return the validation result
	 */
	public ValidationResult getValidationResult() {
		return validationResult;
	}

	/**
	 * Gets all validation errors.
	 * @return list of validation errors
	 */
	public List<ValidationError> getErrors() {
		return validationResult.errors();
	}

	private static String buildMessage(ValidationResult result) {
		if (result.isValid()) {
			return "Validation passed but exception was thrown";
		}

		List<ValidationError> errors = result.errors();
		if (errors.size() == 1) {
			return errors.get(0).message();
		}

		StringBuilder sb = new StringBuilder("Query validation failed with ").append(errors.size())
			.append(" errors:\n");

		for (int i = 0; i < errors.size(); i++) {
			sb.append(i + 1).append(". ").append(errors.get(i).message()).append("\n");
		}

		return sb.toString();
	}

}