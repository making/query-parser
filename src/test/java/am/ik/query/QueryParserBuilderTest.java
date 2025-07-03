package am.ik.query;

import am.ik.query.ast.AndNode;
import am.ik.query.ast.FieldNode;
import am.ik.query.ast.FuzzyNode;
import am.ik.query.ast.NotNode;
import am.ik.query.ast.OrNode;
import am.ik.query.ast.RangeNode;
import am.ik.query.ast.TokenNode;
import am.ik.query.ast.WildcardNode;
import am.ik.query.lexer.TokenType;
import am.ik.query.parser.QueryParser;
import am.ik.query.validation.QueryValidationException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class QueryParserBuilderTest {

	@Test
	void testDefaultOperatorAnd() {
		QueryParser parser = QueryParser.builder().defaultOperator(QueryParser.BooleanOperator.AND).build();

		Query query = parser.parse("hello world");

		assertThat(query.countNodes(AndNode.class)).isEqualTo(1);
	}

	@Test
	void testDefaultOperatorOr() {
		QueryParser parser = QueryParser.builder().defaultOperator(QueryParser.BooleanOperator.OR).build();

		Query query = parser.parse("hello world");

		assertThat(query.countNodes(OrNode.class)).isEqualTo(1);
	}

	@Test
	void testValidationOnParse() {
		QueryParser parser = QueryParser.builder().validateAfterParse(true).throwOnValidationError(true).build();

		assertThatThrownBy(() -> parser.parse("")).isInstanceOf(QueryValidationException.class);
	}

	@Test
	void testFieldParser() {
		QueryParser parser = QueryParser.builder()
			.fieldParser("date", value -> new TokenNode(TokenType.KEYWORD, "parsed:" + value))
			.build();

		Query query = parser.parse("date:2023");

		assertThat(query.toString()).contains("parsed:2023");
	}

	@Test
	void testAllowedTokenTypes() {
		QueryParser parser = QueryParser.builder().allowedTokenTypes(TokenType.KEYWORD, TokenType.PHRASE).build();

		Query query = parser.parse("hello \"world\"");

		assertThat(query.extractKeywords()).containsExactly("hello");
		assertThat(query.extractPhrases()).containsExactly("world");
	}

	@Test
	void testComplexNestedQuery() {
		QueryParser parser = QueryParser.create();

		Query query = parser.parse("(java OR python) AND (\"web development\" OR \"data science\") -basic");

		assertThat(query.countNodes(OrNode.class)).isGreaterThan(0);
		assertThat(query.countNodes(AndNode.class)).isGreaterThan(0);
		assertThat(query.extractPhrases()).isNotEmpty();
		assertThat(query.extractExclusions()).isNotEmpty();
	}

	@Test
	void testWildcardSupport() {
		QueryParser parser = QueryParser.create();

		Query query = parser.parse("hel* wor?d");

		assertThat(query.countNodes(WildcardNode.class)).isEqualTo(2);
	}

	@Test
	void testFuzzySupport() {
		QueryParser parser = QueryParser.create();

		Query query = parser.parse("hello~2 world~");

		assertThat(query.countNodes(FuzzyNode.class)).isEqualTo(2);
	}

	@Test
	void testRangeSupport() {
		QueryParser parser = QueryParser.create();

		Query query = parser.parse("[1 TO 10] {a TO z}");

		assertThat(query.countNodes(RangeNode.class)).isEqualTo(2);
	}

	@Test
	void testFieldSupport() {
		QueryParser parser = QueryParser.create();

		Query query = parser.parse("title:hello author:\"John Doe\"");

		assertThat(query.countNodes(FieldNode.class)).isEqualTo(2);
	}

	@Test
	void testNotOperator() {
		QueryParser parser = QueryParser.create();

		Query query = parser.parse("NOT hello");

		assertThat(query.countNodes(NotNode.class)).isEqualTo(1);
	}

	@Test
	void testRequiredOperator() {
		QueryParser parser = QueryParser.create();

		Query query = parser.parse("+hello world");

		assertThat(query.extractKeywords()).contains("hello", "world");
	}

	@Test
	void testMixedOperators() {
		QueryParser parser = QueryParser.create();

		Query query = parser.parse("hello AND world OR java NOT python");

		assertThat(query.countNodes(AndNode.class)).isGreaterThan(0);
		assertThat(query.countNodes(OrNode.class)).isGreaterThan(0);
		assertThat(query.countNodes(NotNode.class)).isGreaterThan(0);
	}

}