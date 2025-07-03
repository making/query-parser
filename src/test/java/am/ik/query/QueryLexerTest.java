package am.ik.query;

import am.ik.query.lexer.QueryLexer;
import am.ik.query.lexer.Token;
import am.ik.query.lexer.TokenType;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class QueryLexerTest {

	private final QueryLexer lexer = new QueryLexer();

	@Test
	void testSimpleKeywords() {
		List<Token> tokens = lexer.tokenize("hello world");

		assertThat(tokens).extracting(Token::type)
			.containsExactly(TokenType.KEYWORD, TokenType.WHITESPACE, TokenType.KEYWORD, TokenType.EOF);
		assertThat(tokens.get(0).value()).isEqualTo("hello");
		assertThat(tokens.get(2).value()).isEqualTo("world");
	}

	@Test
	void testPhrase() {
		List<Token> tokens = lexer.tokenize("\"hello world\"");

		assertThat(tokens).extracting(Token::type).contains(TokenType.PHRASE);
		assertThat(tokens).anySatisfy(t -> {
			assertThat(t.type()).isEqualTo(TokenType.PHRASE);
			assertThat(t.value()).isEqualTo("hello world");
		});
	}

	@Test
	void testBooleanOperators() {
		List<Token> tokens = lexer.tokenize("hello AND world OR java NOT python");

		assertThat(tokens).extracting(Token::type).contains(TokenType.AND, TokenType.OR, TokenType.NOT);
	}

	@Test
	void testExclusion() {
		List<Token> tokens = lexer.tokenize("-excluded");

		assertThat(tokens).anySatisfy(t -> {
			assertThat(t.type()).isEqualTo(TokenType.EXCLUDE);
			assertThat(t.value()).isEqualTo("excluded");
		});
	}

	@Test
	void testHyphenatedWord() {
		List<Token> tokens = lexer.tokenize("e-mail");

		assertThat(tokens).anySatisfy(t -> {
			assertThat(t.type()).isEqualTo(TokenType.KEYWORD);
			assertThat(t.value()).isEqualTo("e-mail");
		});
	}

	@Test
	void testFieldQuery() {
		List<Token> tokens = lexer.tokenize("title:hello");

		assertThat(tokens).extracting(Token::type).contains(TokenType.KEYWORD, TokenType.COLON, TokenType.KEYWORD);
		assertThat(tokens.get(0).value()).isEqualTo("title");
		assertThat(tokens.get(2).value()).isEqualTo("hello");
	}

	@Test
	void testWildcard() {
		List<Token> tokens = lexer.tokenize("hel* wor?d");

		assertThat(tokens).filteredOn(t -> t.type() == TokenType.WILDCARD)
			.hasSize(2)
			.extracting(Token::value)
			.containsExactly("hel*", "wor?d");
	}

	@Test
	void testFuzzy() {
		List<Token> tokens = lexer.tokenize("hello~2");

		assertThat(tokens).extracting(Token::type).contains(TokenType.KEYWORD, TokenType.FUZZY);
		assertThat(tokens.get(0).value()).isEqualTo("hello");
	}

	@Test
	void testBoost() {
		List<Token> tokens = lexer.tokenize("hello^2");

		assertThat(tokens).extracting(Token::type).contains(TokenType.KEYWORD, TokenType.BOOST);
	}

	@Test
	void testRange() {
		List<Token> tokens = lexer.tokenize("[1 TO 10]");

		assertThat(tokens).extracting(Token::type)
			.contains(TokenType.RANGE_START, TokenType.KEYWORD, TokenType.RANGE_TO, TokenType.KEYWORD,
					TokenType.RANGE_END);

		// Filter out whitespace tokens for position-based assertions
		List<Token> nonWhitespace = tokens.stream().filter(t -> t.type() != TokenType.WHITESPACE).toList();
		assertThat(nonWhitespace.get(0).value()).isEqualTo("[");
		assertThat(nonWhitespace.get(4).value()).isEqualTo("]");
	}

	@Test
	void testParentheses() {
		List<Token> tokens = lexer.tokenize("(hello world)");

		assertThat(tokens).extracting(Token::type).contains(TokenType.LPAREN, TokenType.RPAREN);
	}

	@Test
	void testRequired() {
		List<Token> tokens = lexer.tokenize("+required");

		assertThat(tokens).anySatisfy(t -> assertThat(t.type()).isEqualTo(TokenType.REQUIRED));
	}

	@Test
	void testComplexQuery() {
		List<Token> tokens = lexer.tokenize("title:\"hello world\" AND (java OR python) -basic field:[1 TO 10]");

		assertThat(tokens).extracting(Token::type)
			.contains(TokenType.KEYWORD, TokenType.COLON, TokenType.PHRASE, TokenType.AND, TokenType.LPAREN,
					TokenType.OR, TokenType.RPAREN, TokenType.EXCLUDE, TokenType.RANGE_START, TokenType.RANGE_TO,
					TokenType.RANGE_END);
	}

	@Test
	void testUnterminatedPhrase() {
		List<Token> tokens = lexer.tokenize("\"unterminated");

		assertThat(tokens).anySatisfy(t -> {
			assertThat(t.type()).isEqualTo(TokenType.PHRASE);
			assertThat(t.value()).isEqualTo("unterminated");
		});
	}

	@Test
	void testEmptyInput() {
		List<Token> tokens = lexer.tokenize("");

		assertThat(tokens).hasSize(1);
		assertThat(tokens.get(0).type()).isEqualTo(TokenType.EOF);
	}

	@Test
	void testNullInput() {
		assertThatThrownBy(() -> lexer.tokenize(null)).isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void testSpecialCharacters() {
		List<Token> tokens = lexer.tokenize("user@example.com file_name.txt");

		assertThat(tokens).filteredOn(t -> t.type() == TokenType.KEYWORD)
			.extracting(Token::value)
			.containsExactly("user@example.com", "file_name.txt");
	}

	@Test
	void testMixedCase() {
		List<Token> tokens = lexer.tokenize("Hello AND World OR Java");

		assertThat(tokens).extracting(Token::type).contains(TokenType.AND, TokenType.OR);
	}

}