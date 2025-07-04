package am.ik.query;

import am.ik.query.ast.AndNode;
import am.ik.query.ast.FieldNode;
import am.ik.query.ast.FuzzyNode;
import am.ik.query.ast.Node;
import am.ik.query.ast.OrNode;
import am.ik.query.ast.RangeNode;
import am.ik.query.ast.RootNode;
import am.ik.query.ast.TokenNode;
import am.ik.query.ast.WildcardNode;
import am.ik.query.parser.QueryParser;
import am.ik.query.transform.QueryNormalizer;
import am.ik.query.transform.QueryOptimizer;
import am.ik.query.util.QueryPrinter;
import am.ik.query.validation.QueryValidator;
import am.ik.query.validation.ValidationResult;
import am.ik.query.ast.BaseNodeVisitor;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class QueryTest {

	@Test
	void testSimpleQuery() {
		Query query = QueryParser.create().parse("hello world");

		assertThat(query.originalQuery()).isEqualTo("hello world");
		assertThat(query.isEmpty()).isFalse();
		assertThat(query.extractKeywords()).containsExactly("hello", "world");
		assertThat(query.extractPhrases()).isEmpty();
		assertThat(query.extractExclusions()).isEmpty();
		assertThat(query.countNodes(OrNode.class)).isEqualTo(0);
		assertThat(query.countNodes(AndNode.class)).isGreaterThan(0); // Now true because
																		// AndNode exists
	}

	@Test
	void testPhraseQuery() {
		Query query = QueryParser.create().parse("\"hello world\"");

		assertThat(query.isEmpty()).isFalse();
		assertThat(query.extractPhrases()).containsExactly("hello world");
		assertThat(query.extractPhrases()).isNotEmpty();
	}

	@Test
	void testExclusionQuery() {
		Query query = QueryParser.create().parse("hello -world");

		assertThat(query.extractKeywords()).containsExactly("hello");
		assertThat(query.extractExclusions()).containsExactly("world");
		assertThat(query.extractExclusions()).isNotEmpty();
	}

	@Test
	void testOrQuery() {
		Query query = QueryParser.create().parse("hello OR world");

		assertThat(query.countNodes(OrNode.class)).isGreaterThan(0);
		assertThat(query.extractKeywords()).containsExactly("hello", "world");
	}

	@Test
	void testAndQuery() {
		Query query = QueryParser.create().parse("hello AND world");

		assertThat(query.countNodes(AndNode.class)).isGreaterThan(0);
		assertThat(query.extractKeywords()).containsExactly("hello", "world");
	}

	@Test
	void testComplexQuery() {
		Query query = QueryParser.create().parse("(hello OR world) AND \"foo bar\" -baz");

		assertThat(query.countNodes(OrNode.class)).isGreaterThan(0);
		assertThat(query.countNodes(AndNode.class)).isGreaterThan(0);
		assertThat(query.extractPhrases()).isNotEmpty();
		assertThat(query.extractExclusions()).isNotEmpty();

		assertThat(query.extractKeywords()).containsExactly("hello", "world");
		assertThat(query.extractPhrases()).containsExactly("foo bar");
		assertThat(query.extractExclusions()).containsExactly("baz");
	}

	@Test
	void testOptimization() {
		Query query = QueryParser.create().parse("hello OR hello AND world");
		Query optimized = query.transform(QueryOptimizer.defaultOptimizer());

		assertThat(optimized).isNotNull();
		// The optimizer should remove duplicate "hello"
	}

	@Test
	void testNormalization() {
		Query query = QueryParser.create().parse("HELLO   world");
		Query normalized = query.transform(QueryNormalizer.defaultNormalizer());

		assertThat(normalized).isNotNull();
		// The normalizer should lowercase and normalize whitespace
		assertThat(normalized.toString()).doesNotContain("HELLO");
	}

	@Test
	void testTransformation() {
		Query query = QueryParser.create().parse("hello world");
		Query transformed = query.transform(q -> {
			Node newRoot = new RootNode();
			return new Query(q.originalQuery(), newRoot);
		});

		assertThat(transformed.isEmpty()).isTrue();
	}

	@Test
	void testValidation() {
		Query query = QueryParser.create().parse("hello world");
		ValidationResult result = QueryValidator.validate(query);

		assertThat(result.isValid()).isTrue();
		assertThat(result.errors()).isEmpty();
	}

	@Test
	void testInvalidQuery() {
		Query query = QueryParser.create().parse(""); // Empty query
		ValidationResult result = QueryValidator.validate(query);

		assertThat(result.isValid()).isFalse();
		assertThat(result.errors()).isNotEmpty();
	}

	@Test
	void testWalk() {
		Query query = QueryParser.create().parse("hello world");
		int[] count = { 0 };

		query.walk(node -> count[0]++);

		assertThat(count[0]).isGreaterThan(0);
	}

	@Test
	void testAcceptVisitor() {
		Query query = QueryParser.create().parse("hello");

		String result = query.accept(new BaseNodeVisitor<String>() {
			@Override
			protected String defaultValue() {
				return "default";
			}

			@Override
			public String visitToken(TokenNode node) {
				return "token:" + node.value();
			}

			@Override
			public String visitRoot(RootNode node) {
				return "root";
			}
		});

		assertThat(result).isNotNull();
	}

	@Test
	void testToString() {
		Query query = QueryParser.create().parse("hello AND world");
		String str = query.toString();

		assertThat(str).contains("hello");
		assertThat(str).contains("AND");
		assertThat(str).contains("world");
	}

	@Test
	void testPrettyPrint() {
		Query query = QueryParser.create().parse("hello world");
		String pretty = QueryPrinter.toPrettyString(query);

		assertThat(pretty).contains("Query:");
		assertThat(pretty).contains("AST:");
		// Metadata was removed, so no longer checking for it
	}

	@Test
	void testFieldQuery() {
		Query query = QueryParser.create().parse("title:hello");

		assertThat(query.isEmpty()).isFalse();
		assertThat(query.countNodes(FieldNode.class)).isEqualTo(1);
	}

	@Test
	void testWildcardQuery() {
		Query query = QueryParser.create().parse("hel*");

		assertThat(query.isEmpty()).isFalse();
		assertThat(query.countNodes(WildcardNode.class)).isEqualTo(1);
	}

	@Test
	void testFuzzyQuery() {
		Query query = QueryParser.create().parse("hello~2");

		assertThat(query.isEmpty()).isFalse();
		assertThat(query.countNodes(FuzzyNode.class)).isEqualTo(1);
	}

	@Test
	void testRangeQuery() {
		Query query = QueryParser.create().parse("[1 TO 10]");

		assertThat(query.isEmpty()).isFalse();
		assertThat(query.countNodes(RangeNode.class)).isEqualTo(1);
	}

	@Test
	void testNullQuery() {
		assertThatThrownBy(() -> QueryParser.create().parse(null)).isInstanceOf(IllegalArgumentException.class);
	}

}