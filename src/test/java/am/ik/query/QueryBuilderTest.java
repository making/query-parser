package am.ik.query;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class QueryBuilderTest {

	@Test
	void testSimpleKeyword() {
		Query query = new Query.Builder().keyword("hello").build();

		assertThat(query.extractKeywords()).containsExactly("hello");
	}

	@Test
	void testMultipleKeywords() {
		Query query = new Query.Builder().keywords("hello", "world").build();

		assertThat(query.extractKeywords()).containsExactlyInAnyOrder("hello", "world");
	}

	@Test
	void testPhrase() {
		Query query = new Query.Builder().phrase("hello world").build();

		assertThat(query.extractPhrases()).containsExactly("hello world");
	}

	@Test
	void testField() {
		Query query = new Query.Builder().field("title", "hello").build();

		assertThat(QueryUtils.findNodes(query, FieldNode.class)).hasSize(1).first().satisfies(node -> {
			assertThat(node.field()).isEqualTo("title");
			assertThat(node.fieldValue()).isEqualTo("hello");
		});
	}

	@Test
	void testWildcard() {
		Query query = new Query.Builder().wildcard("hel*").build();

		assertThat(QueryUtils.findNodes(query, WildcardNode.class)).hasSize(1)
			.first()
			.satisfies(node -> assertThat(node.pattern()).isEqualTo("hel*"));
	}

	@Test
	void testFuzzy() {
		Query query = new Query.Builder().fuzzy("hello", 2).build();

		assertThat(QueryUtils.findNodes(query, FuzzyNode.class)).hasSize(1).first().satisfies(node -> {
			assertThat(node.term()).isEqualTo("hello");
			assertThat(node.maxEdits()).isEqualTo(2);
		});
	}

	@Test
	void testRange() {
		Query query = new Query.Builder().range("1", "10").build();

		assertThat(QueryUtils.findNodes(query, RangeNode.class)).hasSize(1).first().satisfies(node -> {
			assertThat(node.start()).isEqualTo("1");
			assertThat(node.end()).isEqualTo("10");
			assertThat(node.includeStart()).isTrue();
			assertThat(node.includeEnd()).isTrue();
		});
	}

	@Test
	void testExclude() {
		Query query = new Query.Builder().keyword("hello").exclude("world").build();

		assertThat(query.extractKeywords()).containsExactly("hello");
		assertThat(query.hasExclusions()).isTrue();
	}

	@Test
	void testAndGroup() {
		Query query = new Query.Builder().and().keyword("hello").keyword("world").endGroup().build();

		assertThat(QueryUtils.countNodesOfType(query, AndNode.class)).isEqualTo(1);
		assertThat(query.extractKeywords()).containsExactlyInAnyOrder("hello", "world");
	}

	@Test
	void testOrGroup() {
		Query query = new Query.Builder().or().keyword("hello").keyword("world").endGroup().build();

		assertThat(QueryUtils.countNodesOfType(query, OrNode.class)).isEqualTo(1);
		assertThat(query.extractKeywords()).containsExactlyInAnyOrder("hello", "world");
	}

	@Test
	void testNotGroup() {
		Query query = new Query.Builder().not().keyword("hello").endGroup().build();

		assertThat(QueryUtils.countNodesOfType(query, NotNode.class)).isEqualTo(1);
	}

	@Test
	void testComplexQuery() {
		Query query = new Query.Builder().or()
			.keyword("java")
			.keyword("python")
			.endGroup()
			.and()
			.phrase("programming language")
			.field("category", "tech")
			.endGroup()
			.exclude("basic")
			.build();

		assertThat(query.extractKeywords()).containsExactlyInAnyOrder("java", "python");
		assertThat(query.extractPhrases()).containsExactly("programming language");
		assertThat(query.hasExclusions()).isTrue();
		assertThat(QueryUtils.countNodesOfType(query, FieldNode.class)).isEqualTo(1);
	}

	@Test
	void testEmptyQuery() {
		Query query = new Query.Builder().build();

		assertThat(query.isEmpty()).isTrue();
	}

}