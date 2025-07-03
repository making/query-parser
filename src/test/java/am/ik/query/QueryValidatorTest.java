package am.ik.query;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class QueryValidatorTest {

	@Test
	void testValidQuery() {
		Query query = Query.parse("hello world");
		ValidationResult result = query.validate();

		assertThat(result.isValid()).isTrue();
		assertThat(result.errors()).isEmpty();
	}

	@Test
	void testEmptyQuery() {
		Query query = Query.parse("");
		ValidationResult result = query.validate();

		assertThat(result.isValid()).isFalse();
		// An empty query has both "Query is empty" and "Empty group node: RootNode"
		// errors
		assertThat(result.errors()).hasSize(2);
		assertThat(result.errors()).anySatisfy(error -> assertThat(error.message()).contains("Query is empty"));
		assertThat(result.errors()).anySatisfy(error -> assertThat(error.message()).contains("Empty group node"));
	}

	@Test
	void testEmptyGroup() {
		// Manually create empty group
		Node emptyAnd = new AndNode(java.util.List.of());
		Query query = new Query("test", emptyAnd, QueryMetadata.builder().build());
		ValidationResult result = query.validate();

		assertThat(result.isValid()).isFalse();
		assertThat(result.errors()).anySatisfy(error -> assertThat(error.message()).contains("Empty group"));
	}

	@Test
	void testConflictingTerms() {
		Query query = Query.parse("hello AND -hello");
		ValidationResult result = query.validate();

		assertThat(result.isValid()).isFalse();
		assertThat(result.errors()).anySatisfy(error -> assertThat(error.message()).contains("conflicting"));
	}

	@Test
	void testAllNegativeOr() {
		Query query = Query.parse("-hello OR -world");
		ValidationResult result = query.validate();

		assertThat(result.isValid()).isFalse();
		assertThat(result.errors()).anySatisfy(error -> assertThat(error.message()).contains("only negative"));
	}

	@Test
	void testEmptyFieldName() {
		FieldNode field = new FieldNode("", "value");
		Query query = new Query("test", field, QueryMetadata.builder().build());
		ValidationResult result = query.validate();

		assertThat(result.isValid()).isFalse();
		assertThat(result.errors()).anySatisfy(error -> assertThat(error.message()).contains("Empty field name"));
	}

	@Test
	void testEmptyFieldValue() {
		FieldNode field = new FieldNode("title", "");
		Query query = new Query("test", field, QueryMetadata.builder().build());
		ValidationResult result = query.validate();

		assertThat(result.isValid()).isFalse();
		assertThat(result.errors()).anySatisfy(error -> assertThat(error.message()).contains("Empty field value"));
	}

	@Test
	void testShortFuzzyTerm() {
		Query query = Query.parse("ab~2");
		ValidationResult result = query.validate();

		assertThat(result.isValid()).isFalse();
		assertThat(result.errors()).anySatisfy(error -> assertThat(error.message()).contains("too short"));
	}

	@Test
	void testSameRangeBoundaries() {
		RangeNode range = RangeNode.builder().start("5").end("5").build();
		Query query = new Query("test", range, QueryMetadata.builder().build());
		ValidationResult result = query.validate();

		assertThat(result.isValid()).isFalse();
		assertThat(result.errors()).anySatisfy(error -> assertThat(error.message()).contains("same"));
	}

	@Test
	void testWildcardRange() {
		RangeNode range = RangeNode.builder().start("*").end("*").build();
		Query query = new Query("test", range, QueryMetadata.builder().build());
		ValidationResult result = query.validate();

		assertThat(result.isValid()).isFalse();
		assertThat(result.errors()).anySatisfy(error -> assertThat(error.message()).contains("matches everything"));
	}

	@Test
	void testThrowIfInvalid() {
		Query query = Query.parse("");
		ValidationResult result = query.validate();

		assertThatThrownBy(() -> result.throwIfInvalid()).isInstanceOf(QueryValidationException.class);
	}

	@Test
	void testCombineValidationResults() {
		ValidationResult valid = ValidationResult.valid();
		ValidationResult invalid = ValidationResult.invalid("Error");

		ValidationResult combined = valid.combine(invalid);

		assertThat(combined.isValid()).isFalse();
		assertThat(combined.errors()).hasSize(1);
	}

	@Test
	void testMultipleErrors() {
		// Create query with multiple issues
		Node root = new AndNode(
				java.util.List.of(new TokenNode(TokenType.KEYWORD, "hello"), new TokenNode(TokenType.EXCLUDE, "hello"), // Conflict
						new FieldNode("", "value") // Empty field name
				));

		Query query = new Query("test", root, QueryMetadata.builder().build());
		ValidationResult result = query.validate();

		assertThat(result.isValid()).isFalse();
		assertThat(result.errors()).hasSizeGreaterThan(1);
	}

}