package am.ik.query;

import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Query parser with advanced features and builder pattern.
 *
 * @author Toshiaki Maki
 */
public final class QueryParser {

	private static final Logger logger = LoggerFactory.getLogger(QueryParser.class);

	private final QueryLexer lexer;

	private final ParserOptions options;

	private final Map<String, Function<String, Node>> fieldParsers;

	private List<Token> tokens;

	private int position;

	private int maxDepth;

	private QueryParser(Builder builder) {
		this.lexer = builder.lexer != null ? builder.lexer : QueryLexer.defaultLexer();
		this.options = builder.options;
		this.fieldParsers = new HashMap<>(builder.fieldParsers);
	}

	/**
	 * Parses the given query string into a Query object.
	 * @param queryString the query string to parse
	 * @return the parsed Query
	 * @throws QueryParseException if parsing fails
	 */
	public Query parse(String queryString) {
		if (queryString == null) {
			throw new IllegalArgumentException("queryString must not be null");
		}

		long startTime = System.nanoTime();
		Instant parsedAt = Instant.now();

		try {
			// Tokenize
			this.tokens = lexer.tokenize(queryString);
			this.position = 0;
			this.maxDepth = 0;

			// Parse
			Node rootNode = parseQuery();

			// Calculate metadata
			long parseTime = System.nanoTime() - startTime;
			QueryMetadata metadata = QueryMetadata.builder()
				.tokenCount(tokens.size())
				.nodeCount(countNodes(rootNode))
				.maxDepth(QueryUtils.calculateMaxDepth(rootNode))
				.parseTimeNanos(parseTime)
				.parsedAt(parsedAt)
				.property("originalTokens", tokens)
				.build();

			Query query = new Query(queryString, rootNode, metadata);

			// Validate if enabled
			if (options.validateAfterParse()) {
				ValidationResult result = QueryValidator.validate(query, options.allowedTokenTypes());
				if (!result.isValid() && options.throwOnValidationError()) {
					throw new QueryValidationException(result);
				}
			}

			return query;
		}
		catch (Exception e) {
			if (e instanceof QueryParseException || e instanceof QueryValidationException) {
				throw e;
			}
			throw new QueryParseException("Failed to parse query: " + e.getMessage(), queryString, position, e);
		}
	}

	private Node parseQuery() {
		List<Node> nodes = new ArrayList<>();

		while (!isAtEnd()) {
			skipWhitespace();
			if (isAtEnd())
				break;

			Node node = parseOr();
			if (node != null) {
				nodes.add(node);
			}
		}

		if (nodes.isEmpty()) {
			return new RootNode();
		}
		else if (nodes.size() == 1) {
			return nodes.get(0);
		}
		else {
			// Multiple nodes - combine with default operator
			if (options.defaultOperator() == BooleanOperator.OR) {
				return new OrNode(nodes);
			}
			else {
				return new AndNode(nodes);
			}
		}
	}

	private Node parseOr() {
		Node left = parseAnd();

		// Skip whitespace before checking for OR
		skipWhitespace();

		// Create OR nodes only for explicit OR tokens
		while (match(TokenType.OR)) {
			List<Node> nodes = new ArrayList<>();
			nodes.add(left);

			do {
				skipWhitespace();
				Node right = parseAnd();
				if (right != null) {
					nodes.add(right);
				}
				skipWhitespace(); // Skip whitespace before checking for more ORs
			}
			while (match(TokenType.OR));

			left = new OrNode(nodes);
		}

		return left;
	}

	private Node parseAnd() {
		Node left = parseNot();

		if (left == null)
			return null;

		// Skip whitespace before checking for OR/AND
		skipWhitespace();

		// Create AND nodes only for explicit AND tokens
		boolean foundAnd = false;
		while (match(TokenType.AND)) {
			foundAnd = true;

			List<Node> nodes = new ArrayList<>();
			nodes.add(left);

			do {
				skipWhitespace();
				if (isAtEnd() || check(TokenType.RPAREN))
					break;

				Node right = parseNot();
				if (right != null) {
					nodes.add(right);
				}
				skipWhitespace(); // Skip whitespace before checking for more ANDs
			}
			while (match(TokenType.AND));

			if (nodes.size() > 1) {
				left = new AndNode(nodes);
			}
		}

		return left;
	}

	private Node parseNot() {
		if (match(TokenType.NOT)) {
			skipWhitespace();
			// Recursively parse NOT to handle double negation
			Node node = parseNot();
			if (node == null) {
				throw new QueryParseException("Expected term after NOT operator", tokens.toString(), position);
			}
			return new NotNode(node);
		}
		// EXCLUDE tokens are handled in parseTerm
		return parseTerm();
	}

	private Node parseTerm() {
		skipWhitespace();

		// Handle grouping
		if (match(TokenType.LPAREN)) {
			maxDepth++;
			Node node = parseOr();
			consume(TokenType.RPAREN, "Expected ')' after expression");
			return node;
		}

		// Handle required terms
		if (match(TokenType.REQUIRED)) {
			return parseTerm(); // Required is handled by validation/execution
		}

		// Handle field queries
		if (check(TokenType.KEYWORD) && checkNext(TokenType.COLON)) {
			return parseField();
		}

		// Handle phrases
		if (match(TokenType.PHRASE)) {
			String phrase = previous().value();
			// Remove quotes if present
			if (phrase.startsWith("\"") && phrase.endsWith("\"") && phrase.length() > 1) {
				phrase = phrase.substring(1, phrase.length() - 1);
			}
			return new PhraseNode(phrase, previous());
		}

		// Handle wildcards
		if (match(TokenType.WILDCARD)) {
			return new WildcardNode(previous().value(), previous());
		}

		// Handle EXCLUDE tokens (when lexer creates "-word" as single EXCLUDE token)
		if (match(TokenType.EXCLUDE)) {
			Token exclude = previous();
			return new NotNode(new TokenNode(TokenType.KEYWORD, exclude.value()));
		}

		// Handle keywords with modifiers
		if (match(TokenType.KEYWORD)) {
			Token keyword = previous();
			String value = keyword.value();

			// Check for fuzzy
			if (match(TokenType.FUZZY)) {
				int maxEdits = 2;
				if (check(TokenType.KEYWORD) && peek().value().matches("\\d")) {
					maxEdits = Integer.parseInt(advance().value());
				}
				return new FuzzyNode(value, maxEdits);
			}

			// Check for wildcard in keyword
			if (value.contains("*") || value.contains("?")) {
				return new WildcardNode(value, keyword);
			}

			return new TokenNode(keyword);
		}

		// Handle range queries
		if (match(TokenType.RANGE_START)) {
			return parseRange();
		}

		// Skip unknown tokens
		if (!isAtEnd()) {
			advance();
		}

		return null;
	}

	private Node parseField() {
		String field = advance().value();
		consume(TokenType.COLON, "Expected ':' after field name");
		skipWhitespace();

		// Check if field has custom parser
		if (fieldParsers.containsKey(field)) {
			String value = advance().value();
			return fieldParsers.get(field).apply(value);
		}

		// Parse field value
		Node value = parseTerm();
		if (value instanceof TokenNode tokenNode) {
			return new FieldNode(field, tokenNode.value(), tokenNode.token());
		}
		else if (value instanceof PhraseNode phraseNode) {
			return new FieldNode(field, phraseNode.phrase(), phraseNode.token());
		}

		throw new QueryParseException("Invalid field value", tokens.toString(), position);
	}

	private Node parseRange() {
		boolean includeStart = previous().value().equals("[");
		skipWhitespace();

		String start = advance().value();
		skipWhitespace();

		// Check for TO keyword
		if (!check(TokenType.RANGE_TO) && peek().value().equalsIgnoreCase("TO")) {
			advance(); // consume TO
		}
		else {
			consume(TokenType.RANGE_TO, "Expected 'TO' in range query");
		}
		skipWhitespace();

		String end = advance().value();
		skipWhitespace();

		Token endBracket = consume(TokenType.RANGE_END, "Expected ']' or '}' to close range");
		boolean includeEnd = endBracket.value().equals("]");

		return RangeNode.builder().start(start).end(end).includeStart(includeStart).includeEnd(includeEnd).build();
	}

	private boolean match(TokenType... types) {
		for (TokenType type : types) {
			if (check(type)) {
				advance();
				return true;
			}
		}
		return false;
	}

	private boolean check(TokenType type) {
		if (isAtEnd())
			return false;
		return peek().type() == type;
	}

	private boolean check(TokenType... types) {
		if (isAtEnd())
			return false;
		TokenType currentType = peek().type();
		for (TokenType type : types) {
			if (currentType == type)
				return true;
		}
		return false;
	}

	private boolean checkNext(TokenType type) {
		if (position + 1 >= tokens.size())
			return false;
		return tokens.get(position + 1).type() == type;
	}

	private Token advance() {
		if (!isAtEnd())
			position++;
		return previous();
	}

	private boolean isAtEnd() {
		return position >= tokens.size() || peek().type() == TokenType.EOF;
	}

	private Token peek() {
		return tokens.get(position);
	}

	private Token previous() {
		return tokens.get(position - 1);
	}

	private Token consume(TokenType type, String message) {
		if (check(type))
			return advance();

		throw new QueryParseException(message + " at position " + position, tokens.toString(), position);
	}

	private void skipWhitespace() {
		while (match(TokenType.WHITESPACE)) {
			// Skip whitespace
		}
	}

	private int countNodes(Node node) {
		final int[] count = { 0 };
		node.walk(n -> count[0]++);
		return count[0];
	}

	/**
	 * Creates a new parser builder.
	 * @return a new builder
	 */
	public static Builder builder() {
		return new Builder();
	}

	/**
	 * Parses a query using default settings (backward compatibility).
	 * @param query the query string to parse
	 * @return the root node of the parsed query
	 * @deprecated Use Query.parse(query) or QueryParser.builder().build().parse(query)
	 * instead
	 */
	@Deprecated(since = "2.0", forRemoval = true)
	@SuppressWarnings("removal")
	public static RootNode parseQuery(String query) {
		List<Token> tokens = legacyTokenize(query);
		LegacyQueryParser legacyParser = new LegacyQueryParser(tokens);
		return legacyParser.parse();
	}

	/**
	 * Legacy tokenization method for backward compatibility.
	 * @param input the input string
	 * @return list of tokens
	 * @deprecated Use QueryLexer.defaultLexer().tokenize() instead
	 */
	@Deprecated(since = "2.0", forRemoval = true)
	@SuppressWarnings("removal")
	private static List<Token> legacyTokenize(String input) {
		// Simple legacy tokenization for backward compatibility
		LegacyQueryLexer lexer = new LegacyQueryLexer(input);
		return lexer.tokenize();
	}

	/**
	 * Builder for QueryParser.
	 */
	public static class Builder {

		private QueryLexer lexer;

		private ParserOptions options = ParserOptions.defaultOptions();

		private final Map<String, Function<String, Node>> fieldParsers = new HashMap<>();

		private Builder() {
		}

		public Builder lexer(QueryLexer lexer) {
			this.lexer = lexer;
			return this;
		}

		public Builder options(ParserOptions options) {
			this.options = Objects.requireNonNull(options, "options must not be null");
			return this;
		}

		public Builder defaultOperator(BooleanOperator operator) {
			this.options = options.withDefaultOperator(operator);
			return this;
		}

		public Builder validateAfterParse(boolean validate) {
			this.options = options.withValidateAfterParse(validate);
			return this;
		}

		public Builder throwOnValidationError(boolean throwOnError) {
			this.options = options.withThrowOnValidationError(throwOnError);
			return this;
		}

		public Builder fieldParser(String field, Function<String, Node> parser) {
			Objects.requireNonNull(field, "field must not be null");
			Objects.requireNonNull(parser, "parser must not be null");
			this.fieldParsers.put(field, parser);
			return this;
		}

		public Builder fieldParsers(Map<String, Function<String, Node>> parsers) {
			Objects.requireNonNull(parsers, "parsers must not be null");
			this.fieldParsers.putAll(parsers);
			return this;
		}

		public Builder allowedTokenTypes(TokenType... types) {
			this.options = options.withAllowedTokenTypes(Set.of(types));
			return this;
		}

		public Builder allowedTokenTypes(Set<TokenType> types) {
			this.options = options.withAllowedTokenTypes(types);
			return this;
		}

		public QueryParser build() {
			return new QueryParser(this);
		}

	}

	/**
	 * Boolean operators for queries.
	 */
	public enum BooleanOperator {

		AND, OR

	}

	/**
	 * Parser options.
	 */
	public record ParserOptions(BooleanOperator defaultOperator, boolean validateAfterParse,
			boolean throwOnValidationError, Set<TokenType> allowedTokenTypes) {

		public ParserOptions {
			Objects.requireNonNull(defaultOperator, "defaultOperator must not be null");
			Objects.requireNonNull(allowedTokenTypes, "allowedTokenTypes must not be null");
			allowedTokenTypes = EnumSet.copyOf(allowedTokenTypes);
		}

		public static ParserOptions defaultOptions() {
			return new ParserOptions(BooleanOperator.AND, false, false, EnumSet.allOf(TokenType.class));
		}

		public ParserOptions withDefaultOperator(BooleanOperator operator) {
			return new ParserOptions(operator, validateAfterParse, throwOnValidationError, allowedTokenTypes);
		}

		public ParserOptions withValidateAfterParse(boolean validate) {
			return new ParserOptions(defaultOperator, validate, throwOnValidationError, allowedTokenTypes);
		}

		public ParserOptions withThrowOnValidationError(boolean throwOnError) {
			return new ParserOptions(defaultOperator, validateAfterParse, throwOnError, allowedTokenTypes);
		}

		public ParserOptions withAllowedTokenTypes(Set<TokenType> types) {
			return new ParserOptions(defaultOperator, validateAfterParse, throwOnValidationError, types);
		}

	}

}