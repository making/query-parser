package am.ik.query;

import java.util.ArrayList;
import java.util.List;

public class QueryLexer {

	public static List<Token> tokenize(String input) {
		List<Token> tokens = new ArrayList<>();
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
				int start = i++;
				while (i < length && !Character.isWhitespace(input.charAt(i))) {
					i++;
				}
				tokens.add(new Token(TokenType.EXCLUDE, input.substring(start + 1, i)));
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
						&& input.charAt(i) != '-' && input.charAt(i) != '(' && input.charAt(i) != ')') {
					i++;
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
