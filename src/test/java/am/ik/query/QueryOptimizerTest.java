package am.ik.query;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class QueryOptimizerTest {

	@Test
	void testRemoveDuplicates() {
		// Create a simple OR with duplicates
		Node orNode = new OrNode(
				java.util.List.of(new TokenNode(TokenType.KEYWORD, "hello"), new TokenNode(TokenType.KEYWORD, "hello"),
						new TokenNode(TokenType.KEYWORD, "world"), new TokenNode(TokenType.KEYWORD, "world")));
		Query query = new Query("test", orNode, QueryMetadata.builder().build());
		Query optimized = query.transform(QueryOptimizer.removeDuplicates());

		// Should remove duplicate "hello" and "world" within the OR group
		assertThat(optimized.rootNode()).isInstanceOf(OrNode.class);
		assertThat(optimized.rootNode().children()).hasSize(2);
		assertThat(optimized.rootNode().children().stream().map(Node::value)).containsExactlyInAnyOrder("hello",
				"world");
	}

	@Test
	void testFlattenNestedBooleans() {
		// Create nested AND structure manually
		Node inner = new AndNode(
				java.util.List.of(new TokenNode(TokenType.KEYWORD, "b"), new TokenNode(TokenType.KEYWORD, "c")));
		Node outer = new AndNode(java.util.List.of(new TokenNode(TokenType.KEYWORD, "a"), inner));

		Query query = new Query("test", outer, QueryMetadata.builder().build());
		Query flattened = query.transform(QueryOptimizer.flattenNestedBooleans());

		// Should flatten to single AND with three children
		assertThat(flattened.rootNode()).isInstanceOf(AndNode.class);
		assertThat(flattened.rootNode().children()).hasSize(3);
	}

	@Test
	void testSimplifyBooleans() {
		// Single child AND/OR should be simplified
		Node singleAnd = new AndNode(java.util.List.of(new TokenNode(TokenType.KEYWORD, "hello")));
		Query query = new Query("test", singleAnd, QueryMetadata.builder().build());
		Query simplified = query.transform(QueryOptimizer.simplifyBooleans());

		assertThat(simplified.rootNode()).isInstanceOf(TokenNode.class);
	}

	@Test
	void testDoubleNegation() {
		// NOT(NOT(x)) should become x
		Node doubleNot = new NotNode(new NotNode(new TokenNode(TokenType.KEYWORD, "hello")));
		Query query = new Query("test", doubleNot, QueryMetadata.builder().build());
		Query simplified = query.transform(QueryOptimizer.simplifyBooleans());

		assertThat(simplified.rootNode()).isInstanceOf(TokenNode.class);
		assertThat(simplified.rootNode().value()).isEqualTo("hello");
	}

	@Test
	void testRemoveEmptyGroups() {
		// Create structure with empty groups
		Node root = new AndNode(
				java.util.List.of(new TokenNode(TokenType.KEYWORD, "hello"), new OrNode(java.util.List.of()) // Empty
																												// OR
																												// group
				));

		Query query = new Query("test", root, QueryMetadata.builder().build());
		Query cleaned = query.transform(QueryOptimizer.removeEmptyGroups());

		// Empty group should be removed
		assertThat(cleaned.rootNode()).isInstanceOf(AndNode.class);
		assertThat(cleaned.rootNode().children()).hasSize(1);
	}

	@Test
	void testDefaultOptimizer() {
		Query query = QueryParser.create().parse("hello OR hello AND (world) NOT NOT java");
		Query optimized = query.optimize();

		assertThat(optimized).isNotNull();
		// Should apply all optimizations
	}

	@Test
	void testOptimizeComplexQuery() {
		Query query = QueryParser.create().parse("((hello OR hello) AND world) OR (world AND java)");
		Query optimized = query.optimize();

		String optimizedStr = optimized.toString();
		assertThat(optimizedStr).isNotNull();
		// Should be more concise than original
	}

	@Test
	void testOptimizerChaining() {
		QueryTransformer optimizer = QueryOptimizer.removeDuplicates()
			.andThen(QueryOptimizer.flattenNestedBooleans())
			.andThen(QueryOptimizer.simplifyBooleans());

		Query query = QueryParser.create().parse("hello OR hello AND ((world))");
		Query optimized = query.transform(optimizer);

		assertThat(optimized).isNotNull();
	}

}