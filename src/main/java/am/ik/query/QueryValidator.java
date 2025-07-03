package am.ik.query;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.Stack;

/**
 * Validates queries for structural and semantic correctness.
 *
 * @author Toshiaki Maki
 */
public class QueryValidator {

	private QueryValidator() {
	}

	/**
	 * Validates a query using default validation rules.
	 * @param query the query to validate
	 * @return the validation result
	 */
	public static ValidationResult validate(Query query) {
		return validate(query, EnumSet.allOf(TokenType.class));
	}

	/**
	 * Validates a query using specified allowed token types.
	 * @param query the query to validate
	 * @param allowedTokenTypes the set of allowed token types
	 * @return the validation result
	 */
	public static ValidationResult validate(Query query, Set<TokenType> allowedTokenTypes) {
		List<ValidationError> errors = new ArrayList<>();

		// Check for empty query
		if (query.isEmpty()) {
			errors.add(new ValidationError("Query is empty"));
		}

		// Validate structure
		errors.addAll(validateStructure(query.rootNode()));

		// Validate semantic rules
		errors.addAll(validateSemantics(query.rootNode()));

		// Validate token types (both AST nodes and raw tokens)
		errors.addAll(validateTokenTypes(query, allowedTokenTypes));

		return errors.isEmpty() ? ValidationResult.valid() : ValidationResult.invalid(errors);
	}

	private static List<ValidationError> validateStructure(Node root) {
		List<ValidationError> errors = new ArrayList<>();
		StructureValidator validator = new StructureValidator();
		root.walk(validator::visit);
		errors.addAll(validator.getErrors());
		return errors;
	}

	private static List<ValidationError> validateSemantics(Node root) {
		List<ValidationError> errors = new ArrayList<>();
		SemanticsValidator validator = new SemanticsValidator();
		root.accept(validator);
		errors.addAll(validator.getErrors());
		return errors;
	}

	private static List<ValidationError> validateTokenTypes(Node root, Set<TokenType> allowedTokenTypes) {
		List<ValidationError> errors = new ArrayList<>();
		TokenTypeValidator validator = new TokenTypeValidator(allowedTokenTypes);
		root.walk(validator::visit);
		errors.addAll(validator.getErrors());
		return errors;
	}

	private static List<ValidationError> validateTokenTypes(Query query, Set<TokenType> allowedTokenTypes) {
		List<ValidationError> errors = new ArrayList<>();

		// Validate the AST nodes
		TokenTypeValidator nodeValidator = new TokenTypeValidator(allowedTokenTypes);
		query.rootNode().walk(nodeValidator::visit);
		errors.addAll(nodeValidator.getErrors());

		// Also validate original tokens to catch things like BOOST and REQUIRED
		// that might be consumed during parsing but not reflected in the AST
		Object originalTokensObj = query.metadata().properties().get("originalTokens");
		if (originalTokensObj instanceof List<?> originalTokensList) {
			for (Object tokenObj : originalTokensList) {
				if (tokenObj instanceof Token token) {
					if (!allowedTokenTypes.contains(token.type())) {
						errors.add(new ValidationError("Token type not allowed: " + token.type().name()));
					}
				}
			}
		}

		return errors;
	}

	private static class StructureValidator {

		private final List<ValidationError> errors = new ArrayList<>();

		private final Stack<Node> nodeStack = new Stack<>();

		void visit(Node node) {
			// Check for empty groups
			if (node.isGroup() && node.children().isEmpty()) {
				errors.add(new ValidationError("Empty group node: " + node.getClass().getSimpleName()));
			}

			// Check NOT nodes have exactly one child
			if (node instanceof NotNode notNode) {
				if (notNode.children().size() != 1) {
					errors.add(new ValidationError("NOT node must have exactly one child"));
				}
			}

			// Check for deeply nested structures (max depth 10)
			if (node.depth() > 10) {
				errors.add(new ValidationError("Query is too deeply nested (max depth: 10)"));
			}

			// Check for circular references
			if (nodeStack.contains(node)) {
				errors.add(new ValidationError("Circular reference detected in query structure"));
			}

			nodeStack.push(node);
			node.children().forEach(this::visit);
			nodeStack.pop();
		}

		List<ValidationError> getErrors() {
			return errors;
		}

	}

	private static class SemanticsValidator implements NodeVisitor<Void> {

		private final List<ValidationError> errors = new ArrayList<>();

		@Override
		public Void visitToken(TokenNode node) {
			// Validate token value is not empty
			if (node.value().trim().isEmpty()) {
				errors.add(new ValidationError("Empty token value", node.type().name()));
			}
			return null;
		}

		@Override
		public Void visitRoot(RootNode node) {
			node.children().forEach(child -> child.accept(this));
			return null;
		}

		@Override
		public Void visitAnd(AndNode node) {
			// Check for conflicting terms (term and -term)
			checkConflictingTerms(node.children(), "AND");
			node.children().forEach(child -> child.accept(this));
			return null;
		}

		@Override
		public Void visitOr(OrNode node) {
			// OR of all negative terms is likely an error
			boolean allNegative = node.children()
				.stream()
				.allMatch(child -> child instanceof NotNode
						|| (child instanceof TokenNode && ((TokenNode) child).type() == TokenType.EXCLUDE));

			if (allNegative) {
				errors.add(new ValidationError("OR expression contains only negative terms"));
			}

			node.children().forEach(child -> child.accept(this));
			return null;
		}

		@Override
		public Void visitField(FieldNode node) {
			// Validate field name
			if (node.field().trim().isEmpty()) {
				errors.add(new ValidationError("Empty field name"));
			}

			// Validate field value
			if (node.fieldValue().trim().isEmpty()) {
				errors.add(new ValidationError("Empty field value for field: " + node.field()));
			}

			return null;
		}

		@Override
		public Void visitFuzzy(FuzzyNode node) {
			// Fuzzy queries should have reasonable length
			if (node.term().length() < 3) {
				errors.add(new ValidationError(
						"Fuzzy term '" + node.term() + "' is too short (minimum 3 characters recommended)"));
			}
			return null;
		}

		@Override
		public Void visitNot(NotNode node) {
			node.child().accept(this);
			return null;
		}

		@Override
		public Void visitPhrase(PhraseNode node) {
			if (node.phrase().trim().isEmpty()) {
				errors.add(new ValidationError("Empty phrase"));
			}
			return null;
		}

		@Override
		public Void visitWildcard(WildcardNode node) {
			if (node.pattern().trim().isEmpty()) {
				errors.add(new ValidationError("Empty wildcard pattern"));
			}
			return null;
		}

		@Override
		public Void visitRange(RangeNode node) {
			// Check range values
			if (node.start().equals(node.end())) {
				errors.add(new ValidationError("Range start and end values are the same: " + node.start()));
			}

			// Check for wildcard boundaries
			if ("*".equals(node.start()) && "*".equals(node.end())) {
				errors.add(new ValidationError("Range with both boundaries as wildcards matches everything"));
			}

			return null;
		}

		private void checkConflictingTerms(List<Node> nodes, String operator) {
			List<String> positiveTerms = new ArrayList<>();
			List<String> negativeTerms = new ArrayList<>();

			for (Node node : nodes) {
				if (node instanceof TokenNode tokenNode) {
					if (tokenNode.type() == TokenType.EXCLUDE) {
						negativeTerms.add(tokenNode.value());
					}
					else {
						positiveTerms.add(tokenNode.value());
					}
				}
				else if (node instanceof NotNode notNode && notNode.child() instanceof TokenNode tokenNode) {
					negativeTerms.add(tokenNode.value());
				}
			}

			// Check for conflicts
			for (String neg : negativeTerms) {
				if (positiveTerms.contains(neg)) {
					errors.add(new ValidationError(
							operator + " expression contains conflicting terms: " + neg + " and -" + neg));
				}
			}
		}

		List<ValidationError> getErrors() {
			return errors;
		}

	}

	private static class TokenTypeValidator {

		private final Set<TokenType> allowedTokenTypes;

		private final List<ValidationError> errors = new ArrayList<>();

		TokenTypeValidator(Set<TokenType> allowedTokenTypes) {
			this.allowedTokenTypes = allowedTokenTypes;
		}

		void visit(Node node) {
			if (node instanceof TokenNode tokenNode) {
				TokenType type = tokenNode.type();
				if (!allowedTokenTypes.contains(type)) {
					errors.add(new ValidationError("Token type not allowed: " + type.name()));
				}
			}
			else if (node instanceof FieldNode) {
				if (!allowedTokenTypes.contains(TokenType.FIELD)) {
					errors.add(new ValidationError("Token type not allowed: FIELD"));
				}
			}
			else if (node instanceof WildcardNode) {
				if (!allowedTokenTypes.contains(TokenType.WILDCARD)) {
					errors.add(new ValidationError("Token type not allowed: WILDCARD"));
				}
			}
			else if (node instanceof FuzzyNode) {
				if (!allowedTokenTypes.contains(TokenType.FUZZY)) {
					errors.add(new ValidationError("Token type not allowed: FUZZY"));
				}
			}
			else if (node instanceof RangeNode) {
				if (!allowedTokenTypes.contains(TokenType.RANGE_START)
						|| !allowedTokenTypes.contains(TokenType.RANGE_END)
						|| !allowedTokenTypes.contains(TokenType.RANGE_TO)) {
					errors.add(new ValidationError("Token type not allowed: RANGE"));
				}
			}
		}

		List<ValidationError> getErrors() {
			return errors;
		}

	}

}