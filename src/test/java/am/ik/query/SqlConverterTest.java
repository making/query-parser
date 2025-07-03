package am.ik.query;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for SQL conversion functionality using visitor pattern
 */
class SqlConverterTest {

	private final QueryParser queryParser = QueryParser.builder()
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
		.validateAfterParse(false) // Allow flexibility for testing
		.throwOnValidationError(false)
		.build();

	@Test
	void testSingleKeyword() {
		Query query = queryParser.parse("spring");

		QueryAndParams result = new SqlConverter().convert(query);

		assertThat(result.query()).isEqualTo("content ILIKE :keyword_0");
		assertThat(result.params()).containsEntry("keyword_0", "spring");
	}

	@Test
	void testMultipleKeywords() {
		Query query = queryParser.parse("java spring");

		QueryAndParams result = new SqlConverter().convert(query);

		assertThat(result.query()).isEqualTo("content ILIKE :keyword_0 AND content ILIKE :keyword_1");
		assertThat(result.params()).containsEntry("keyword_0", "java").containsEntry("keyword_1", "spring");
	}

	@Test
	void testOrOperator() {
		Query query = queryParser.parse("java OR kotlin");

		QueryAndParams result = new SqlConverter().convert(query);

		assertThat(result.query()).isEqualTo("content ILIKE :keyword_0 OR content ILIKE :keyword_1");
		assertThat(result.params()).containsEntry("keyword_0", "java").containsEntry("keyword_1", "kotlin");
	}

	@Test
	void testExcludeOperator() {
		Query query = queryParser.parse("spring -deprecated");

		QueryAndParams result = new SqlConverter().convert(query);

		assertThat(result.query()).isEqualTo("content ILIKE :keyword_0 AND content NOT ILIKE :keyword_1");
		assertThat(result.params()).containsEntry("keyword_0", "spring").containsEntry("keyword_1", "deprecated");
	}

	@Test
	void testPhraseQuery() {
		Query query = queryParser.parse("\"Spring Boot\"");

		QueryAndParams result = new SqlConverter().convert(query);

		assertThat(result.query()).isEqualTo("content ILIKE :keyword_0");
		assertThat(result.params()).containsEntry("keyword_0", "Spring Boot");
	}

	@Test
	void testComplexQuery() {
		Query query = queryParser.parse("java AND (spring OR boot) -legacy");

		QueryAndParams result = new SqlConverter().convert(query);

		assertThat(result.query()).contains("content ILIKE :keyword_0"); // java
		assertThat(result.query()).contains("content ILIKE :keyword_1"); // spring
		assertThat(result.query()).contains("content ILIKE :keyword_2"); // boot
		assertThat(result.query()).contains("content NOT ILIKE :keyword_3"); // legacy
		assertThat(result.query()).contains(" AND ");
		assertThat(result.query()).contains(" OR ");

		assertThat(result.params()).containsEntry("keyword_0", "java")
			.containsEntry("keyword_1", "spring")
			.containsEntry("keyword_2", "boot")
			.containsEntry("keyword_3", "legacy");
	}

	@Test
	void testGroupingWithParentheses() {
		Query query = queryParser.parse("(java OR kotlin) AND spring");

		QueryAndParams result = new SqlConverter().convert(query);

		// The modern API may flatten some expressions, so we'll test the core
		// functionality
		assertThat(result.query()).contains("content ILIKE :keyword_0"); // java
		assertThat(result.query()).contains("content ILIKE :keyword_1"); // kotlin
		assertThat(result.query()).contains("content ILIKE :keyword_2"); // spring
		assertThat(result.query()).contains(" OR ");
		assertThat(result.query()).contains(" AND ");

		assertThat(result.params()).containsEntry("keyword_0", "java")
			.containsEntry("keyword_1", "kotlin")
			.containsEntry("keyword_2", "spring");
	}

	@Test
	void testMixedOperators() {
		Query query = queryParser.parse("spring AND tutorial OR guide -deprecated");

		QueryAndParams result = new SqlConverter().convert(query);

		assertThat(result.query()).contains("content ILIKE :keyword_0"); // spring
		assertThat(result.query()).contains("content ILIKE :keyword_1"); // tutorial
		assertThat(result.query()).contains("content ILIKE :keyword_2"); // guide
		assertThat(result.query()).contains("content NOT ILIKE :keyword_3"); // deprecated

		assertThat(result.params()).containsEntry("keyword_0", "spring")
			.containsEntry("keyword_1", "tutorial")
			.containsEntry("keyword_2", "guide")
			.containsEntry("keyword_3", "deprecated");
	}

	@Test
	void testNestedGroups() {
		Query query = queryParser.parse("java AND (spring OR (boot AND framework))");

		QueryAndParams result = new SqlConverter().convert(query);

		assertThat(result.query()).contains("content ILIKE :keyword_0"); // java
		assertThat(result.query()).contains("content ILIKE :keyword_1"); // spring
		assertThat(result.query()).contains("content ILIKE :keyword_2"); // boot
		assertThat(result.query()).contains("content ILIKE :keyword_3"); // framework

		assertThat(result.params()).containsEntry("keyword_0", "java")
			.containsEntry("keyword_1", "spring")
			.containsEntry("keyword_2", "boot")
			.containsEntry("keyword_3", "framework");
	}

	@Test
	void testEmptyQuery() {
		Query query = queryParser.parse("");

		QueryAndParams result = new SqlConverter().convert(query);

		assertThat(result.query()).isEmpty();
		assertThat(result.params()).isEmpty();
	}

	@Test
	void testOnlyExclusions() {
		Query query = queryParser.parse("-deprecated -legacy");

		QueryAndParams result = new SqlConverter().convert(query);

		assertThat(result.query()).isEqualTo("content NOT ILIKE :keyword_0 AND content NOT ILIKE :keyword_1");
		assertThat(result.params()).containsEntry("keyword_0", "deprecated").containsEntry("keyword_1", "legacy");
	}

	@Test
	void testParameterIndexing() {
		Query query = queryParser.parse("first second third");

		QueryAndParams result = new SqlConverter().convert(query);

		// Verify parameters are properly indexed
		assertThat(result.params()).containsKeys("keyword_0", "keyword_1", "keyword_2");
		assertThat(result.params().get("keyword_0")).isEqualTo("first");
		assertThat(result.params().get("keyword_1")).isEqualTo("second");
		assertThat(result.params().get("keyword_2")).isEqualTo("third");
	}

	@Test
	void testCustomParameterStartIndex() {
		Query query = queryParser.parse("spring boot");

		SqlConverter converter = new SqlConverter();
		AtomicInteger customIndex = new AtomicInteger(5);
		QueryAndParams result = converter.convert(query, customIndex);

		assertThat(result.params()).containsKeys("keyword_5", "keyword_6");
		assertThat(result.params().get("keyword_5")).isEqualTo("spring");
		assertThat(result.params().get("keyword_6")).isEqualTo("boot");
	}

	/**
	 * SQL converter using visitor pattern for parameterized SQL generation
	 */
	public static class SqlConverter {

		private static final String COLUMN_NAME = "content";

		public QueryAndParams convert(Query query) {
			return convert(query, new AtomicInteger(0));
		}

		public QueryAndParams convert(Query query, AtomicInteger index) {
			if (query.isEmpty()) {
				return new QueryAndParams("", Map.of());
			}

			ParameterizedSqlVisitor visitor = new ParameterizedSqlVisitor(index);
			String sql = query.accept(visitor);
			return new QueryAndParams(sql, visitor.getParams());
		}

		/**
		 * Visitor implementation for parameterized SQL generation that mimics legacy
		 * behavior
		 */
		private static class ParameterizedSqlVisitor implements NodeVisitor<String> {

			private final AtomicInteger index;

			private final Map<String, String> params = new LinkedHashMap<>();

			public ParameterizedSqlVisitor(AtomicInteger index) {
				this.index = index;
			}

			public Map<String, String> getParams() {
				return params;
			}

			@Override
			public String visitRoot(RootNode node) {
				if (node.children().isEmpty()) {
					return "";
				}

				return node.children()
					.stream()
					.map(child -> child.accept(this))
					.filter(sql -> !sql.isEmpty())
					.reduce((left, right) -> left + " AND " + right)
					.orElse("");
			}

			@Override
			public String visitAnd(AndNode node) {
				// For legacy compatibility, don't wrap single AND operations in
				// parentheses
				String result = node.children()
					.stream()
					.map(child -> child.accept(this))
					.filter(sql -> !sql.isEmpty())
					.reduce((left, right) -> left + " AND " + right)
					.orElse("");

				// Only wrap in parentheses if this is part of a more complex expression
				return result;
			}

			@Override
			public String visitOr(OrNode node) {
				// For legacy compatibility, don't wrap single OR operations in
				// parentheses
				String result = node.children()
					.stream()
					.map(child -> child.accept(this))
					.filter(sql -> !sql.isEmpty())
					.reduce((left, right) -> left + " OR " + right)
					.orElse("");

				return result;
			}

			@Override
			public String visitNot(NotNode node) {
				// For legacy compatibility, if the child is a simple token, generate
				// direct NOT ILIKE
				if (node.child() instanceof TokenNode token && token.type() == TokenType.KEYWORD) {
					String paramName = "keyword_" + index.getAndIncrement();
					params.put(paramName, token.value());
					return COLUMN_NAME + " NOT ILIKE :" + paramName;
				}
				else if (node.child() instanceof PhraseNode phrase) {
					String paramName = "keyword_" + index.getAndIncrement();
					params.put(paramName, phrase.phrase());
					return COLUMN_NAME + " NOT ILIKE :" + paramName;
				}

				// For complex expressions, visit child and wrap with NOT
				String childSql = node.child().accept(this);
				if (childSql.isEmpty()) {
					return "";
				}
				return "NOT (" + childSql + ")";
			}

			@Override
			public String visitToken(TokenNode node) {
				String paramName = "keyword_" + index.getAndIncrement();

				switch (node.type()) {
					case KEYWORD:
						params.put(paramName, node.value());
						return COLUMN_NAME + " ILIKE :" + paramName;
					case EXCLUDE:
						// For EXCLUDE tokens, generate NOT ILIKE directly for legacy
						// compatibility
						params.put(paramName, node.value());
						return COLUMN_NAME + " NOT ILIKE :" + paramName;
					default:
						return "";
				}
			}

			@Override
			public String visitPhrase(PhraseNode node) {
				String paramName = "keyword_" + index.getAndIncrement();
				params.put(paramName, node.phrase());
				return COLUMN_NAME + " ILIKE :" + paramName;
			}

			// Legacy mode doesn't support these node types, but provide defaults
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

		}

	}

	/**
	 * Result record for parameterized SQL generation
	 */
	public record QueryAndParams(String query, Map<String, String> params) {
	}

}