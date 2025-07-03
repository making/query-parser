package am.ik.query;

import java.util.ArrayList;
import java.util.List;

/**
 * Legacy lexer for backward compatibility.
 * 
 * @author Toshiaki Maki
 * @deprecated Use QueryLexer instead
 */
@Deprecated(since = "0.2.0", forRemoval = true)
class LegacyQueryLexer {

	private final String input;

	LegacyQueryLexer(String input) {
		this.input = input;
	}

	List<Token> tokenize() {
		List<Token> tokens = new ArrayList<>();
		if (input == null || input.isEmpty()) {
			return tokens;
		}

		int i = 0;
		while (i < input.length()) {
			char c = input.charAt(i);

			// Skip whitespace
			if (Character.isWhitespace(c)) {
				i++;
				continue;
			}

			// Handle quotes
			if (c == '"') {
				i++; // skip opening quote
				int start = i;
				while (i < input.length() && input.charAt(i) != '"') {
					i++;
				}
				String value = input.substring(start, i);
				tokens.add(new Token(TokenType.PHRASE, value));
				if (i < input.length()) {
					i++; // skip closing quote
				}
				continue;
			}

			// Handle parentheses
			if (c == '(') {
				tokens.add(new Token(TokenType.LPAREN, "("));
				i++;
				continue;
			}
			if (c == ')') {
				tokens.add(new Token(TokenType.RPAREN, ")"));
				i++;
				continue;
			}

			// Handle exclusion
			if (c == '-' && i + 1 < input.length() && Character.isLetterOrDigit(input.charAt(i + 1))) {
				i++; // skip minus
				int start = i;
				while (i < input.length() && !Character.isWhitespace(input.charAt(i)) && input.charAt(i) != ')'
						&& input.charAt(i) != '"') {
					i++;
				}
				String value = input.substring(start, i);
				tokens.add(new Token(TokenType.EXCLUDE, value));
				continue;
			}

			// Handle keywords and operators
			int start = i;
			while (i < input.length() && !Character.isWhitespace(input.charAt(i)) && input.charAt(i) != '('
					&& input.charAt(i) != ')') {
				// Check for quoted value after equals
				if (input.charAt(i) == '=' && i + 1 < input.length() && input.charAt(i + 1) == '"') {
					i++; // include =
					i++; // skip opening quote
					while (i < input.length() && input.charAt(i) != '"') {
						i++;
					}
					if (i < input.length()) {
						i++; // include closing quote
					}
					break;
				}
				else if (input.charAt(i) == '"') {
					break;
				}
				i++;
			}
			String value = input.substring(start, i);

			// Check for OR operator
			if ("or".equalsIgnoreCase(value) || "OR".equals(value)) {
				tokens.add(new Token(TokenType.OR, value));
			}
			else if ("and".equalsIgnoreCase(value) || "AND".equals(value)) {
				tokens.add(new Token(TokenType.AND, value));
			}
			else if ("not".equalsIgnoreCase(value) || "NOT".equals(value)) {
				tokens.add(new Token(TokenType.NOT, value));
			}
			else {
				tokens.add(new Token(TokenType.KEYWORD, value));
			}
		}

		return tokens;
	}

}