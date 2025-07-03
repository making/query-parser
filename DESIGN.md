# Query Parser v0.2 - Detailed Design Document

## Overview

Query Parser v0.2 represents a complete architectural redesign of the query parsing library, introducing a modern, sophisticated API with enhanced functionality while maintaining backward compatibility through legacy compatibility modes.

## Architecture Overview

### Core Components

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   QueryLexer    │───▶│  QueryParser    │───▶│     Query       │
└─────────────────┘    └─────────────────┘    └─────────────────┘
         │                       │                       │
         ▼                       ▼                       ▼
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│     Tokens      │    │   AST Nodes     │    │   Fluent API    │
└─────────────────┘    └─────────────────┘    └─────────────────┘
```

## 1. Lexical Analysis (QueryLexer)

### Design Principles
- **Single Responsibility**: Focus solely on tokenization
- **Extensibility**: Support for new token types without major refactoring
- **Performance**: Efficient single-pass scanning
- **Robustness**: Graceful handling of malformed input

### Token Types

```java
public enum TokenType {
    // Basic tokens
    KEYWORD,     // hello, world
    PHRASE,      // "hello world"
    EXCLUDE,     // -unwanted
    
    // Boolean operators
    AND, OR, NOT,
    
    // Advanced features
    WILDCARD,    // spring*, wor?d
    FUZZY,       // hello~2
    FIELD,       // title:value
    RANGE,       // [1 TO 10]
    BOOST,       // important^2
    REQUIRED,    // +required
    
    // Structural
    LPAREN, RPAREN, COLON,
    RANGE_START, RANGE_END, RANGE_TO,
    WHITESPACE, EOF
}
```

### Lexical Rules

#### Word Character Recognition
```java
private boolean isWordChar(char c) {
    return Character.isLetterOrDigit(c) || c == '_' || c == '.' || c == '@' || c == '=' || c == '\\';
}
```

#### Special Handling for Quoted Strings in Keywords
The lexer handles complex patterns like `foo="bar"` as single keywords:

```java
// In keyword() method
while (isWordChar(peek()) || peek() == '"') {
    if (peek() == '"') {
        advance(); // consume opening quote
        while (peek() != '"' && !isAtEnd()) {
            advance();
        }
        if (peek() == '"') {
            advance(); // consume closing quote
        }
    } else {
        advance();
    }
}
```

#### Exclusion Operator Processing
```java
private void minus() {
    if (position > 1 && isWordChar(input.charAt(position - 2)) && isWordChar(peek())) {
        // Part of hyphenated word like "e-mail"
        keyword();
    } else if (isWordChar(peek())) {
        // Exclusion operator followed by a word
        // Create EXCLUDE token with the excluded word as value
    }
}
```

## 2. Syntactic Analysis (QueryParser)

### Design Philosophy
- **Recursive Descent**: Clean, maintainable parsing logic
- **Operator Precedence**: Proper handling of AND/OR precedence
- **Error Recovery**: Graceful handling of syntax errors
- **Configurability**: Flexible parsing behavior through builder pattern

### Grammar Definition

```
query           → or_expression
or_expression   → and_expression ( "OR" and_expression )*
and_expression  → not_expression ( ("AND" | implicit) not_expression )*
not_expression  → "NOT" not_expression | term
term            → group | field | phrase | wildcard | fuzzy | range | exclude | keyword
group           → "(" query ")"
field           → KEYWORD ":" term
phrase          → PHRASE
wildcard        → WILDCARD
fuzzy           → KEYWORD "~" NUMBER?
range           → ("[" | "{") term "TO" term ("]" | "}")
exclude         → EXCLUDE
keyword         → KEYWORD
```

### Parser Configuration

The parser supports extensive configuration through the builder pattern:

```java
QueryParser parser = QueryParser.builder()
    .defaultOperator(QueryParser.BooleanOperator.OR)
    .allowedTokenTypes(TokenType.KEYWORD, TokenType.PHRASE)
    .validateAfterParse(true)
    .throwOnValidationError(true)
    .build();
```

### Operator Behavior Configuration

#### `defaultOperator`
Controls how multiple terms are combined when parsed as a group:
- Determines the root node type (AndNode vs OrNode)
- Affects the logical relationship between terms
- `AND`: Terms must all match
- `OR`: Any term can match

#### Behavior Examples

For query `"hello world java"`:

| Configuration | Result AST Structure | Semantic Meaning |
|---------------|---------------------|------------------|
| `defaultOperator=AND` | `AndNode{hello, world, java}` | All terms must match |
| `defaultOperator=OR` | `OrNode{hello, world, java}` | Any term can match |

#### Practical Usage

**AND-biased (Recommended for most cases)**:
```java
// For precise matching - all terms must be present
QueryParser.builder()
    .defaultOperator(QueryParser.BooleanOperator.AND)
    .build();
```

**OR-biased (Search engine style)**:
```java
// For broader results - any term can match
QueryParser.builder()
    .defaultOperator(QueryParser.BooleanOperator.OR)
    .build();
```

#### Implementation Notes

The parser implements the default operator behavior by collecting terms and applying the operator:

```java
// Collect all terms first, then apply default operator
if (options.defaultOperator() == BooleanOperator.OR) {
    return new OrNode(nodes);
} else {
    return new AndNode(nodes);
}
```

This design provides clear semantics and consistent behavior across different use cases.

## 3. Abstract Syntax Tree (AST)

### Node Hierarchy

```java
public sealed interface Node permits RootNode, AndNode, OrNode, NotNode, 
                                    TokenNode, PhraseNode, FieldNode, 
                                    WildcardNode, FuzzyNode, RangeNode {
    String value();
    <T> T accept(NodeVisitor<T> visitor);
    void walk(Consumer<Node> visitor);
}
```

### Node Types and Purposes

#### Structural Nodes
- **RootNode**: Container for top-level expressions
- **AndNode**: Boolean AND operations
- **OrNode**: Boolean OR operations  
- **NotNode**: Negation operations

#### Terminal Nodes
- **TokenNode**: Basic keywords and operators
- **PhraseNode**: Quoted phrases with exact matching
- **FieldNode**: Field-specific searches (title:value)
- **WildcardNode**: Pattern matching with * and ?
- **FuzzyNode**: Fuzzy matching with edit distance
- **RangeNode**: Range queries [start TO end]

### Node Creation Strategy

The parser creates appropriate node types based on token sequences:

```java
private Node parseField() {
    Token fieldToken = advance(); // KEYWORD
    expect(TokenType.COLON);
    Node valueNode = parseTerm();
    return new FieldNode(fieldToken.value(), valueNode);
}
```

## 4. Query API

### Immutable Design
All Query objects are immutable, returning new instances for transformations:

```java
public final class Query {
    private final Node rootNode;
    private final QueryMetadata metadata;
    
    public Query normalize() { /* returns new Query */ }
    public Query optimize() { /* returns new Query */ }
    public Query transform(QueryTransformer transformer) { /* returns new Query */ }
}
```

### Fluent Extraction API

```java
// Keyword extraction
List<String> keywords = query.extractKeywords();

// Boolean operation checks
boolean hasAnd = query.hasAndOperations();
boolean hasOr = query.hasOrOperations();

// Advanced feature extraction
List<String> wildcards = query.extractWildcards();
Map<String, List<String>> fields = query.extractFields();
List<FuzzyTerm> fuzzyTerms = query.extractFuzzyTerms();
```

### Visitor Pattern Support

```java
public <T> T accept(NodeVisitor<T> visitor) {
    return rootNode.accept(visitor);
}

// Example usage
String sql = query.accept(new SqlWhereVisitor());
String elasticsearch = query.accept(new ElasticsearchVisitor());
```

## 5. Metadata System

### Query Metadata
```java
public record QueryMetadata(
    int tokenCount,
    int nodeCount, 
    int maxDepth,
    Duration parseTime,
    Instant parsedAt,
    Map<String, Object> properties
) {
    // Factory methods and utilities
}
```

### Usage Patterns
- **Performance Monitoring**: Track parsing times and complexity
- **Query Analysis**: Understand query structure and characteristics
- **Caching Keys**: Use metadata for intelligent caching strategies
- **Debugging**: Detailed information for troubleshooting

## 6. Validation System

### Validation Architecture

```java
public class QueryValidator {
    public static ValidationResult validate(Query query) {
        return validate(query, ValidationRules.DEFAULT);
    }
    
    public static ValidationResult validate(Query query, Set<TokenType> allowedTokenTypes) {
        // Validates both AST structure and original tokens
    }
}
```

### Validation Rules
- **Token Type Restrictions**: Limit available query features
- **Structural Validation**: Ensure AST consistency
- **Semantic Validation**: Check for logical query errors
- **Custom Rules**: Extensible validation framework

### Legacy Compatibility Validation
```java
Set<TokenType> v01Tokens = Set.of(
    TokenType.KEYWORD, TokenType.PHRASE, TokenType.EXCLUDE,
    TokenType.OR, TokenType.AND, TokenType.NOT,
    TokenType.LPAREN, TokenType.RPAREN,
    TokenType.WHITESPACE, TokenType.EOF
);

ValidationResult result = QueryValidator.validate(query, v01Tokens);
```

## 7. Transformation Framework

### Query Normalization
```java
public class QueryNormalizer {
    public static Query normalize(Query query) {
        return query.transform(new NormalizationTransformer());
    }
}
```

**Normalization Rules:**
- Convert keywords to lowercase
- Standardize boolean operators
- Remove redundant whitespace
- Canonical ordering of terms

### Query Optimization
```java
public class QueryOptimizer {
    public static Query optimize(Query query) {
        return query.transform(new OptimizationTransformer());
    }
}
```

**Optimization Strategies:**
- Remove double negations: `NOT NOT term` → `term`
- Eliminate empty clauses
- Flatten nested boolean operations
- Short-circuit evaluation opportunities

### Custom Transformations
```java
public interface QueryTransformer {
    Node transform(Node node);
}

// Example: Term rewriting
class SynonymTransformer implements QueryTransformer {
    public Node transform(Node node) {
        if (node instanceof TokenNode token && token.type() == TokenType.KEYWORD) {
            String synonym = synonymMap.get(token.value());
            return synonym != null ? new TokenNode(TokenType.KEYWORD, synonym) : node;
        }
        return node;
    }
}
```

## 8. Backward Compatibility

### Legacy Compatibility Mode

v0.2 provides full backward compatibility through configuration:

```java
QueryParser legacyCompatibleParser = QueryParser.builder()
    .allowedTokenTypes(TokenType.KEYWORD, TokenType.PHRASE, TokenType.EXCLUDE,
                      TokenType.OR, TokenType.AND, TokenType.NOT,
                      TokenType.LPAREN, TokenType.RPAREN,
                      TokenType.WHITESPACE, TokenType.EOF)
    .validateAfterParse(true)
    .throwOnValidationError(true)
    .build();
```

### Migration Strategy
1. **Phase 1**: Use legacy-compatible parser with validation
2. **Phase 2**: Gradually enable new features
3. **Phase 3**: Full migration to modern API

## 9. Advanced Features

### Field Queries
```java
// Syntax: field:value
Query query = parser.parse("title:\"Spring Boot\" author:Johnson");

// Extraction
Map<String, List<String>> fields = query.extractFields();
// Returns: {"title": ["Spring Boot"], "author": ["Johnson"]}
```

### Wildcard Searches
```java
// Patterns: * (any characters), ? (single character)
Query query = parser.parse("spring* boot?");

List<String> wildcards = query.extractWildcards();
// Returns: ["spring*", "boot?"]
```

### Fuzzy Matching
```java
// Syntax: term~[distance]
Query query = parser.parse("spring~2 boot~");

List<FuzzyTerm> fuzzyTerms = query.extractFuzzyTerms();
// Returns fuzzy terms with edit distances
```

### Range Queries
```java
// Syntax: [start TO end] or {start TO end}
Query query = parser.parse("date:[2020 TO 2023] price:{100 TO 500}");

List<RangeTerm> ranges = query.extractRanges();
// Returns range specifications with inclusive/exclusive bounds
```

### Boost and Required Terms
```java
// Boost: term^factor, Required: +term
Query query = parser.parse("important^2 +required optional");

List<BoostTerm> boosts = query.extractBoosts();
List<String> required = query.extractRequired();
```

## 10. Practical Applications

### SQL Generation
```java
class SqlWhereVisitor implements NodeVisitor<String> {
    public String visitField(FieldNode node) {
        return node.field() + " = '" + escapeSql(node.fieldValue()) + "'";
    }
    
    public String visitWildcard(WildcardNode node) {
        String pattern = node.pattern().replace('*', '%').replace('?', '_');
        return "content LIKE '" + escapeSql(pattern) + "'";
    }
}

String whereClause = query.accept(new SqlWhereVisitor());
```

### Elasticsearch Query Generation
```java
class ElasticsearchVisitor implements NodeVisitor<JsonObject> {
    public JsonObject visitFuzzy(FuzzyNode node) {
        return Json.createObjectBuilder()
            .add("fuzzy", Json.createObjectBuilder()
                .add(node.field(), Json.createObjectBuilder()
                    .add("value", node.term())
                    .add("fuzziness", node.distance())))
            .build();
    }
}
```

### Query Analysis and Metrics
```java
// Performance monitoring
QueryMetadata metadata = query.metadata();
logger.info("Parsed query with {} tokens in {}ms", 
           metadata.tokenCount(), metadata.parseTime().toMillis());

// Complexity analysis
if (metadata.maxDepth() > 5) {
    logger.warn("Complex query detected: depth={}", metadata.maxDepth());
}

// Feature usage tracking
if (query.hasWildcards()) {
    metrics.increment("wildcard_usage");
}
```

## 11. Error Handling

### Parse Errors
```java
try {
    Query query = parser.parse("invalid (( query");
} catch (QueryParseException e) {
    logger.error("Parse error at position {}: {}", e.getPosition(), e.getMessage());
}
```

### Validation Errors
```java
ValidationResult result = QueryValidator.validate(query, allowedTokens);
if (!result.isValid()) {
    for (ValidationError error : result.errors()) {
        logger.warn("Validation error: {}", error.message());
    }
}
```

## 12. Performance Considerations

### Memory Efficiency
- Immutable objects with structural sharing
- Lazy evaluation where possible
- Minimal object allocation during parsing

### Parsing Performance
- Single-pass lexical analysis
- Recursive descent with minimal backtracking
- Efficient string operations

### Optimization Strategies
- AST node interning for common patterns
- Metadata caching
- Visitor pattern for efficient transformations

## 13. Testing Strategy

### Test Categories
1. **Unit Tests**: Individual component behavior
2. **Integration Tests**: End-to-end parsing workflows
3. **Compatibility Tests**: Legacy behavior verification
4. **Performance Tests**: Benchmarking and regression detection

### Test Coverage Areas
- **Lexical Analysis**: All token types and edge cases
- **Parsing**: Grammar rules and error recovery
- **AST Operations**: Node creation and manipulation
- **API Behavior**: Fluent methods and transformations
- **Validation**: Rule enforcement and error reporting

## 14. Future Extensibility

### Plugin Architecture
The design supports future extensions through:
- Custom `NodeVisitor` implementations
- Pluggable `QueryTransformer` components
- Extensible validation rules
- Custom metadata providers

### Potential Enhancements
- **Query Suggestions**: Auto-completion and correction
- **Query Optimization**: Cost-based optimization
- **Caching Layer**: Intelligent query result caching
- **Analytics Integration**: Query pattern analysis
- **Multi-language Support**: Internationalization

## Conclusion

Query Parser v0.2 represents a significant advancement in query parsing capabilities while maintaining strict backward compatibility. The architecture emphasizes modularity, extensibility, and performance, providing a solid foundation for current and future requirements.

The design successfully balances sophistication with usability, offering both powerful advanced features and simple migration paths from legacy systems. The comprehensive validation and transformation frameworks ensure that the library can adapt to diverse use cases while maintaining consistent behavior and performance characteristics.