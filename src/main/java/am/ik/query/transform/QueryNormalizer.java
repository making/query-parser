package am.ik.query.transform;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import am.ik.query.Query;
import am.ik.query.ast.AndNode;
import am.ik.query.ast.FieldNode;
import am.ik.query.ast.FuzzyNode;
import am.ik.query.ast.Node;
import am.ik.query.ast.NotNode;
import am.ik.query.ast.OrNode;
import am.ik.query.ast.PhraseNode;
import am.ik.query.ast.RangeNode;
import am.ik.query.ast.RootNode;
import am.ik.query.ast.TokenNode;
import am.ik.query.ast.WildcardNode;
import am.ik.query.lexer.Token;
import am.ik.query.visitor.NodeVisitor;

/**
 * Query normalizer that standardizes queries.
 *
 * @author Toshiaki Maki
 */
public class QueryNormalizer {

	private QueryNormalizer() {
	}

	/**
	 * Creates the default query normalizer.
	 * @return the default normalizer
	 */
	public static QueryTransformer defaultNormalizer() {
		return toLowerCase().andThen(sortTerms()).andThen(normalizeWhitespace());
	}

	/**
	 * Converts all terms to lowercase.
	 * @return the normalizer
	 */
	public static QueryTransformer toLowerCase() {
		return toLowerCase(Locale.ROOT);
	}

	/**
	 * Converts all terms to lowercase using the specified locale.
	 * @param locale the locale to use
	 * @return the normalizer
	 */
	public static QueryTransformer toLowerCase(Locale locale) {
		return QueryTransformer.fromVisitor(new NodeVisitor<Node>() {
			@Override
			public Node visitToken(TokenNode node) {
				return new TokenNode(node.type(), node.value().toLowerCase(locale));
			}

			@Override
			public Node visitRoot(RootNode node) {
				List<Node> children = node.children().stream().map(child -> child.accept(this)).toList();
				return new RootNode(children);
			}

			@Override
			public Node visitField(FieldNode node) {
				// Keep field name as-is, only lowercase the value
				return new FieldNode(node.field(), node.fieldValue().toLowerCase(locale));
			}

			@Override
			public Node visitPhrase(PhraseNode node) {
				return new PhraseNode(node.phrase().toLowerCase(locale));
			}

			@Override
			public Node visitWildcard(WildcardNode node) {
				return new WildcardNode(node.pattern().toLowerCase(locale));
			}

			@Override
			public Node visitFuzzy(FuzzyNode node) {
				return new FuzzyNode(node.term().toLowerCase(locale), node.maxEdits());
			}

			@Override
			public Node visitNot(NotNode node) {
				return new NotNode(node.child().accept(this));
			}

			@Override
			public Node visitAnd(AndNode node) {
				List<Node> children = node.children().stream().map(child -> child.accept(this)).toList();
				return new AndNode(children);
			}

			@Override
			public Node visitOr(OrNode node) {
				List<Node> children = node.children().stream().map(child -> child.accept(this)).toList();
				return new OrNode(children);
			}

			@Override
			public Node visitRange(RangeNode node) {
				return node; // Range values typically shouldn't be lowercased
			}
		});
	}

	/**
	 * Sorts terms within AND/OR groups alphabetically.
	 * @return the normalizer
	 */
	public static QueryTransformer sortTerms() {
		return QueryTransformer.fromVisitor(new NodeVisitor<Node>() {
			private final Comparator<Node> nodeComparator = Comparator.comparing(Node::value);

			@Override
			public Node visitToken(TokenNode node) {
				return node;
			}

			@Override
			public Node visitRoot(RootNode node) {
				List<Node> children = node.children()
					.stream()
					.map(child -> child.accept(this))
					.sorted(nodeComparator)
					.toList();
				return new RootNode(children);
			}

			@Override
			public Node visitAnd(AndNode node) {
				List<Node> children = node.children()
					.stream()
					.map(child -> child.accept(this))
					.sorted(nodeComparator)
					.toList();
				return new AndNode(children);
			}

			@Override
			public Node visitOr(OrNode node) {
				List<Node> children = node.children()
					.stream()
					.map(child -> child.accept(this))
					.sorted(nodeComparator)
					.toList();
				return new OrNode(children);
			}

			@Override
			public Node visitNot(NotNode node) {
				return new NotNode(node.child().accept(this));
			}

			@Override
			public Node visitField(FieldNode node) {
				return node;
			}

			@Override
			public Node visitPhrase(PhraseNode node) {
				return node;
			}

			@Override
			public Node visitWildcard(WildcardNode node) {
				return node;
			}

			@Override
			public Node visitFuzzy(FuzzyNode node) {
				return node;
			}

			@Override
			public Node visitRange(RangeNode node) {
				return node;
			}
		});
	}

	/**
	 * Normalizes whitespace in phrases and terms.
	 * @return the normalizer
	 */
	public static QueryTransformer normalizeWhitespace() {
		return QueryTransformer.fromVisitor(new NodeVisitor<Node>() {
			@Override
			public Node visitToken(TokenNode node) {
				return new TokenNode(node.type(), normalizeSpace(node.value()));
			}

			@Override
			public Node visitRoot(RootNode node) {
				List<Node> children = node.children().stream().map(child -> child.accept(this)).toList();
				return new RootNode(children);
			}

			@Override
			public Node visitPhrase(PhraseNode node) {
				return new PhraseNode(normalizeSpace(node.phrase()));
			}

			@Override
			public Node visitAnd(AndNode node) {
				List<Node> children = node.children().stream().map(child -> child.accept(this)).toList();
				return new AndNode(children);
			}

			@Override
			public Node visitOr(OrNode node) {
				List<Node> children = node.children().stream().map(child -> child.accept(this)).toList();
				return new OrNode(children);
			}

			@Override
			public Node visitNot(NotNode node) {
				return new NotNode(node.child().accept(this));
			}

			@Override
			public Node visitField(FieldNode node) {
				return new FieldNode(node.field(), normalizeSpace(node.fieldValue()));
			}

			@Override
			public Node visitWildcard(WildcardNode node) {
				return new WildcardNode(normalizeSpace(node.pattern()));
			}

			@Override
			public Node visitFuzzy(FuzzyNode node) {
				return new FuzzyNode(normalizeSpace(node.term()), node.maxEdits());
			}

			@Override
			public Node visitRange(RangeNode node) {
				return node;
			}

			private String normalizeSpace(String text) {
				return text.trim().replaceAll("\\s+", " ");
			}
		});
	}

	/**
	 * Removes diacritics from terms.
	 * @return the normalizer
	 */
	public static QueryTransformer removeDiacritics() {
		return QueryTransformer.fromVisitor(new NodeVisitor<Node>() {
			@Override
			public Node visitToken(TokenNode node) {
				return new TokenNode(node.type(), removeDiacriticsFromString(node.value()));
			}

			@Override
			public Node visitRoot(RootNode node) {
				List<Node> children = node.children().stream().map(child -> child.accept(this)).toList();
				return new RootNode(children);
			}

			@Override
			public Node visitPhrase(PhraseNode node) {
				return new PhraseNode(removeDiacriticsFromString(node.phrase()));
			}

			@Override
			public Node visitField(FieldNode node) {
				return new FieldNode(node.field(), removeDiacriticsFromString(node.fieldValue()));
			}

			@Override
			public Node visitWildcard(WildcardNode node) {
				return new WildcardNode(removeDiacriticsFromString(node.pattern()));
			}

			@Override
			public Node visitFuzzy(FuzzyNode node) {
				return new FuzzyNode(removeDiacriticsFromString(node.term()), node.maxEdits());
			}

			@Override
			public Node visitAnd(AndNode node) {
				List<Node> children = node.children().stream().map(child -> child.accept(this)).toList();
				return new AndNode(children);
			}

			@Override
			public Node visitOr(OrNode node) {
				List<Node> children = node.children().stream().map(child -> child.accept(this)).toList();
				return new OrNode(children);
			}

			@Override
			public Node visitNot(NotNode node) {
				return new NotNode(node.child().accept(this));
			}

			@Override
			public Node visitRange(RangeNode node) {
				return node;
			}

			private String removeDiacriticsFromString(String text) {
				return java.text.Normalizer.normalize(text, java.text.Normalizer.Form.NFD)
					.replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
			}
		});
	}

}