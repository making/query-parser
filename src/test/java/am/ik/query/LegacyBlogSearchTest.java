package am.ik.query;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Practical example of using legacy-compatible parser for blog content search
 */
class LegacyBlogSearchTest {

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
		.validateAfterParse(false) // Disable validation for empty queries
		.throwOnValidationError(false)
		.build();

	@Test
	void testSimpleBlogContentSearch() {
		Query query = legacyCompatibleParser.parse("spring");

		String sql = convertToContentSearchSql(query);

		assertThat(sql).isEqualTo("content LIKE '%spring%'");
	}

	@Test
	void testMultipleKeywordSearch() {
		Query query = legacyCompatibleParser.parse("spring boot");

		String sql = convertToContentSearchSql(query);

		assertThat(sql).isEqualTo("(content LIKE '%spring%' AND content LIKE '%boot%')");
	}

	@Test
	void testSpaceSeparatedTermsSupport() {
		// Legacy parser supports space-separated terms as AND operations
		Query spaceSeparatedQuery = legacyCompatibleParser.parse("java spring framework");
		String spaceSeparatedSql = convertToContentSearchSql(spaceSeparatedQuery);

		Query explicitQuery = legacyCompatibleParser.parse("java AND spring AND framework");
		String explicitSql = convertToContentSearchSql(explicitQuery);

		// Both should generate similar AND conditions
		assertThat(spaceSeparatedSql).contains("content LIKE '%java%'");
		assertThat(spaceSeparatedSql).contains("content LIKE '%spring%'");
		assertThat(spaceSeparatedSql).contains("content LIKE '%framework%'");
		assertThat(spaceSeparatedSql).contains(" AND ");

		// Verify space-separated terms create proper structure
		assertThat(spaceSeparatedSql)
			.isEqualTo("(content LIKE '%java%' AND content LIKE '%spring%' AND content LIKE '%framework%')");
	}

	@Test
	void testPhraseSearch() {
		Query query = legacyCompatibleParser.parse("\"Spring Boot Framework\"");

		String sql = convertToContentSearchSql(query);

		assertThat(sql).isEqualTo("content LIKE '%Spring Boot Framework%'");
	}

	@Test
	void testBooleanOperators() {
		// AND operator
		Query andQuery = legacyCompatibleParser.parse("java AND spring");
		String andSql = convertToContentSearchSql(andQuery);
		assertThat(andSql).isEqualTo("(content LIKE '%java%' AND content LIKE '%spring%')");

		// OR operator
		Query orQuery = legacyCompatibleParser.parse("java OR kotlin");
		String orSql = convertToContentSearchSql(orQuery);
		assertThat(orSql).isEqualTo("(content LIKE '%java%' OR content LIKE '%kotlin%')");

		// NOT operator
		Query notQuery = legacyCompatibleParser.parse("java NOT android");
		String notSql = convertToContentSearchSql(notQuery);
		assertThat(notSql).isEqualTo("(content LIKE '%java%' AND NOT content LIKE '%android%')");
	}

	@Test
	void testExclusionSearch() {
		Query query = legacyCompatibleParser.parse("spring -deprecated");

		String sql = convertToContentSearchSql(query);

		assertThat(sql).isEqualTo("(content LIKE '%spring%' AND NOT content LIKE '%deprecated%')");
	}

	@Test
	void testComplexBlogSearch() {
		Query query = legacyCompatibleParser.parse("(java OR kotlin) AND \"tutorial\" -legacy");

		String sql = convertToContentSearchSql(query);

		assertThat(sql).contains("content LIKE '%java%'");
		assertThat(sql).contains("content LIKE '%kotlin%'");
		assertThat(sql).contains("content LIKE '%tutorial%'");
		assertThat(sql).contains("NOT content LIKE '%legacy%'");
		assertThat(sql).contains(" OR ");
		assertThat(sql).contains(" AND ");
	}

	@Test
	void testGroupingWithParentheses() {
		Query query = legacyCompatibleParser.parse("(spring OR \"Spring Boot\") AND -legacy");

		String sql = convertToContentSearchSql(query);

		assertThat(sql).contains("(content LIKE '%spring%' OR content LIKE '%Spring Boot%')");
		assertThat(sql).contains("NOT content LIKE '%legacy%'");
		assertThat(sql).contains(" AND ");
	}

	@Test
	void testEmptyQuery() {
		Query query = legacyCompatibleParser.parse("");

		String sql = convertToContentSearchSql(query);

		assertThat(sql).isEqualTo("1=1"); // No restrictions, return all
	}

	@Test
	void testOnlyExclusions() {
		Query query = legacyCompatibleParser.parse("-deprecated -legacy");

		String sql = convertToContentSearchSql(query);

		assertThat(sql).isEqualTo("(NOT content LIKE '%deprecated%' AND NOT content LIKE '%legacy%')");
	}

	// Test that advanced features are properly rejected when manually validated
	@Test
	void testFieldQueriesRejected() {
		Query query = legacyCompatibleParser.parse("title:spring");
		ValidationResult result = QueryValidator.validate(query,
				java.util.EnumSet.of(TokenType.KEYWORD, TokenType.PHRASE, TokenType.EXCLUDE, TokenType.OR,
						TokenType.AND, TokenType.NOT, TokenType.LPAREN, TokenType.RPAREN, TokenType.WHITESPACE,
						TokenType.EOF));
		assertThat(result.isValid()).isFalse();
		assertThat(result.errors().get(0).message()).contains("FIELD");
	}

	@Test
	void testWildcardsRejected() {
		Query query = legacyCompatibleParser.parse("spring*");
		ValidationResult result = QueryValidator.validate(query,
				java.util.EnumSet.of(TokenType.KEYWORD, TokenType.PHRASE, TokenType.EXCLUDE, TokenType.OR,
						TokenType.AND, TokenType.NOT, TokenType.LPAREN, TokenType.RPAREN, TokenType.WHITESPACE,
						TokenType.EOF));
		assertThat(result.isValid()).isFalse();
		assertThat(result.errors().get(0).message()).contains("WILDCARD");
	}

	@Test
	void testFuzzySearchRejected() {
		Query query = legacyCompatibleParser.parse("spring~2");
		ValidationResult result = QueryValidator.validate(query,
				java.util.EnumSet.of(TokenType.KEYWORD, TokenType.PHRASE, TokenType.EXCLUDE, TokenType.OR,
						TokenType.AND, TokenType.NOT, TokenType.LPAREN, TokenType.RPAREN, TokenType.WHITESPACE,
						TokenType.EOF));
		assertThat(result.isValid()).isFalse();
		assertThat(result.errors().get(0).message()).contains("FUZZY");
	}

	@Test
	void testRangeQueriesRejected() {
		Query query = legacyCompatibleParser.parse("[1 TO 10]");
		ValidationResult result = QueryValidator.validate(query,
				java.util.EnumSet.of(TokenType.KEYWORD, TokenType.PHRASE, TokenType.EXCLUDE, TokenType.OR,
						TokenType.AND, TokenType.NOT, TokenType.LPAREN, TokenType.RPAREN, TokenType.WHITESPACE,
						TokenType.EOF));
		assertThat(result.isValid()).isFalse();
		// Check if any error message contains RANGE (might have multiple validation
		// errors)
		assertThat(result.errors().stream().anyMatch(error -> error.message().contains("RANGE"))).isTrue();
	}

	@Test
	void testBoostRejected() {
		Query query = legacyCompatibleParser.parse("important^2");
		ValidationResult result = QueryValidator.validate(query,
				java.util.EnumSet.of(TokenType.KEYWORD, TokenType.PHRASE, TokenType.EXCLUDE, TokenType.OR,
						TokenType.AND, TokenType.NOT, TokenType.LPAREN, TokenType.RPAREN, TokenType.WHITESPACE,
						TokenType.EOF));
		assertThat(result.isValid()).isFalse();
		assertThat(result.errors().get(0).message()).contains("BOOST");
	}

	@Test
	void testRequiredTermsRejected() {
		Query query = legacyCompatibleParser.parse("+required");
		ValidationResult result = QueryValidator.validate(query,
				java.util.EnumSet.of(TokenType.KEYWORD, TokenType.PHRASE, TokenType.EXCLUDE, TokenType.OR,
						TokenType.AND, TokenType.NOT, TokenType.LPAREN, TokenType.RPAREN, TokenType.WHITESPACE,
						TokenType.EOF));
		assertThat(result.isValid()).isFalse();
		assertThat(result.errors().get(0).message()).contains("REQUIRED");
	}

	// Practical example with parameterized SQL
	@Test
	void testParameterizedContentSearch() {
		Query query = legacyCompatibleParser.parse("spring AND tutorial -deprecated");

		SqlParameterizedResult result = convertToParameterizedContentSql(query);

		assertThat(result.sql()).contains("content LIKE ?");
		assertThat(result.sql()).contains("NOT content LIKE ?");
		assertThat(result.parameters()).containsExactlyInAnyOrder("%spring%", "%tutorial%", "%deprecated%");
	}

	/**
	 * Convert Query to SQL WHERE clause for content-only search
	 */
	private String convertToContentSearchSql(Query query) {
		if (query.isEmpty()) {
			return "1=1";
		}

		return query.accept(new ContentSearchVisitor());
	}

	/**
	 * Convert Query to parameterized SQL for content search
	 */
	private SqlParameterizedResult convertToParameterizedContentSql(Query query) {
		ParameterizedContentSearchVisitor visitor = new ParameterizedContentSearchVisitor();
		String sql = query.accept(visitor);
		return new SqlParameterizedResult(sql, visitor.getParameters());
	}

	/**
	 * SQL generation visitor for content-only blog search
	 */
	private static class ContentSearchVisitor implements NodeVisitor<String> {

		@Override
		public String visitRoot(RootNode node) {
			if (node.children().isEmpty()) {
				return "1=1";
			}

			List<String> conditions = new ArrayList<>();
			List<String> exclusions = new ArrayList<>();

			for (Node child : node.children()) {
				String condition = child.accept(this);
				if (condition.startsWith("NOT ")) {
					exclusions.add(condition);
				}
				else if (!condition.isEmpty()) {
					conditions.add(condition);
				}
			}

			String result = "";
			if (!conditions.isEmpty()) {
				result = String.join(" AND ", conditions);
			}
			if (!exclusions.isEmpty()) {
				if (!result.isEmpty()) {
					result += " AND ";
				}
				result += String.join(" AND ", exclusions);
			}

			return result.isEmpty() ? "1=1" : result;
		}

		@Override
		public String visitAnd(AndNode node) {
			List<String> conditions = node.children()
				.stream()
				.map(child -> child.accept(this))
				.filter(condition -> !condition.isEmpty())
				.toList();

			if (conditions.isEmpty()) {
				return "";
			}
			if (conditions.size() == 1) {
				return conditions.get(0);
			}

			return "(" + String.join(" AND ", conditions) + ")";
		}

		@Override
		public String visitOr(OrNode node) {
			List<String> conditions = node.children()
				.stream()
				.map(child -> child.accept(this))
				.filter(condition -> !condition.isEmpty())
				.toList();

			if (conditions.isEmpty()) {
				return "";
			}
			if (conditions.size() == 1) {
				return conditions.get(0);
			}

			return "(" + String.join(" OR ", conditions) + ")";
		}

		@Override
		public String visitNot(NotNode node) {
			String condition = node.child().accept(this);
			if (condition.isEmpty()) {
				return "";
			}
			return "NOT " + condition;
		}

		@Override
		public String visitToken(TokenNode node) {
			switch (node.type()) {
				case KEYWORD:
					return createContentLikeCondition(node.value());
				case EXCLUDE:
					return "NOT " + createContentLikeCondition(node.value());
				default:
					return "";
			}
		}

		@Override
		public String visitPhrase(PhraseNode node) {
			return createContentLikeCondition(node.phrase());
		}

		// These should not be called in legacy mode, but provide defaults
		@Override
		public String visitField(FieldNode node) {
			return "";
		}

		@Override
		public String visitWildcard(WildcardNode node) {
			return "";
		}

		@Override
		public String visitFuzzy(FuzzyNode node) {
			return "";
		}

		@Override
		public String visitRange(RangeNode node) {
			return "";
		}

		/**
		 * Generate LIKE condition for content search
		 */
		private String createContentLikeCondition(String term) {
			String escapedTerm = escapeSql(term);
			return "content LIKE '%" + escapedTerm + "%'";
		}

		/**
		 * Simple SQL escaping for content search
		 */
		private String escapeSql(String value) {
			return value.replace("'", "''");
		}

	}

	/**
	 * Parameterized SQL generation visitor for content search
	 */
	private static class ParameterizedContentSearchVisitor implements NodeVisitor<String> {

		private final List<String> parameters = new ArrayList<>();

		public List<String> getParameters() {
			return parameters;
		}

		@Override
		public String visitRoot(RootNode node) {
			if (node.children().isEmpty()) {
				return "1=1";
			}

			return node.children()
				.stream()
				.map(child -> child.accept(this))
				.filter(condition -> !condition.isEmpty())
				.collect(Collectors.joining(" AND "));
		}

		@Override
		public String visitAnd(AndNode node) {
			List<String> conditions = node.children()
				.stream()
				.map(child -> child.accept(this))
				.filter(condition -> !condition.isEmpty())
				.toList();

			if (conditions.isEmpty()) {
				return "";
			}
			if (conditions.size() == 1) {
				return conditions.get(0);
			}

			return "(" + String.join(" AND ", conditions) + ")";
		}

		@Override
		public String visitOr(OrNode node) {
			List<String> conditions = node.children()
				.stream()
				.map(child -> child.accept(this))
				.filter(condition -> !condition.isEmpty())
				.toList();

			if (conditions.isEmpty()) {
				return "";
			}
			if (conditions.size() == 1) {
				return conditions.get(0);
			}

			return "(" + String.join(" OR ", conditions) + ")";
		}

		@Override
		public String visitNot(NotNode node) {
			String condition = node.child().accept(this);
			if (condition.isEmpty()) {
				return "";
			}
			return "NOT " + condition;
		}

		@Override
		public String visitToken(TokenNode node) {
			switch (node.type()) {
				case KEYWORD:
					return createParameterizedContentCondition(node.value());
				case EXCLUDE:
					return "NOT " + createParameterizedContentCondition(node.value());
				default:
					return "";
			}
		}

		@Override
		public String visitPhrase(PhraseNode node) {
			return createParameterizedContentCondition(node.phrase());
		}

		// These should not be called in legacy mode, but provide defaults
		@Override
		public String visitField(FieldNode node) {
			return "";
		}

		@Override
		public String visitWildcard(WildcardNode node) {
			return "";
		}

		@Override
		public String visitFuzzy(FuzzyNode node) {
			return "";
		}

		@Override
		public String visitRange(RangeNode node) {
			return "";
		}

		/**
		 * Generate parameterized LIKE condition for content search
		 */
		private String createParameterizedContentCondition(String term) {
			parameters.add("%" + term + "%");
			return "content LIKE ?";
		}

	}

	/**
	 * Result of parameterized SQL generation
	 */
	public record SqlParameterizedResult(String sql, List<String> parameters) {
	}

}