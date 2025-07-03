package am.ik.query;

import java.util.ArrayList;
import java.util.List;

/**
 * Utility methods for working with queries.
 *
 * @author Toshiaki Maki
 */
public class QueryUtils {

	private QueryUtils() {
	}

	/**
	 * Extracts all tokens of the specified type from a query.
	 * @param query the query
	 * @param tokenType the token type to extract
	 * @return list of matching tokens
	 */
	public static List<Token> extractTokens(Query query, TokenType tokenType) {
		List<Token> tokens = new ArrayList<>();
		extractTokensWithContext(query.rootNode(), tokenType, tokens, false);
		return tokens;
	}

	private static void extractTokensWithContext(Node node, TokenType tokenType, List<Token> tokens,
			boolean insideNot) {
		if (node instanceof NotNode notNode) {
			// Mark that we're inside a NOT node
			extractTokensWithContext(notNode.child(), tokenType, tokens, true);
		}
		else if (node.isLeaf()) {
			switch (tokenType) {
				case KEYWORD:
					if (!insideNot && node instanceof TokenNode tokenNode && tokenNode.type() == TokenType.KEYWORD) {
						tokens.add(tokenNode.token());
					}
					break;
				case PHRASE:
					if (!insideNot && node instanceof PhraseNode phraseNode) {
						tokens.add(phraseNode.token());
					}
					break;
				case EXCLUDE:
					if (insideNot) {
						if (node instanceof TokenNode tokenNode) {
							tokens.add(new Token(TokenType.EXCLUDE, tokenNode.value()));
						}
						else if (node instanceof PhraseNode phraseNode) {
							tokens.add(new Token(TokenType.EXCLUDE, phraseNode.phrase()));
						}
					}
					break;
				case WILDCARD:
					if (!insideNot && node instanceof WildcardNode wildcardNode) {
						tokens.add(new Token(TokenType.WILDCARD, wildcardNode.pattern()));
					}
					break;
				default:
					if (!insideNot && node instanceof TokenNode tokenNode && tokenNode.type() == tokenType) {
						tokens.add(tokenNode.token());
					}
			}
		}
		else {
			// Recurse for non-leaf nodes
			for (Node child : node.children()) {
				extractTokensWithContext(child, tokenType, tokens, insideNot);
			}
		}
	}

	/**
	 * Checks if a query contains tokens of the specified type.
	 * @param query the query
	 * @param tokenType the token type to check
	 * @return true if the query contains the token type
	 */
	public static boolean hasTokenType(Query query, TokenType tokenType) {
		final boolean[] found = { false };
		query.walk(node -> {
			if (!found[0]) {
				switch (tokenType) {
					case KEYWORD:
						if (node instanceof TokenNode tokenNode && tokenNode.type() == TokenType.KEYWORD) {
							found[0] = true;
						}
						break;
					case PHRASE:
						if (node instanceof PhraseNode) {
							found[0] = true;
						}
						break;
					case EXCLUDE:
						if (node instanceof NotNode) {
							found[0] = true;
						}
						break;
					case OR:
						if (node instanceof TokenNode tokenNode && tokenNode.type() == TokenType.OR) {
							found[0] = true;
						}
						break;
					case AND:
						if (node instanceof TokenNode tokenNode && tokenNode.type() == TokenType.AND) {
							found[0] = true;
						}
						break;
					case WILDCARD:
						if (node instanceof WildcardNode) {
							found[0] = true;
						}
						break;
					default:
						if (node instanceof TokenNode tokenNode && tokenNode.type() == tokenType) {
							found[0] = true;
						}
				}
			}
		});
		return found[0];
	}

	/**
	 * Checks if a query is empty (contains no meaningful tokens).
	 * @param query the query
	 * @return true if the query is empty
	 */
	public static boolean isEmpty(Query query) {
		final boolean[] hasContent = { false };
		query.walk(node -> {
			if (!hasContent[0]) {
				if (node instanceof TokenNode tokenNode) {
					TokenType type = tokenNode.type();
					if (type.isContent() || type == TokenType.EXCLUDE) {
						hasContent[0] = true;
					}
				}
				else if (node instanceof PhraseNode || node instanceof FieldNode || node instanceof WildcardNode
						|| node instanceof FuzzyNode || node instanceof RangeNode || node instanceof NotNode) {
					hasContent[0] = true;
				}
			}
		});
		return !hasContent[0];
	}

	/**
	 * Counts the total number of nodes in a query.
	 * @param query the query
	 * @return the node count
	 */
	public static int countNodes(Query query) {
		final int[] count = { 0 };
		query.walk(node -> count[0]++);
		return count[0];
	}

	/**
	 * Counts nodes of a specific type.
	 * @param query the query
	 * @param nodeClass the node class to count
	 * @return the count
	 */
	public static int countNodesOfType(Query query, Class<? extends Node> nodeClass) {
		final int[] count = { 0 };
		query.walk(node -> {
			if (nodeClass.isInstance(node)) {
				count[0]++;
			}
		});
		return count[0];
	}

	/**
	 * Finds all nodes of a specific type.
	 * @param query the query
	 * @param nodeClass the node class to find
	 * @param <T> the node type
	 * @return list of matching nodes
	 */
	@SuppressWarnings("unchecked")
	public static <T extends Node> List<T> findNodes(Query query, Class<T> nodeClass) {
		List<T> nodes = new ArrayList<>();
		query.walk(node -> {
			if (nodeClass.isInstance(node)) {
				nodes.add((T) node);
			}
		});
		return nodes;
	}

	/**
	 * Calculates the maximum depth of a query AST.
	 * @param node the root node
	 * @return the maximum depth
	 */
	public static int calculateMaxDepth(Node node) {
		if (node.isLeaf()) {
			return 1;
		}

		int maxChildDepth = 0;
		for (Node child : node.children()) {
			maxChildDepth = Math.max(maxChildDepth, calculateMaxDepth(child));
		}

		return maxChildDepth + 1;
	}

	/**
	 * Creates a deep copy of a node and its subtree.
	 * @param node the node to copy
	 * @return the copied node
	 */
	public static Node deepCopy(Node node) {
		return node.accept(new NodeVisitor<Node>() {
			@Override
			public Node visitToken(TokenNode node) {
				return new TokenNode(node.type(), node.value());
			}

			@Override
			public Node visitRoot(RootNode node) {
				List<Node> copiedChildren = node.children().stream().map(child -> child.accept(this)).toList();
				return new RootNode(copiedChildren);
			}

			@Override
			public Node visitAnd(AndNode node) {
				List<Node> copiedChildren = node.children().stream().map(child -> child.accept(this)).toList();
				return new AndNode(copiedChildren);
			}

			@Override
			public Node visitOr(OrNode node) {
				List<Node> copiedChildren = node.children().stream().map(child -> child.accept(this)).toList();
				return new OrNode(copiedChildren);
			}

			@Override
			public Node visitNot(NotNode node) {
				return new NotNode(node.child().accept(this));
			}

			@Override
			public Node visitField(FieldNode node) {
				return new FieldNode(node.field(), node.fieldValue());
			}

			@Override
			public Node visitPhrase(PhraseNode node) {
				return new PhraseNode(node.phrase());
			}

			@Override
			public Node visitWildcard(WildcardNode node) {
				return new WildcardNode(node.pattern());
			}

			@Override
			public Node visitFuzzy(FuzzyNode node) {
				return new FuzzyNode(node.term(), node.maxEdits());
			}

			@Override
			public Node visitRange(RangeNode node) {
				return RangeNode.builder()
					.start(node.start())
					.end(node.end())
					.includeStart(node.includeStart())
					.includeEnd(node.includeEnd())
					.field(node.field())
					.build();
			}
		});
	}

}