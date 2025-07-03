package am.ik.query;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests the same queries as QueryParserTest but using legacy-compatible parser
 * configuration to ensure compatibility with v0.1 behavior
 */
class LegacyCompatibleQueryParserTest {

	// Legacy-compatible parser with v0.1 limitations
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
		.validateAfterParse(false) // Disable validation for structural tests
		.throwOnValidationError(false)
		.build();

	@Test
	void singleKeyword() {
		Query query = legacyCompatibleParser.parse("hello");

		// Modern API assertions - focus on behavior rather than structure
		assertThat(query.extractKeywords()).containsExactly("hello");
		assertThat(query.isEmpty()).isFalse();

		// Verify it's a single keyword token
		assertThat(query.rootNode()).isInstanceOf(TokenNode.class);
		TokenNode tokenNode = (TokenNode) query.rootNode();
		assertThat(tokenNode.type()).isEqualTo(TokenType.KEYWORD);
		assertThat(tokenNode.value()).isEqualTo("hello");
	}

	@Test
	void singlePhrase() {
		Query query = legacyCompatibleParser.parse("\"hello world\"");

		// Modern API assertions
		assertThat(query.extractPhrases()).containsExactly("hello world");
		assertThat(query.hasPhrases()).isTrue();

		// Verify it's a phrase node
		assertThat(query.rootNode()).isInstanceOf(PhraseNode.class);
		PhraseNode phraseNode = (PhraseNode) query.rootNode();
		assertThat(phraseNode.phrase()).isEqualTo("hello world");
	}

	@Test
	void doubleKeyword() {
		Query query = legacyCompatibleParser.parse("hello world");

		// Modern API assertions - focus on the actual behavior
		assertThat(query.extractKeywords()).containsExactly("hello", "world");
		assertThat(query.isEmpty()).isFalse();
		// Space-separated terms create AndNode structure
		assertThat(QueryUtils.countNodesOfType(query, AndNode.class)).isGreaterThan(0);
		assertThat(query.hasAndOperations()).isTrue(); // True because AndNode exists

		// Verify structure with space-separated terms
		assertThat(query.rootNode()).isInstanceOf(AndNode.class);
	}

	@Test
	void or() {
		Query query = legacyCompatibleParser.parse("hello or world");

		// Modern API assertions
		assertThat(query.extractKeywords()).containsExactly("hello", "world");
		assertThat(query.hasOrOperations()).isTrue();

		// Structure will be OrNode due to modern parser
		assertThat(QueryUtils.countNodesOfType(query, OrNode.class)).isGreaterThan(0);
		assertThat(query.rootNode()).isInstanceOf(OrNode.class);
	}

	@Test
	void orPhrase() {
		Query query = legacyCompatibleParser.parse("hello \"or\" world");

		// Modern API assertions
		assertThat(query.extractKeywords()).containsExactly("hello", "world");
		assertThat(query.extractPhrases()).containsExactly("or");
		assertThat(query.hasPhrases()).isTrue();
		// Space-separated terms create AND structure
		assertThat(QueryUtils.countNodesOfType(query, AndNode.class)).isGreaterThan(0);

		// With default AND operator, this becomes: hello AND "or" AND world
		assertThat(query.rootNode()).isInstanceOf(AndNode.class);
	}

	@Test
	void exclude() {
		Query query = legacyCompatibleParser.parse("hello -world");

		// Modern API assertions
		assertThat(query.extractKeywords()).containsExactly("hello");
		assertThat(query.extractExclusions()).containsExactly("world");
		assertThat(query.hasExclusions()).isTrue();

		// Structure will include AndNode and NotNode due to modern parser
		assertThat(QueryUtils.countNodesOfType(query, AndNode.class)).isGreaterThan(0);
		assertThat(QueryUtils.countNodesOfType(query, NotNode.class)).isGreaterThan(0);
	}

	@Test
	void nest() {
		Query query = legacyCompatibleParser.parse("hello (world or java)");

		// Modern API assertions
		assertThat(query.extractKeywords()).containsExactly("hello", "world", "java");
		assertThat(query.hasOrOperations()).isTrue();
		assertThat(QueryUtils.countNodesOfType(query, OrNode.class)).isGreaterThan(0);
		assertThat(QueryUtils.countNodesOfType(query, AndNode.class)).isGreaterThan(0);

		// Modern parser creates proper nested structure with AndNode and OrNode
		assertThat(query.rootNode()).isInstanceOf(AndNode.class);
	}

	@Test
	void nestAndKeyword() {
		Query query = legacyCompatibleParser.parse("hello (world or java) test");

		// Modern API assertions
		assertThat(query.extractKeywords()).containsExactly("hello", "world", "java", "test");
		assertThat(query.hasOrOperations()).isTrue();
		assertThat(QueryUtils.countNodesOfType(query, OrNode.class)).isGreaterThan(0);
		assertThat(QueryUtils.countNodesOfType(query, AndNode.class)).isGreaterThan(0);

		// Modern parser creates proper nested structure
		assertThat(query.rootNode()).isInstanceOf(AndNode.class);
	}

	@Test
	void deepNest() {
		Query query = legacyCompatibleParser.parse("hello (world or (java or bean))");

		// Modern API assertions
		assertThat(query.extractKeywords()).containsExactly("hello", "world", "java", "bean");
		assertThat(query.hasOrOperations()).isTrue();
		assertThat(QueryUtils.countNodesOfType(query, OrNode.class)).isGreaterThan(0);
		assertThat(QueryUtils.countNodesOfType(query, AndNode.class)).isGreaterThan(0);

		// Verify proper nesting depth
		assertThat(query.metadata().maxDepth()).isGreaterThan(2);

		// Modern parser creates proper deeply nested structure
		assertThat(query.rootNode()).isInstanceOf(AndNode.class);
	}

	@Test
	void hyphen() {
		Query query = legacyCompatibleParser.parse("hello-world");

		// Modern API assertions
		assertThat(query.extractKeywords()).containsExactly("hello-world");

		// Verify it's a single keyword token
		assertThat(query.rootNode()).isInstanceOf(TokenNode.class);
		TokenNode tokenNode = (TokenNode) query.rootNode();
		assertThat(tokenNode.type()).isEqualTo(TokenType.KEYWORD);
		assertThat(tokenNode.value()).isEqualTo("hello-world");
	}

	@Test
	void quotedString() {
		Query query = legacyCompatibleParser.parse("foo=\"bar\"");

		// Modern API assertions - this gets parsed as a single keyword
		assertThat(query.extractKeywords()).containsExactly("foo=\"bar\"");

		// This gets parsed as a single keyword token
		assertThat(query.rootNode()).isInstanceOf(TokenNode.class);
		TokenNode tokenNode = (TokenNode) query.rootNode();
		assertThat(tokenNode.type()).isEqualTo(TokenType.KEYWORD);
		assertThat(tokenNode.value()).isEqualTo("foo=\"bar\"");
	}

	// Additional tests to verify legacy compatibility constraints

	@Test
	void legacyCompatibilityValidation() {
		// Test that legacy-compatible parser can handle all the basic queries
		// but would reject advanced features when validation is enabled

		QueryParser strictLegacyParser = QueryParser.builder()
			.allowedTokenTypes(TokenType.KEYWORD, TokenType.PHRASE, TokenType.EXCLUDE, TokenType.OR, TokenType.AND,
					TokenType.NOT, TokenType.LPAREN, TokenType.RPAREN, TokenType.WHITESPACE, TokenType.EOF)
			.validateAfterParse(true)
			.throwOnValidationError(false) // Don't throw for testing
			.build();

		// These should parse successfully
		Query basicQuery = strictLegacyParser.parse("hello world");
		assertThat(basicQuery.extractKeywords()).containsExactly("hello", "world");

		Query phraseQuery = strictLegacyParser.parse("\"hello world\"");
		assertThat(phraseQuery.extractPhrases()).containsExactly("hello world");

		Query booleanQuery = strictLegacyParser.parse("hello OR world");
		assertThat(booleanQuery.hasOrOperations()).isTrue();

		Query excludeQuery = strictLegacyParser.parse("hello -world");
		assertThat(excludeQuery.hasExclusions()).isTrue();

		Query nestedQuery = strictLegacyParser.parse("hello (world OR java)");
		assertThat(nestedQuery.extractKeywords()).containsExactly("hello", "world", "java");
	}

	@Test
	void testSpaceSeparatedVsExplicitAndBehavior() {
		// Verify that space-separated terms work as expected in legacy mode
		Query spaceSeparatedQuery = legacyCompatibleParser.parse("java spring framework");
		Query explicitQuery = legacyCompatibleParser.parse("java AND spring AND framework");

		// Both should extract the same keywords
		assertThat(spaceSeparatedQuery.extractKeywords()).containsExactly("java", "spring", "framework");
		assertThat(explicitQuery.extractKeywords()).containsExactly("java", "spring", "framework");

		// Both queries should have AndNode structure and AND operations
		assertThat(QueryUtils.countNodesOfType(spaceSeparatedQuery, AndNode.class)).isGreaterThan(0);
		assertThat(spaceSeparatedQuery.hasAndOperations()).isTrue(); // True because
																		// AndNode exists

		// Explicit query should also have AndNode structure and AND operations
		assertThat(QueryUtils.countNodesOfType(explicitQuery, AndNode.class)).isGreaterThan(0);
		assertThat(explicitQuery.hasAndOperations()).isTrue(); // True because AndNode
																// exists
	}

	@Test
	void testLegacyParserMetadata() {
		// Verify that legacy parser generates proper metadata
		Query query = legacyCompatibleParser.parse("hello (world OR java) -test");
		QueryMetadata metadata = query.metadata();

		assertThat(metadata.tokenCount()).isGreaterThan(0);
		assertThat(metadata.nodeCount()).isGreaterThan(0);
		assertThat(metadata.maxDepth()).isGreaterThan(1);
		assertThat(metadata.parseTime()).isNotNull();
		assertThat(metadata.parsedAt()).isNotNull();
	}

}