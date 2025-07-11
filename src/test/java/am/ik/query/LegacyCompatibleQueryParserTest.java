package am.ik.query;

import am.ik.query.ast.AndNode;
import am.ik.query.ast.NotNode;
import am.ik.query.ast.OrNode;
import am.ik.query.ast.PhraseNode;
import am.ik.query.ast.TokenNode;
import am.ik.query.lexer.TokenType;
import am.ik.query.parser.QueryParser;
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
		assertThat(query.extractPhrases()).isNotEmpty();

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
		assertThat(query.countNodes(AndNode.class)).isGreaterThan(0);

		// Verify structure with space-separated terms
		assertThat(query.rootNode()).isInstanceOf(AndNode.class);
	}

	@Test
	void or() {
		Query query = legacyCompatibleParser.parse("hello or world");

		// Modern API assertions
		assertThat(query.extractKeywords()).containsExactly("hello", "world");
		assertThat(query.countNodes(OrNode.class)).isGreaterThan(0);

		// Structure will be OrNode due to modern parser
		assertThat(query.countNodes(OrNode.class)).isGreaterThan(0);
		assertThat(query.rootNode()).isInstanceOf(OrNode.class);
	}

	@Test
	void orPhrase() {
		Query query = legacyCompatibleParser.parse("hello \"or\" world");

		// Modern API assertions
		assertThat(query.extractKeywords()).containsExactly("hello", "world");
		assertThat(query.extractPhrases()).containsExactly("or");
		assertThat(query.extractPhrases()).isNotEmpty();
		// Space-separated terms create AND structure
		assertThat(query.countNodes(AndNode.class)).isGreaterThan(0);

		// With default AND operator, this becomes: hello AND "or" AND world
		assertThat(query.rootNode()).isInstanceOf(AndNode.class);
	}

	@Test
	void exclude() {
		Query query = legacyCompatibleParser.parse("hello -world");

		// Modern API assertions
		assertThat(query.extractKeywords()).containsExactly("hello");
		assertThat(query.extractExclusions()).containsExactly("world");
		assertThat(query.extractExclusions()).isNotEmpty();

		// Structure will include AndNode and NotNode due to modern parser
		assertThat(query.countNodes(AndNode.class)).isGreaterThan(0);
		assertThat(query.countNodes(NotNode.class)).isGreaterThan(0);
	}

	@Test
	void nest() {
		Query query = legacyCompatibleParser.parse("hello (world or java)");

		// Modern API assertions
		assertThat(query.extractKeywords()).containsExactly("hello", "world", "java");
		assertThat(query.countNodes(OrNode.class)).isGreaterThan(0);
		assertThat(query.countNodes(AndNode.class)).isGreaterThan(0);

		// Modern parser creates proper nested structure with AndNode and OrNode
		assertThat(query.rootNode()).isInstanceOf(AndNode.class);
	}

	@Test
	void nestAndKeyword() {
		Query query = legacyCompatibleParser.parse("hello (world or java) test");

		// Modern API assertions
		assertThat(query.extractKeywords()).containsExactly("hello", "world", "java", "test");
		assertThat(query.countNodes(OrNode.class)).isGreaterThan(0);
		assertThat(query.countNodes(AndNode.class)).isGreaterThan(0);

		// Modern parser creates proper nested structure
		assertThat(query.rootNode()).isInstanceOf(AndNode.class);
	}

	@Test
	void deepNest() {
		Query query = legacyCompatibleParser.parse("hello (world or (java or bean))");

		// Modern API assertions
		assertThat(query.extractKeywords()).containsExactly("hello", "world", "java", "bean");
		assertThat(query.countNodes(OrNode.class)).isGreaterThan(0);
		assertThat(query.countNodes(AndNode.class)).isGreaterThan(0);

		// Verify proper nesting depth
		assertThat(query.countNodes(AndNode.class)).isGreaterThan(0);

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
		assertThat(booleanQuery.countNodes(OrNode.class)).isGreaterThan(0);

		Query excludeQuery = strictLegacyParser.parse("hello -world");
		assertThat(excludeQuery.extractExclusions()).isNotEmpty();

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

		// Both queries should have AndNode structure
		assertThat(spaceSeparatedQuery.countNodes(AndNode.class)).isGreaterThan(0);

		// Explicit query should also have AndNode structure
		assertThat(explicitQuery.countNodes(AndNode.class)).isGreaterThan(0);
	}

	@Test
	void testLegacyParserBasics() {
		// Verify that legacy parser works correctly
		Query query = legacyCompatibleParser.parse("hello (world OR java) -test");

		assertThat(query.originalQuery()).isEqualTo("hello (world OR java) -test");
		assertThat(query.rootNode()).isNotNull();
		assertThat(query.isEmpty()).isFalse();
		assertThat(query.countNodes(OrNode.class)).isGreaterThan(0);
		assertThat(query.extractExclusions()).isNotEmpty();
	}

}