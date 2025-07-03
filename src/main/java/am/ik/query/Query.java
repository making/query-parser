package am.ik.query;

import am.ik.query.ast.AndNode;
import am.ik.query.ast.FieldNode;
import am.ik.query.ast.FuzzyNode;
import am.ik.query.ast.Node;
import am.ik.query.ast.NodeVisitor;
import am.ik.query.ast.NotNode;
import am.ik.query.ast.OrNode;
import am.ik.query.ast.PhraseNode;
import am.ik.query.ast.RangeNode;
import am.ik.query.ast.RootNode;
import am.ik.query.ast.TokenNode;
import am.ik.query.ast.WildcardNode;
import am.ik.query.lexer.Token;
import am.ik.query.lexer.TokenType;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Represents a parsed search query with its AST structure. Provides fluent API for query
 * manipulation and traversal.
 *
 * @author Toshiaki Maki
 */
public final class Query {

	private final String originalQuery;

	private final Node rootNode;

	public Query(String originalQuery, Node rootNode) {
		this.originalQuery = Objects.requireNonNull(originalQuery, "originalQuery must not be null");
		this.rootNode = Objects.requireNonNull(rootNode, "rootNode must not be null");
	}

	/**
	 * Gets the original query string.
	 * @return the original query string
	 */
	public String originalQuery() {
		return originalQuery;
	}

	/**
	 * Gets the root node of the AST.
	 * @return the root node
	 */
	public Node rootNode() {
		return rootNode;
	}

	/**
	 * Accepts a visitor to traverse the query AST.
	 * @param visitor the visitor to accept
	 * @param <T> the return type of the visitor
	 * @return the result of the visitor
	 */
	public <T> T accept(NodeVisitor<T> visitor) {
		return rootNode.accept(visitor);
	}

	/**
	 * Walks through all nodes in the query AST.
	 * @param consumer the consumer to apply to each node
	 */
	public void walk(Consumer<Node> consumer) {
		rootNode.walk(consumer);
	}

	/**
	 * Transforms this query using the given transformer function.
	 * @param transformer the query transformer function
	 * @return the transformed query
	 */
	public Query transform(Function<Query, Query> transformer) {
		return transformer.apply(this);
	}

	/**
	 * Converts this query back to a string representation.
	 * @return the string representation of the query
	 */
	@Override
	public String toString() {
		return this.rootNode().accept(new SerializerVisitor());
	}

	/**
	 * Extracts all tokens of the specified type.
	 * @param tokenType the token type to extract
	 * @return list of matching tokens
	 */
	public List<Token> extractTokens(TokenType tokenType) {
		List<Token> tokens = new ArrayList<>();
		extractTokensWithContext(rootNode, tokenType, tokens, false);
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
	 * Extracts all keywords from the query.
	 * @return list of keyword tokens
	 */
	public List<String> extractKeywords() {
		return extractTokens(TokenType.KEYWORD).stream().map(Token::value).toList();
	}

	/**
	 * Extracts all phrases from the query.
	 * @return list of phrase tokens
	 */
	public List<String> extractPhrases() {
		return extractTokens(TokenType.PHRASE).stream().map(Token::value).toList();
	}

	/**
	 * Extracts all excluded terms from the query.
	 * @return list of excluded terms
	 */
	public List<String> extractExclusions() {
		return extractTokens(TokenType.EXCLUDE).stream().map(Token::value).toList();
	}

	/**
	 * Extracts all wildcard patterns from the query.
	 * @return list of wildcard patterns
	 */
	public List<String> extractWildcards() {
		return extractTokens(TokenType.WILDCARD).stream().map(Token::value).toList();
	}

	/**
	 * Extracts all field queries from the query.
	 * @return map of field names to their values
	 */
	public Map<String, List<String>> extractFields() {
		Map<String, List<String>> fields = new HashMap<>();
		rootNode.walk(node -> {
			if (node instanceof FieldNode fieldNode) {
				fields.computeIfAbsent(fieldNode.field(), k -> new ArrayList<>()).add(fieldNode.fieldValue());
			}
		});
		return fields;
	}

	/**
	 * Checks if the query is empty (contains no meaningful tokens).
	 * @return true if the query is empty
	 */
	public boolean isEmpty() {
		final boolean[] hasContent = { false };
		rootNode.walk(node -> {
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
	 * Counts nodes of a specific type.
	 * @param nodeClass the node class to count
	 * @return the count
	 */
	private int countNodesOfType(Class<? extends Node> nodeClass) {
		final int[] count = { 0 };
		rootNode.walk(node -> {
			if (nodeClass.isInstance(node)) {
				count[0]++;
			}
		});
		return count[0];
	}

	/**
	 * Finds all nodes of a specific type.
	 * @param nodeClass the node class to find
	 * @param <T> the node type
	 * @return list of matching nodes
	 */
	@SuppressWarnings("unchecked")
	public <T extends Node> List<T> findNodes(Class<T> nodeClass) {
		List<T> nodes = new ArrayList<>();
		rootNode.walk(node -> {
			if (nodeClass.isInstance(node)) {
				nodes.add((T) node);
			}
		});
		return nodes;
	}

	/**
	 * Counts nodes of a specific type (public version).
	 * @param nodeClass the node class to count
	 * @return the count
	 */
	public int countNodes(Class<? extends Node> nodeClass) {
		return countNodesOfType(nodeClass);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (!(o instanceof Query query))
			return false;
		return Objects.equals(originalQuery, query.originalQuery) && Objects.equals(rootNode, query.rootNode);
	}

	@Override
	public int hashCode() {
		return Objects.hash(originalQuery, rootNode);
	}

	private static class SerializerVisitor implements NodeVisitor<String> {

		@Override
		public String visitToken(TokenNode node) {
			switch (node.type()) {
				case EXCLUDE:
					return "-" + node.value();
				case REQUIRED:
					return "+" + node.value();
				default:
					return node.value();
			}
		}

		@Override
		public String visitRoot(RootNode node) {
			return String.join(" ", node.children().stream().map(child -> child.accept(this)).toList());
		}

		@Override
		public String visitAnd(AndNode node) {
			String content = String.join(" AND ", node.children().stream().map(child -> child.accept(this)).toList());
			return node.parent() != null ? "(" + content + ")" : content;
		}

		@Override
		public String visitOr(OrNode node) {
			String content = String.join(" OR ", node.children().stream().map(child -> child.accept(this)).toList());
			return node.parent() != null ? "(" + content + ")" : content;
		}

		@Override
		public String visitNot(NotNode node) {
			String child = node.child().accept(this);
			if (node.child() instanceof TokenNode) {
				return "-" + child;
			}
			return "NOT " + child;
		}

		@Override
		public String visitField(FieldNode node) {
			String value = node.fieldValue();
			// Quote value if it contains spaces
			if (value.contains(" ")) {
				value = "\"" + value + "\"";
			}
			return node.field() + ":" + value;
		}

		@Override
		public String visitPhrase(PhraseNode node) {
			return "\"" + node.phrase() + "\"";
		}

		@Override
		public String visitWildcard(WildcardNode node) {
			return node.pattern();
		}

		@Override
		public String visitFuzzy(FuzzyNode node) {
			return node.term() + "~" + (node.maxEdits() != 2 ? node.maxEdits() : "");
		}

		@Override
		public String visitRange(RangeNode node) {
			String startBracket = node.includeStart() ? "[" : "{";
			String endBracket = node.includeEnd() ? "]" : "}";
			String range = startBracket + node.start() + " TO " + node.end() + endBracket;
			return node.field() != null ? node.field() + ":" + range : range;
		}

	}

}