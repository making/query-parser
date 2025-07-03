package am.ik.query;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Query optimizer that applies various optimization strategies.
 *
 * @author Toshiaki Maki
 */
public class QueryOptimizer {

	private QueryOptimizer() {
	}

	/**
	 * Creates the default query optimizer.
	 * @return the default optimizer
	 */
	public static QueryTransformer defaultOptimizer() {
		return removeDuplicates().andThen(flattenNestedBooleans())
			.andThen(simplifyBooleans())
			.andThen(removeEmptyGroups());
	}

	/**
	 * Removes duplicate terms from AND/OR groups.
	 * @return the optimizer
	 */
	public static QueryTransformer removeDuplicates() {
		return QueryTransformer.fromVisitor(new NodeVisitor<Node>() {
			@Override
			public Node visitToken(TokenNode node) {
				return node;
			}

			@Override
			public Node visitRoot(RootNode node) {
				List<Node> children = node.children().stream().map(child -> child.accept(this)).toList();
				return new RootNode(deduplicate(children));
			}

			@Override
			public Node visitAnd(AndNode node) {
				List<Node> children = node.children().stream().map(child -> child.accept(this)).toList();
				return new AndNode(deduplicate(children));
			}

			@Override
			public Node visitOr(OrNode node) {
				List<Node> children = node.children().stream().map(child -> child.accept(this)).toList();
				return new OrNode(deduplicate(children));
			}

			private List<Node> deduplicate(List<Node> nodes) {
				Set<String> seen = new HashSet<>();
				List<Node> result = new ArrayList<>();
				for (Node node : nodes) {
					String key = nodeKey(node);
					if (seen.add(key)) {
						result.add(node);
					}
				}
				return result;
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

			private String nodeKey(Node node) {
				return node.getClass().getSimpleName() + ":" + node.value();
			}
		});
	}

	/**
	 * Flattens nested boolean operations of the same type. For example: AND(a, AND(b, c))
	 * becomes AND(a, b, c)
	 * @return the optimizer
	 */
	public static QueryTransformer flattenNestedBooleans() {
		return QueryTransformer.fromVisitor(new NodeVisitor<Node>() {
			@Override
			public Node visitToken(TokenNode node) {
				return node;
			}

			@Override
			public Node visitRoot(RootNode node) {
				List<Node> children = node.children().stream().map(child -> child.accept(this)).toList();
				return new RootNode(children);
			}

			@Override
			public Node visitAnd(AndNode node) {
				List<Node> flattened = new ArrayList<>();
				for (Node child : node.children()) {
					Node processed = child.accept(this);
					if (processed instanceof AndNode) {
						flattened.addAll(processed.children());
					}
					else {
						flattened.add(processed);
					}
				}
				return new AndNode(flattened);
			}

			@Override
			public Node visitOr(OrNode node) {
				List<Node> flattened = new ArrayList<>();
				for (Node child : node.children()) {
					Node processed = child.accept(this);
					if (processed instanceof OrNode) {
						flattened.addAll(processed.children());
					}
					else {
						flattened.add(processed);
					}
				}
				return new OrNode(flattened);
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
	 * Simplifies boolean operations. - AND/OR with single child becomes the child -
	 * NOT(NOT(x)) becomes x
	 * @return the optimizer
	 */
	public static QueryTransformer simplifyBooleans() {
		return QueryTransformer.fromVisitor(new NodeVisitor<Node>() {
			@Override
			public Node visitToken(TokenNode node) {
				return node;
			}

			@Override
			public Node visitRoot(RootNode node) {
				List<Node> children = node.children().stream().map(child -> child.accept(this)).toList();
				return new RootNode(children);
			}

			@Override
			public Node visitAnd(AndNode node) {
				List<Node> children = node.children().stream().map(child -> child.accept(this)).toList();
				if (children.size() == 1) {
					return children.get(0);
				}
				return new AndNode(children);
			}

			@Override
			public Node visitOr(OrNode node) {
				List<Node> children = node.children().stream().map(child -> child.accept(this)).toList();
				if (children.size() == 1) {
					return children.get(0);
				}
				return new OrNode(children);
			}

			@Override
			public Node visitNot(NotNode node) {
				Node child = node.child().accept(this);
				if (child instanceof NotNode) {
					// Double negation
					return ((NotNode) child).child();
				}
				return new NotNode(child);
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
	 * Removes empty groups from the query.
	 * @return the optimizer
	 */
	public static QueryTransformer removeEmptyGroups() {
		return QueryTransformer.fromVisitor(new NodeVisitor<Node>() {
			@Override
			public Node visitToken(TokenNode node) {
				return node;
			}

			@Override
			public Node visitRoot(RootNode node) {
				List<Node> children = node.children()
					.stream()
					.map(child -> child.accept(this))
					.filter(this::isNotEmpty)
					.toList();
				return new RootNode(children);
			}

			@Override
			public Node visitAnd(AndNode node) {
				List<Node> children = node.children()
					.stream()
					.map(child -> child.accept(this))
					.filter(this::isNotEmpty)
					.toList();
				if (children.isEmpty()) {
					return null;
				}
				return new AndNode(children);
			}

			@Override
			public Node visitOr(OrNode node) {
				List<Node> children = node.children()
					.stream()
					.map(child -> child.accept(this))
					.filter(this::isNotEmpty)
					.toList();
				if (children.isEmpty()) {
					return null;
				}
				return new OrNode(children);
			}

			@Override
			public Node visitNot(NotNode node) {
				Node child = node.child().accept(this);
				if (child == null) {
					return null;
				}
				return new NotNode(child);
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

			private boolean isNotEmpty(Node node) {
				if (node == null) {
					return false;
				}
				if (node.isLeaf()) {
					return true;
				}
				return !node.children().isEmpty();
			}
		});
	}

}