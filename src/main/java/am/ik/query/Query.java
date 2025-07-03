package am.ik.query;

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

	private final QueryMetadata metadata;

	Query(String originalQuery, Node rootNode, QueryMetadata metadata) {
		this.originalQuery = Objects.requireNonNull(originalQuery, "originalQuery must not be null");
		this.rootNode = Objects.requireNonNull(rootNode, "rootNode must not be null");
		this.metadata = Objects.requireNonNull(metadata, "metadata must not be null");
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
	 * Gets the query metadata.
	 * @return the query metadata
	 */
	public QueryMetadata metadata() {
		return metadata;
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
		return QuerySerializer.serialize(this);
	}

	/**
	 * Pretty prints this query to the console.
	 */
	public void prettyPrint() {
		QueryPrinter.print(this);
	}

	/**
	 * Pretty prints this query and returns the string representation.
	 * @return the pretty printed string
	 */
	public String toPrettyString() {
		return QueryPrinter.toPrettyString(this);
	}

	/**
	 * Extracts all tokens of the specified type.
	 * @param tokenType the token type to extract
	 * @return list of matching tokens
	 */
	public List<Token> extractTokens(TokenType tokenType) {
		return QueryUtils.extractTokens(this, tokenType);
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
		return QueryUtils.isEmpty(this);
	}

	/**
	 * Checks if the query contains any OR operations.
	 * @return true if the query contains OR operations
	 */
	public boolean hasOrOperations() {
		return QueryUtils.hasTokenType(this, TokenType.OR) || QueryUtils.countNodesOfType(this, OrNode.class) > 0;
	}

	/**
	 * Checks if the query contains any AND operations.
	 * @return true if the query contains AND operations
	 */
	public boolean hasAndOperations() {
		return QueryUtils.hasTokenType(this, TokenType.AND) || QueryUtils.countNodesOfType(this, AndNode.class) > 0;
	}

	/**
	 * Checks if the query contains any exclusions.
	 * @return true if the query contains exclusions
	 */
	public boolean hasExclusions() {
		return QueryUtils.hasTokenType(this, TokenType.EXCLUDE);
	}

	/**
	 * Checks if the query contains any phrases.
	 * @return true if the query contains phrases
	 */
	public boolean hasPhrases() {
		return QueryUtils.hasTokenType(this, TokenType.PHRASE);
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

	/**
	 * Fluent builder for programmatically constructing queries.
	 */
	public static class Builder {

		private final java.util.Stack<BuilderNode> stack = new java.util.Stack<>();

		Builder() {
			stack.push(new BuilderNode(null, null));
		}

		/**
		 * Adds a keyword to the query.
		 * @param keyword the keyword to add
		 * @return this builder
		 */
		public Builder keyword(String keyword) {
			Objects.requireNonNull(keyword, "keyword must not be null");
			stack.peek().children.add(new TokenNode(TokenType.KEYWORD, keyword));
			return this;
		}

		/**
		 * Adds multiple keywords to the query.
		 * @param keywords the keywords to add
		 * @return this builder
		 */
		public Builder keywords(String... keywords) {
			for (String keyword : keywords) {
				keyword(keyword);
			}
			return this;
		}

		/**
		 * Adds a phrase to the query.
		 * @param phrase the phrase to add
		 * @return this builder
		 */
		public Builder phrase(String phrase) {
			Objects.requireNonNull(phrase, "phrase must not be null");
			stack.peek().children.add(new PhraseNode(phrase));
			return this;
		}

		/**
		 * Adds a field query.
		 * @param field the field name
		 * @param value the field value
		 * @return this builder
		 */
		public Builder field(String field, String value) {
			Objects.requireNonNull(field, "field must not be null");
			Objects.requireNonNull(value, "value must not be null");
			stack.peek().children.add(new FieldNode(field, value));
			return this;
		}

		/**
		 * Adds a wildcard query.
		 * @param pattern the wildcard pattern
		 * @return this builder
		 */
		public Builder wildcard(String pattern) {
			Objects.requireNonNull(pattern, "pattern must not be null");
			stack.peek().children.add(new WildcardNode(pattern));
			return this;
		}

		/**
		 * Adds a fuzzy query.
		 * @param term the term
		 * @param maxEdits the maximum edit distance (0-2)
		 * @return this builder
		 */
		public Builder fuzzy(String term, int maxEdits) {
			Objects.requireNonNull(term, "term must not be null");
			stack.peek().children.add(new FuzzyNode(term, maxEdits));
			return this;
		}

		/**
		 * Adds a fuzzy query with default max edits (2).
		 * @param term the term
		 * @return this builder
		 */
		public Builder fuzzy(String term) {
			return fuzzy(term, 2);
		}

		/**
		 * Adds a range query.
		 * @param start the start value
		 * @param end the end value
		 * @param includeStart whether to include the start value
		 * @param includeEnd whether to include the end value
		 * @return this builder
		 */
		public Builder range(String start, String end, boolean includeStart, boolean includeEnd) {
			stack.peek().children.add(RangeNode.builder()
				.start(start)
				.end(end)
				.includeStart(includeStart)
				.includeEnd(includeEnd)
				.build());
			return this;
		}

		/**
		 * Adds an inclusive range query.
		 * @param start the start value
		 * @param end the end value
		 * @return this builder
		 */
		public Builder range(String start, String end) {
			return range(start, end, true, true);
		}

		/**
		 * Starts an AND group.
		 * @return this builder
		 */
		public Builder and() {
			BuilderNode node = new BuilderNode("AND", stack.peek());
			stack.push(node);
			return this;
		}

		/**
		 * Starts an OR group.
		 * @return this builder
		 */
		public Builder or() {
			BuilderNode node = new BuilderNode("OR", stack.peek());
			stack.push(node);
			return this;
		}

		/**
		 * Starts a NOT group.
		 * @return this builder
		 */
		public Builder not() {
			BuilderNode node = new BuilderNode("NOT", stack.peek());
			stack.push(node);
			return this;
		}

		/**
		 * Ends the current group.
		 * @return this builder
		 */
		public Builder endGroup() {
			if (stack.size() > 1) {
				BuilderNode completed = stack.pop();
				stack.peek().children.add(completed.toNode());
			}
			return this;
		}

		/**
		 * Adds an excluded term.
		 * @param term the term to exclude
		 * @return this builder
		 */
		public Builder exclude(String term) {
			Objects.requireNonNull(term, "term must not be null");
			stack.peek().children.add(new NotNode(new TokenNode(TokenType.KEYWORD, term)));
			return this;
		}

		/**
		 * Builds the query.
		 * @return the built Query
		 */
		public Query build() {
			// Complete any open groups
			while (stack.size() > 1) {
				endGroup();
			}

			BuilderNode root = stack.peek();
			Node rootNode = root.toNode();

			String queryString = QuerySerializer.serialize(new Query("", rootNode, QueryMetadata.builder().build()));

			return QueryParser.create().parse(queryString);
		}

		private static class BuilderNode {

			private final String type;

			private final BuilderNode parent;

			private final List<Node> children = new ArrayList<>();

			BuilderNode(String type, BuilderNode parent) {
				this.type = type;
				this.parent = parent;
			}

			Node toNode() {
				if (type == null) {
					// Root node
					if (children.isEmpty()) {
						return new RootNode();
					}
					else if (children.size() == 1) {
						return children.get(0);
					}
					else {
						return new AndNode(new ArrayList<>(children));
					}
				}

				switch (type) {
					case "AND":
						return new AndNode(new ArrayList<>(children));
					case "OR":
						return new OrNode(new ArrayList<>(children));
					case "NOT":
						if (children.size() != 1) {
							throw new IllegalStateException("NOT must have exactly one child");
						}
						return new NotNode(children.get(0));
					default:
						throw new IllegalStateException("Unknown node type: " + type);
				}
			}

		}

	}

}