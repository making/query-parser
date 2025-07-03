package am.ik.query;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Example of converting QueryParser to SQL WHERE clause for blog article search
 */
class BlogSearchSqlConverterTest {

	@Test
	void testSimpleKeywordSearch() {
		Query query = QueryParser.create().parse("spring");

		String sql = convertToSqlWhere(query);

		assertThat(sql).isEqualTo("(title LIKE '%spring%' OR content LIKE '%spring%' OR tags LIKE '%spring%')");
	}

	@Test
	void testMultipleKeywords() {
		Query query = QueryParser.create().parse("spring boot");

		String sql = convertToSqlWhere(query);

		assertThat(sql).isEqualTo("((title LIKE '%spring%' OR content LIKE '%spring%' OR tags LIKE '%spring%') "
				+ "AND (title LIKE '%boot%' OR content LIKE '%boot%' OR tags LIKE '%boot%'))");
	}

	@Test
	void testExplicitAndOrOperators() {
		Query query = QueryParser.create().parse("java AND spring OR kotlin");

		String sql = convertToSqlWhere(query);

		assertThat(sql).contains("(title LIKE '%java%' OR content LIKE '%java%' OR tags LIKE '%java%')");
		assertThat(sql).contains("(title LIKE '%spring%' OR content LIKE '%spring%' OR tags LIKE '%spring%')");
		assertThat(sql).contains("(title LIKE '%kotlin%' OR content LIKE '%kotlin%' OR tags LIKE '%kotlin%')");
		assertThat(sql).contains(" AND ");
		assertThat(sql).contains(" OR ");
	}

	@Test
	void testPhraseSearch() {
		Query query = QueryParser.create().parse("\"Spring Boot Framework\"");

		String sql = convertToSqlWhere(query);

		assertThat(sql).isEqualTo(
				"(title LIKE '%Spring Boot Framework%' OR content LIKE '%Spring Boot Framework%' OR tags LIKE '%Spring Boot Framework%')");
	}

	@Test
	void testFieldSpecificSearch() {
		Query query = QueryParser.create().parse("title:spring author:john");

		String sql = convertToSqlWhere(query);

		assertThat(sql).isEqualTo("((title LIKE '%spring%') AND (author = 'john'))");
	}

	@Test
	void testFieldWithPhrase() {
		Query query = QueryParser.create().parse("title:\"Spring Boot Tutorial\"");

		String sql = convertToSqlWhere(query);

		assertThat(sql).isEqualTo("(title LIKE '%Spring Boot Tutorial%')");
	}

	@Test
	void testExclusions() {
		Query query = QueryParser.create().parse("spring -deprecated");

		String sql = convertToSqlWhere(query);

		assertThat(sql).isEqualTo("((title LIKE '%spring%' OR content LIKE '%spring%' OR tags LIKE '%spring%') "
				+ "AND NOT (title LIKE '%deprecated%' OR content LIKE '%deprecated%' OR tags LIKE '%deprecated%'))");
	}

	@Test
	void testWildcardSearch() {
		Query query = QueryParser.create().parse("spring*");

		String sql = convertToSqlWhere(query);

		// Convert wildcard to SQL LIKE pattern
		assertThat(sql).isEqualTo("(title LIKE '%spring%' OR content LIKE '%spring%' OR tags LIKE '%spring%')");
	}

	@Test
	void testDateRangeSearch() {
		// Note: Range queries with field prefixes are not fully supported in the current
		// parser
		// This test demonstrates a simple date search instead
		Query query = QueryParser.create().parse("created_date:2024");

		String sql = convertToSqlWhere(query);

		assertThat(sql).isEqualTo("(created_date = '2024')");
	}

	@Test
	void testCategorySearch() {
		Query query = QueryParser.create().parse("category:programming status:published");

		String sql = convertToSqlWhere(query);

		assertThat(sql).isEqualTo("((category = 'programming') AND (status = 'published'))");
	}

	@Test
	void testComplexBlogSearch() {
		Query query = QueryParser.builder()
			.build()
			.parse("(java OR kotlin) AND title:\"tutorial\" AND category:programming AND -deprecated");

		String sql = convertToSqlWhere(query);

		assertThat(sql).contains("(title LIKE '%java%' OR content LIKE '%java%' OR tags LIKE '%java%')");
		assertThat(sql).contains("(title LIKE '%kotlin%' OR content LIKE '%kotlin%' OR tags LIKE '%kotlin%')");
		assertThat(sql).contains("(title LIKE '%tutorial%')");
		assertThat(sql).contains("(category = 'programming')");
		assertThat(sql)
			.contains("NOT (title LIKE '%deprecated%' OR content LIKE '%deprecated%' OR tags LIKE '%deprecated%')");
	}

	@Test
	void testEmptyQuery() {
		Query query = QueryParser.create().parse("");

		String sql = convertToSqlWhere(query);

		assertThat(sql).isEqualTo("1=1"); // Condition for retrieving all records
	}

	@Test
	void testOnlyExclusions() {
		Query query = QueryParser.create().parse("-deprecated -draft");

		String sql = convertToSqlWhere(query);

		assertThat(sql)
			.isEqualTo("(NOT (title LIKE '%deprecated%' OR content LIKE '%deprecated%' OR tags LIKE '%deprecated%') "
					+ "AND NOT (title LIKE '%draft%' OR content LIKE '%draft%' OR tags LIKE '%draft%'))");
	}

	/**
	 * Method to convert Query to SQL WHERE clause
	 */
	private String convertToSqlWhere(Query query) {
		if (query.isEmpty()) {
			return "1=1";
		}

		return query.accept(new SqlWhereVisitor());
	}

	/**
	 * SQL WHERE clause generation Visitor for blog article search
	 */
	private static class SqlWhereVisitor implements NodeVisitor<String> {

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
					return createFullTextSearchCondition(node.value());
				case EXCLUDE:
					return "NOT " + createFullTextSearchCondition(node.value());
				default:
					return "";
			}
		}

		@Override
		public String visitField(FieldNode node) {
			String field = node.field();
			String value = node.fieldValue();

			// Handle specific fields
			switch (field.toLowerCase()) {
				case "category":
				case "status":
				case "author":
					return "(" + field + " = '" + escapeSql(value) + "')";
				case "title":
				case "content":
				case "tags":
					return "(" + field + " LIKE '%" + escapeSql(value) + "%')";
				case "created_date":
				case "updated_date":
				case "published_date":
					return "(" + field + " = '" + escapeSql(value) + "')";
				default:
					// Unknown fields use LIKE search
					return "(" + field + " LIKE '%" + escapeSql(value) + "%')";
			}
		}

		@Override
		public String visitPhrase(PhraseNode node) {
			return createFullTextSearchCondition(node.phrase());
		}

		@Override
		public String visitWildcard(WildcardNode node) {
			// Convert wildcard to simple LIKE search
			String pattern = node.pattern().replace("*", "").replace("?", "");
			return createFullTextSearchCondition(pattern);
		}

		@Override
		public String visitFuzzy(FuzzyNode node) {
			// Convert fuzzy search to regular LIKE search
			return createFullTextSearchCondition(node.term());
		}

		@Override
		public String visitRange(RangeNode node) {
			String field = node.field();
			if (field == null) {
				// Skip if field is not specified
				return "";
			}

			String start = node.start();
			String end = node.end();
			boolean includeStart = node.includeStart();
			boolean includeEnd = node.includeEnd();

			String startOp = includeStart ? ">=" : ">";
			String endOp = includeEnd ? "<=" : "<";

			return "(" + field + " " + startOp + " '" + escapeSql(start) + "' AND " + field + " " + endOp + " '"
					+ escapeSql(end) + "')";
		}

		/**
		 * Generate full-text search condition (for title, content, tags)
		 */
		private String createFullTextSearchCondition(String term) {
			String escapedTerm = escapeSql(term);
			return "(title LIKE '%" + escapedTerm + "%' OR content LIKE '%" + escapedTerm + "%' OR tags LIKE '%"
					+ escapedTerm + "%')";
		}

		/**
		 * Simple escaping for SQL injection prevention
		 */
		private String escapeSql(String value) {
			return value.replace("'", "''");
		}

	}

	/**
	 * Example of SQL generation for parameterized queries
	 */
	@Test
	void testParameterizedQuery() {
		Query query = QueryParser.create().parse("title:spring author:john");

		SqlParameterizedResult result = convertToParameterizedSql(query);

		assertThat(result.sql()).isEqualTo("((title LIKE ?) AND (author = ?))");
		assertThat(result.parameters()).containsExactly("%spring%", "john");
	}

	/**
	 * Result of parameterized SQL
	 */
	public record SqlParameterizedResult(String sql, List<String> parameters) {
	}

	/**
	 * Convert to parameterized SQL
	 */
	private SqlParameterizedResult convertToParameterizedSql(Query query) {
		ParameterizedSqlVisitor visitor = new ParameterizedSqlVisitor();
		String sql = query.accept(visitor);
		return new SqlParameterizedResult(sql, visitor.getParameters());
	}

	/**
	 * Parameterized SQL generation Visitor
	 */
	private static class ParameterizedSqlVisitor implements NodeVisitor<String> {

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
					return createParameterizedFullTextSearch(node.value());
				case EXCLUDE:
					return "NOT " + createParameterizedFullTextSearch(node.value());
				default:
					return "";
			}
		}

		@Override
		public String visitField(FieldNode node) {
			String field = node.field();
			String value = node.fieldValue();

			switch (field.toLowerCase()) {
				case "category":
				case "status":
				case "author":
					parameters.add(value);
					return "(" + field + " = ?)";
				case "title":
				case "content":
				case "tags":
					parameters.add("%" + value + "%");
					return "(" + field + " LIKE ?)";
				default:
					parameters.add("%" + value + "%");
					return "(" + field + " LIKE ?)";
			}
		}

		@Override
		public String visitPhrase(PhraseNode node) {
			return createParameterizedFullTextSearch(node.phrase());
		}

		@Override
		public String visitWildcard(WildcardNode node) {
			String pattern = node.pattern().replace("*", "").replace("?", "");
			return createParameterizedFullTextSearch(pattern);
		}

		@Override
		public String visitFuzzy(FuzzyNode node) {
			return createParameterizedFullTextSearch(node.term());
		}

		@Override
		public String visitRange(RangeNode node) {
			String field = node.field();
			if (field == null) {
				return "";
			}

			String start = node.start();
			String end = node.end();
			boolean includeStart = node.includeStart();
			boolean includeEnd = node.includeEnd();

			String startOp = includeStart ? ">=" : ">";
			String endOp = includeEnd ? "<=" : "<";

			parameters.add(start);
			parameters.add(end);

			return "(" + field + " " + startOp + " ? AND " + field + " " + endOp + " ?)";
		}

		private String createParameterizedFullTextSearch(String term) {
			String param = "%" + term + "%";
			parameters.add(param);
			parameters.add(param);
			parameters.add(param);
			return "(title LIKE ? OR content LIKE ? OR tags LIKE ?)";
		}

	}

}