package am.ik.query;

import java.util.LinkedList;
import java.util.List;

public class QueryLexer {

	public static List<Token> tokenize(String input) {
		List<Token> tokens = new LinkedList<>();
		int length = input.length();
		for (int i = 0; i < length;) {
			char current = input.charAt(i);
			if (Character.isWhitespace(current)) {
				int start = i;
				while (i < length && Character.isWhitespace(input.charAt(i))) {
					i++;
				}
				tokens.add(new Token(TokenType.WHITESPACE, input.substring(start, i)));
			}
			else if (current == '"') {
				int start = i++;
				while (i < length && input.charAt(i) != '"') {
					i++;
				}
				if (i < length) {
					i++; // consume closing "
				}
				tokens.add(new Token(TokenType.PHRASE, input.substring(start + 1, i - 1)));
			}
			else if (current == '-') {
				if (i > 0 && !Character.isWhitespace(input.charAt(i - 1))
						&& (Character.isLetterOrDigit(input.charAt(i + 1)) || input.charAt(i + 1) == '"')) {
					int start = i - 1;
					while (start > 0 && !Character.isWhitespace(input.charAt(start - 1))
							&& input.charAt(start - 1) != '(' && input.charAt(start - 1) != ')') {
						start--;
					}
					while (i < length && !Character.isWhitespace(input.charAt(i)) && input.charAt(i) != '('
							&& input.charAt(i) != ')') {
						i++;
					}
					tokens.remove(tokens.size() - 1);
					tokens.add(new Token(TokenType.KEYWORD, input.substring(start, i)));
				}
				else {
					int start = i++;
					while (i < length && !Character.isWhitespace(input.charAt(i))) {
						i++;
					}
					tokens.add(new Token(TokenType.EXCLUDE, input.substring(start + 1, i)));
				}
			}
			else if (current == '(') {
				tokens.add(new Token(TokenType.LPAREN, String.valueOf(current)));
				i++;
			}
			else if (current == ')') {
				tokens.add(new Token(TokenType.RPAREN, String.valueOf(current)));
				i++;
			}
			else {
				int start = i;
				while (i < length && !Character.isWhitespace(input.charAt(i)) && input.charAt(i) != '"'
						&& input.charAt(i) != '-' && input.charAt(i) != '(' && input.charAt(i) != ')'
						&& input.charAt(i) != '=') {
					i++;
				}
				if (i < length && input.charAt(i) == '=') {
					i++;
					while (i < length && !Character.isWhitespace(input.charAt(i)) && input.charAt(i) != '('
							&& input.charAt(i) != ')') {
						i++;
					}
				}
				String value = input.substring(start, i);
				if (value.equalsIgnoreCase("OR")) {
					tokens.add(new Token(TokenType.OR, value));
				}
				else {
					tokens.add(new Token(TokenType.KEYWORD, value));
				}
			}
		}
		return tokens;
	}

}
