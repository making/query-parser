package am.ik.query;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for configuring QueryParser to be compatible with v0.1 limitations
 */
class LegacyCompatibilityTest {

	private final QueryParser legacyCompatibleParser = QueryParser.builder()
		.allowedTokenTypes(TokenType.KEYWORD, // Basic keywords
				TokenType.PHRASE, // "quoted phrases"
				TokenType.EXCLUDE, // -excluded terms
				TokenType.OR, // OR operator
				TokenType.AND, // AND operator
				TokenType.NOT, // NOT operator
				TokenType.LPAREN, // (
				TokenType.RPAREN, // )
				TokenType.WHITESPACE, // Whitespace
				TokenType.EOF // End of file
		)
		.validateAfterParse(true) // Enable validation
		.throwOnValidationError(true) // Throw exception on validation error
		.build();

	@Test
	void testBasicKeywordQueries() {
		// These should work in legacy compatibility mode
		Query query1 = legacyCompatibleParser.parse("hello world");
		assertThat(query1.extractKeywords()).containsExactly("hello", "world");

		Query query2 = legacyCompatibleParser.parse("java");
		assertThat(query2.extractKeywords()).containsExactly("java");
	}

	@Test
	void testSpaceSeparatedTermsSupport() {
		// Legacy parser supports space-separated terms as AND operations
		Query query = legacyCompatibleParser.parse("java spring boot");

		// Should extract all keywords
		assertThat(query.extractKeywords()).containsExactly("java", "spring", "boot");

		// Space-separated terms create AndNode structure
		assertThat(QueryUtils.countNodesOfType(query, AndNode.class)).isGreaterThan(0);
		assertThat(query.hasAndOperations()).isTrue(); // True because AndNode exists
	}

	@Test
	void testPhraseQueries() {
		// Phrase queries should work
		Query query = legacyCompatibleParser.parse("\"Spring Boot\"");
		assertThat(query.extractPhrases()).containsExactly("Spring Boot");
	}

	@Test
	void testBooleanOperators() {
		// AND operator
		Query andQuery = legacyCompatibleParser.parse("java AND spring");
		assertThat(andQuery.hasAndOperations()).isTrue();
		assertThat(andQuery.extractKeywords()).containsExactly("java", "spring");

		// OR operator
		Query orQuery = legacyCompatibleParser.parse("java OR kotlin");
		assertThat(orQuery.hasOrOperations()).isTrue();
		assertThat(orQuery.extractKeywords()).containsExactly("java", "kotlin");

		// NOT operator
		Query notQuery = legacyCompatibleParser.parse("java NOT android");
		assertThat(notQuery.extractKeywords()).containsExactly("java");
	}

	@Test
	void testExclusionOperator() {
		// Exclusion with minus operator should work
		Query query = legacyCompatibleParser.parse("spring -deprecated");
		assertThat(query.extractKeywords()).containsExactly("spring");
		assertThat(query.extractExclusions()).containsExactly("deprecated");
		assertThat(query.hasExclusions()).isTrue();
	}

	@Test
	void testGroupingWithParentheses() {
		// Parentheses for grouping should work
		Query query = legacyCompatibleParser.parse("(java OR kotlin) AND spring");
		assertThat(query.hasOrOperations()).isTrue();
		assertThat(query.hasAndOperations()).isTrue();
		assertThat(query.extractKeywords()).containsExactly("java", "kotlin", "spring");
	}

	@Test
	void testComplexLegacyQuery() {
		// Complex query that should work in legacy mode
		Query query = legacyCompatibleParser
			.parse("(\"Spring Boot\" OR \"Spring Framework\") AND java -android -legacy");

		assertThat(query.extractPhrases()).containsExactly("Spring Boot", "Spring Framework");
		assertThat(query.extractKeywords()).containsExactly("java");
		assertThat(query.extractExclusions()).containsExactly("android", "legacy");
		assertThat(query.hasOrOperations()).isTrue();
		assertThat(query.hasAndOperations()).isTrue();
		assertThat(query.hasExclusions()).isTrue();
	}

	// Test that advanced features are rejected

	@Test
	void testFieldQueriesAreRejected() {
		// Field queries should be rejected
		assertThatThrownBy(() -> legacyCompatibleParser.parse("title:hello"))
			.isInstanceOf(QueryValidationException.class)
			.hasMessageContaining("FIELD");
	}

	@Test
	void testWildcardQueriesAreRejected() {
		// Wildcard queries should be rejected
		assertThatThrownBy(() -> legacyCompatibleParser.parse("spring*")).isInstanceOf(QueryValidationException.class)
			.hasMessageContaining("WILDCARD");

		assertThatThrownBy(() -> legacyCompatibleParser.parse("wor?d")).isInstanceOf(QueryValidationException.class)
			.hasMessageContaining("WILDCARD");
	}

	@Test
	void testFuzzyQueriesAreRejected() {
		// Fuzzy queries should be rejected
		assertThatThrownBy(() -> legacyCompatibleParser.parse("hello~")).isInstanceOf(QueryValidationException.class)
			.hasMessageContaining("FUZZY");

		assertThatThrownBy(() -> legacyCompatibleParser.parse("spring~2")).isInstanceOf(QueryValidationException.class)
			.hasMessageContaining("FUZZY");
	}

	@Test
	void testRangeQueriesAreRejected() {
		// Range queries should be rejected
		assertThatThrownBy(() -> legacyCompatibleParser.parse("[1 TO 10]")).isInstanceOf(QueryValidationException.class)
			.hasMessageContaining("RANGE");

		assertThatThrownBy(() -> legacyCompatibleParser.parse("{a TO z}")).isInstanceOf(QueryValidationException.class)
			.hasMessageContaining("RANGE");
	}

	@Test
	void testBoostQueriesAreRejected() {
		// Boost queries should be rejected
		assertThatThrownBy(() -> legacyCompatibleParser.parse("important^2"))
			.isInstanceOf(QueryValidationException.class)
			.hasMessageContaining("BOOST");
	}

	@Test
	void testRequiredTermsAreRejected() {
		// Required term operator should be rejected
		assertThatThrownBy(() -> legacyCompatibleParser.parse("+required")).isInstanceOf(QueryValidationException.class)
			.hasMessageContaining("REQUIRED");
	}

	@Test
	void testLegacyVsModernComparison() {
		// Create both parsers
		QueryParser modernParser = QueryParser.builder().build();

		// Basic query should work in both
		String basicQuery = "java AND spring";
		Query legacyResult = legacyCompatibleParser.parse(basicQuery);
		Query modernResult = modernParser.parse(basicQuery);

		assertThat(legacyResult.extractKeywords()).isEqualTo(modernResult.extractKeywords());
		assertThat(legacyResult.hasAndOperations()).isEqualTo(modernResult.hasAndOperations());

		// Advanced query should only work in modern
		String advancedQuery = "title:spring";
		assertThatThrownBy(() -> legacyCompatibleParser.parse(advancedQuery))
			.isInstanceOf(QueryValidationException.class);

		// But should work fine with modern parser
		Query modernAdvanced = modernParser.parse(advancedQuery);
		assertThat(modernAdvanced.extractFields()).containsKey("title");
	}

	@Test
	void testLegacyCompatibleParserMetadata() {
		// Even in legacy mode, metadata should be available
		Query query = legacyCompatibleParser.parse("hello world");
		QueryMetadata metadata = query.metadata();

		assertThat(metadata.tokenCount()).isGreaterThan(0);
		assertThat(metadata.nodeCount()).isGreaterThan(0);
		assertThat(metadata.maxDepth()).isGreaterThan(0);
		assertThat(metadata.parseTime()).isNotNull();
	}

	@Test
	void testLegacyCompatibleParserTransformations() {
		// Legacy compatible parser should still support transformations
		Query query = legacyCompatibleParser.parse("HELLO WORLD");

		Query normalized = query.normalize();
		assertThat(normalized.toString()).contains("hello");
		assertThat(normalized.toString()).contains("world");

		Query optimized = query.optimize();
		assertThat(optimized).isNotNull();
	}

}