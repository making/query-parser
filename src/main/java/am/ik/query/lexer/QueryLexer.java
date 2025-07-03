package am.ik.query.lexer;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Query lexer with support for advanced query features.
 *
 * @author Toshiaki Maki
 */
public class QueryLexer {

	private static final Set<String> BOOLEAN_OPERATORS = Set.of("AND", "OR", "NOT");

	private String input;

	private int position;

	private int length;

	private List<Token> tokens;

	public List<Token> tokenize(String input) {
		if (input == null) {
			throw new IllegalArgumentException("Input cannot be null");
		}

		this.input = input;
		this.position = 0;
		this.length = input.length();
		this.tokens = new ArrayList<>();

		while (!isAtEnd()) {
			scanToken();
		}

		// Add EOF token
		tokens.add(new Token(TokenType.EOF, ""));

		return tokens;
	}

	private void scanToken() {
		char c = advance();

		switch (c) {
			case ' ':
			case '\t':
			case '\r':
			case '\n':
				whitespace();
				break;
			case '"':
				phrase();
				break;
			case '(':
				addToken(TokenType.LPAREN, "(");
				break;
			case ')':
				addToken(TokenType.RPAREN, ")");
				break;
			case '[':
				addToken(TokenType.RANGE_START, "[");
				break;
			case ']':
				addToken(TokenType.RANGE_END, "]");
				break;
			case '{':
				addToken(TokenType.RANGE_START, "{");
				break;
			case '}':
				addToken(TokenType.RANGE_END, "}");
				break;
			case ':':
				addToken(TokenType.COLON, ":");
				break;
			case '+':
				addToken(TokenType.REQUIRED, "+");
				break;
			case '-':
				minus();
				break;
			case '*':
				if (isWordChar(peek())) {
					keyword(); // Part of a word
				}
				else {
					addToken(TokenType.WILDCARD, "*");
				}
				break;
			case '?':
				if (isWordChar(peek())) {
					keyword(); // Part of a word
				}
				else {
					addToken(TokenType.WILDCARD, "?");
				}
				break;
			case '~':
				addToken(TokenType.FUZZY, "~");
				break;
			case '^':
				addToken(TokenType.BOOST, "^");
				break;
			default:
				if (isWordStart(c)) {
					keyword();
				}
				else {
					// Skip unknown character
				}
		}
	}

	private void whitespace() {
		int start = position - 1;
		while (Character.isWhitespace(peek())) {
			advance();
		}
		addToken(TokenType.WHITESPACE, input.substring(start, position));
	}

	private void phrase() {
		int start = position - 1;
		while (peek() != '"' && !isAtEnd()) {
			advance();
		}

		if (isAtEnd()) {
			// Unterminated phrase
			addToken(TokenType.PHRASE, input.substring(start + 1));
		}
		else {
			// Consume closing "
			advance();
			addToken(TokenType.PHRASE, input.substring(start + 1, position - 1));
		}
	}

	private void minus() {
		// Check if this is an exclusion operator or part of a hyphenated word
		if (position > 1 && isWordChar(input.charAt(position - 2)) && isWordChar(peek())) {
			// Part of hyphenated word like "e-mail"
			keyword();
		}
		else if (isWordChar(peek())) {
			// Exclusion operator followed by a word
			int start = position - 1;
			while (isWordChar(peek()) || peek() == '*' || peek() == '?') {
				advance();
			}
			String value = input.substring(start + 1, position); // Skip the minus
			addToken(TokenType.EXCLUDE, value);
		}
		else {
			// Just a minus sign
			addToken(TokenType.EXCLUDE, "-");
		}
	}

	private void keyword() {
		int start = position - 1;

		// Handle leading minus for exclusion
		boolean isExclusion = false;
		if (start > 0 && input.charAt(start) == '-'
				&& (start == 0 || Character.isWhitespace(input.charAt(start - 1)))) {
			isExclusion = true;
			start++;
		}

		// Scan the word, including quoted strings within words
		while (isWordChar(peek()) || (peek() == '-' && isWordChar(peekNext())) || peek() == '*' || peek() == '?'
				|| peek() == '"') {
			if (peek() == '"') {
				// Include quoted content as part of the keyword
				advance(); // consume opening quote
				while (peek() != '"' && !isAtEnd()) {
					advance();
				}
				if (peek() == '"') {
					advance(); // consume closing quote
				}
			}
			else {
				advance();
			}
		}

		// Check for field:value pattern
		if (peek() == ':' && !Character.isWhitespace(peekNext())) {
			// This is a field name
			String fieldName = input.substring(start, position);
			advance(); // consume ':'
			addToken(TokenType.KEYWORD, fieldName);
			addToken(TokenType.COLON, ":");
			return;
		}

		// Check for fuzzy operator
		if (peek() == '~') {
			String term = input.substring(start, position);
			addToken(TokenType.KEYWORD, term);
			return;
		}

		// Check for quoted value after = (e.g., foo="bar")
		if (peek() == '"' && position > start && input.charAt(position - 1) == '=') {
			// Include the quoted part as part of the keyword
			advance(); // consume opening quote
			while (peek() != '"' && !isAtEnd()) {
				advance();
			}
			if (peek() == '"') {
				advance(); // consume closing quote
			}
		}

		String value = input.substring(start, position);

		// Check for boolean operators
		if (BOOLEAN_OPERATORS.contains(value.toUpperCase(java.util.Locale.ROOT))) {
			switch (value.toUpperCase(java.util.Locale.ROOT)) {
				case "AND":
					addToken(TokenType.AND, value);
					break;
				case "OR":
					addToken(TokenType.OR, value);
					break;
				case "NOT":
					addToken(TokenType.NOT, value);
					break;
			}
		}
		else if ("TO".equals(value.toUpperCase(java.util.Locale.ROOT))) {
			addToken(TokenType.RANGE_TO, value);
		}
		else if (isExclusion) {
			addToken(TokenType.EXCLUDE, value);
		}
		else if (value.contains("*") || value.contains("?")) {
			addToken(TokenType.WILDCARD, value);
		}
		else {
			addToken(TokenType.KEYWORD, value);
		}
	}

	private boolean isWordStart(char c) {
		return Character.isLetterOrDigit(c) || c == '_' || c == '-';
	}

	private boolean isWordChar(char c) {
		return Character.isLetterOrDigit(c) || c == '_' || c == '.' || c == '@' || c == '=' || c == '\\';
	}

	private char advance() {
		return input.charAt(position++);
	}

	private boolean isAtEnd() {
		return position >= length;
	}

	private char peek() {
		if (isAtEnd())
			return '\0';
		return input.charAt(position);
	}

	private char peekNext() {
		if (position + 1 >= length)
			return '\0';
		return input.charAt(position + 1);
	}

	private void addToken(TokenType type, String value) {
		tokens.add(new Token(type, value));
	}

	/**
	 * Creates the default query lexer.
	 * @return the default lexer
	 */
	public static QueryLexer defaultLexer() {
		return new QueryLexer();
	}

}