package am.ik.query;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class QueryParserBuilderTest {

	@Test
	void testDefaultOperatorAnd() {
		QueryParser parser = QueryParser.builder().defaultOperator(QueryParser.BooleanOperator.AND).build();

		Query query = parser.parse("hello world");

		assertThat(QueryUtils.countNodesOfType(query, AndNode.class)).isEqualTo(1);
	}

	@Test
	void testDefaultOperatorOr() {
		QueryParser parser = QueryParser.builder().defaultOperator(QueryParser.BooleanOperator.OR).build();

		Query query = parser.parse("hello world");

		assertThat(QueryUtils.countNodesOfType(query, OrNode.class)).isEqualTo(1);
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
		QueryParser parser = QueryParser.builder().build();

		Query query = parser.parse("(java OR python) AND (\"web development\" OR \"data science\") -basic");

		assertThat(query.hasOrOperations()).isTrue();
		assertThat(query.hasAndOperations()).isTrue();
		assertThat(query.hasPhrases()).isTrue();
		assertThat(query.hasExclusions()).isTrue();
	}

	@Test
	void testWildcardSupport() {
		QueryParser parser = QueryParser.builder().build();

		Query query = parser.parse("hel* wor?d");

		assertThat(QueryUtils.countNodesOfType(query, WildcardNode.class)).isEqualTo(2);
	}

	@Test
	void testFuzzySupport() {
		QueryParser parser = QueryParser.builder().build();

		Query query = parser.parse("hello~2 world~");

		assertThat(QueryUtils.countNodesOfType(query, FuzzyNode.class)).isEqualTo(2);
	}

	@Test
	void testRangeSupport() {
		QueryParser parser = QueryParser.builder().build();

		Query query = parser.parse("[1 TO 10] {a TO z}");

		assertThat(QueryUtils.countNodesOfType(query, RangeNode.class)).isEqualTo(2);
	}

	@Test
	void testFieldSupport() {
		QueryParser parser = QueryParser.builder().build();

		Query query = parser.parse("title:hello author:\"John Doe\"");

		assertThat(QueryUtils.countNodesOfType(query, FieldNode.class)).isEqualTo(2);
	}

	@Test
	void testNotOperator() {
		QueryParser parser = QueryParser.builder().build();

		Query query = parser.parse("NOT hello");

		assertThat(QueryUtils.countNodesOfType(query, NotNode.class)).isEqualTo(1);
	}

	@Test
	void testRequiredOperator() {
		QueryParser parser = QueryParser.builder().build();

		Query query = parser.parse("+hello world");

		assertThat(query.extractKeywords()).contains("hello", "world");
	}

	@Test
	void testMixedOperators() {
		QueryParser parser = QueryParser.builder().build();

		Query query = parser.parse("hello AND world OR java NOT python");

		assertThat(query.hasAndOperations()).isTrue();
		assertThat(query.hasOrOperations()).isTrue();
		assertThat(QueryUtils.countNodesOfType(query, NotNode.class)).isGreaterThan(0);
	}

}