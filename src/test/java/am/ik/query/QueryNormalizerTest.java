package am.ik.query;

import org.junit.jupiter.api.Test;

import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;

class QueryNormalizerTest {

	@Test
	void testToLowerCase() {
		Query query = QueryParser.create().parse("HELLO World");
		Query normalized = query.transform(QueryNormalizer.toLowerCase());

		assertThat(normalized.toString()).doesNotContain("HELLO");
		assertThat(normalized.toString()).doesNotContain("World");
		assertThat(normalized.toString()).contains("hello");
		assertThat(normalized.toString()).contains("world");
	}

	@Test
	void testToLowerCaseWithLocale() {
		Query query = QueryParser.create().parse("İstanbul"); // Turkish capital
																// I
		Query normalized = query.transform(QueryNormalizer.toLowerCase(new Locale("tr", "TR")));

		// Turkish lowercase of İ is i
		assertThat(normalized.toString()).contains("istanbul");
	}

	@Test
	void testSortTerms() {
		Query query = QueryParser.create().parse("zebra AND apple OR banana");
		Query sorted = query.transform(QueryNormalizer.sortTerms());

		String result = sorted.toString();
		int applePos = result.indexOf("apple");
		int bananaPos = result.indexOf("banana");
		int zebraPos = result.indexOf("zebra");

		// Terms should be sorted alphabetically within groups
		assertThat(applePos).isLessThan(zebraPos);
	}

	@Test
	void testNormalizeWhitespace() {
		Query query = QueryParser.create().parse("\"hello   world\" AND   test");
		Query normalized = query.transform(QueryNormalizer.normalizeWhitespace());

		assertThat(normalized.toString()).contains("hello world"); // Single space
		// Multiple spaces should be normalized
	}

	@Test
	void testRemoveDiacritics() {
		Query query = QueryParser.create().parse("café naïve résumé");
		Query normalized = query.transform(QueryNormalizer.removeDiacritics());

		assertThat(normalized.toString()).contains("cafe");
		assertThat(normalized.toString()).contains("naive");
		assertThat(normalized.toString()).contains("resume");
	}

	@Test
	void testDefaultNormalizer() {
		Query query = QueryParser.create().parse("HELLO   café  WORLD");
		Query normalized = query.normalize();

		// Should apply lowercase, whitespace normalization, and sorting
		assertThat(normalized.toString()).doesNotContain("HELLO");
		assertThat(normalized.toString()).doesNotContain("WORLD");
	}

	@Test
	void testNormalizerChaining() {
		QueryTransformer normalizer = QueryNormalizer.toLowerCase()
			.andThen(QueryNormalizer.removeDiacritics())
			.andThen(QueryNormalizer.sortTerms());

		Query query = QueryParser.create().parse("Zürich CAFÉ apple");
		Query normalized = query.transform(normalizer);

		assertThat(normalized.toString()).contains("zurich");
		assertThat(normalized.toString()).contains("cafe");
		assertThat(normalized.toString()).contains("apple");
	}

	@Test
	void testNormalizeComplexQuery() {
		Query query = QueryParser.create().parse("(HELLO OR world) AND \"Test  Phrase\" -EXCLUDE");
		Query normalized = query.normalize();

		assertThat(normalized.toString()).contains("hello");
		assertThat(normalized.toString()).contains("world");
		assertThat(normalized.toString()).contains("test phrase"); // Normalized
																	// whitespace and
																	// lowercase
	}

	@Test
	void testFieldNormalization() {
		Query query = QueryParser.create().parse("title:HELLO author:\"John  Doe\"");
		Query normalized = query.normalize();

		// Field names should remain as-is, values should be normalized
		assertThat(normalized.toString()).contains("title:");
		assertThat(normalized.toString()).contains("hello");
		assertThat(normalized.toString()).contains("john doe");
	}

}