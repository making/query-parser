# Query Parser

A powerful and flexible query parsing library for Java that transforms search queries into structured Abstract Syntax Trees (AST).

## Features

- **Advanced Query Syntax**: Support for boolean operators (AND, OR, NOT), phrases, wildcards, fuzzy search, field queries, and range queries
- **Fluent Builder API**: Modern, type-safe API for parser configuration
- **AST-based**: Generates a traversable Abstract Syntax Tree for advanced query manipulation
- **Query Optimization**: Built-in optimizers for query simplification and performance
- **Query Validation**: Comprehensive validation with detailed error reporting
- **Extensible**: Custom field parsers and node visitors for domain-specific requirements
- **Zero Dependencies**: No external runtime dependencies (test dependencies only)

## Requirements

- Java 17 or higher
- Maven 3.6 or higher

## Installation

Add the following dependency to your `pom.xml`:

```xml
<dependency>
    <groupId>am.ik.query</groupId>
    <artifactId>query-parser</artifactId>
    <version>0.2.0-SNAPSHOT</version>
</dependency>
```

## Quick Start

### Basic Usage

```java
import am.ik.query.Query;

// Parse a simple query
Query query = Query.parse("java AND (spring OR boot)");

// Extract keywords
List<String> keywords = query.extractKeywords();  // ["java", "spring", "boot"]

// Check query properties
boolean hasAnd = query.hasAndOperations();        // true
boolean hasOr = query.hasOrOperations();          // true
```

### Using the Parser Builder

```java
import am.ik.query.QueryParser;
import am.ik.query.QueryParser.BooleanOperator;

// Create a customized parser
QueryParser parser = QueryParser.builder()
    .defaultOperator(BooleanOperator.AND)  // Default operator between terms
    .validateAfterParse(true)              // Validate query after parsing
    .throwOnValidationError(true)          // Throw exception on validation error
    .build();

Query query = parser.parse("java spring boot");  // Interpreted as: java AND spring AND boot
// Note: hasAndOperations() returns true when AND operations exist (explicit or implicit)
```

## Query Syntax

### Boolean Operators

```java
// Explicit operators
Query.parse("java AND spring");
Query.parse("java OR kotlin");
Query.parse("java NOT android");  // NOT creates exclusions (same as -)

// Implicit AND (default behavior)
Query.parse("java spring");  // Same as: java AND spring

// Complex boolean expressions
Query.parse("(java OR kotlin) AND (spring OR boot) NOT legacy");
```

### Phrases

```java
// Exact phrase matching
Query.parse("\"hello world\"");

// Phrases in complex queries  
Query.parse("\"Spring Boot\" AND \"Josh Long\"");

// Note: Field phrases are extracted via extractFields(), not extractPhrases()
```

### Wildcards

```java
// ? matches single character, * matches multiple characters
Query.parse("spr?ng");      // Matches: spring, sprang
Query.parse("spring*");     // Matches: spring, springframework, springboot
Query.parse("*boot*");      // Matches: boot, springboot, bootstrap
```

### Fuzzy Search

```java
// Default edit distance (2)
Query.parse("spring~");

// Specific edit distance
Query.parse("spring~1");    // Maximum 1 character difference
```

### Field Queries

```java
// Field-specific search
Query.parse("title:spring");
Query.parse("author:\"John Doe\"");  // Field phrases work correctly
Query.parse("date:2024 AND status:published");
```

### Range Queries

```java
// Inclusive ranges
Query.parse("[1 TO 10]");          // 1 <= x <= 10

// Exclusive ranges
Query.parse("{1 TO 10}");          // 1 < x < 10

// Mixed ranges
Query.parse("[1 TO 10}");          // 1 <= x < 10

// Note: Field-specific ranges like "price:[100 TO 500]" are not supported
// Use separate field queries instead
```

### Exclusions

```java
// Exclude terms
Query.parse("java -android");      // Java but not Android
Query.parse("spring -legacy -deprecated");  // Individual exclusions work better than grouped
```

## Advanced Features

### Query Traversal

```java
Query query = Query.parse("java AND (spring OR boot)");

// Walk through all nodes
query.walk(node -> {
    System.out.println(node.getClass().getSimpleName() + ": " + node.value());
});

// Use visitor pattern
String result = query.accept(new NodeVisitor<String>() {
    @Override
    public String visitAnd(AndNode node) {
        return "AND(" + node.children().stream()
            .map(child -> child.accept(this))
            .collect(Collectors.joining(", ")) + ")";
    }
    
    @Override
    public String visitToken(TokenNode node) {
        return node.value();
    }
    // ... implement other visit methods
});
```

### Query Transformation

```java
// Normalize query (lowercase, sort terms, normalize whitespace)
Query normalized = query.normalize();

// Optimize query (remove duplicates, flatten nested operations)
Query optimized = query.optimize();

// Chain transformations
Query transformed = query
    .transform(QueryNormalizer.toLowerCase())
    .transform(QueryOptimizer.removeDuplicates())
    .transform(QueryOptimizer.simplifyBooleans());
```

### Query Validation

The query parser provides flexible validation options to suit different use cases. You can control when validation occurs and how errors are handled.

#### Validation Configuration Options

```java
// Option 1: Strict validation (recommended for production)
QueryParser strictParser = QueryParser.builder()
    .validateAfterParse(true)           // Enable validation
    .throwOnValidationError(true)       // Throw exception on errors
    .allowedTokenTypes(TokenType.KEYWORD, TokenType.PHRASE, TokenType.OR, TokenType.AND)
    .build();

// Option 2: Validation with manual error handling (useful for testing/debugging)
QueryParser testParser = QueryParser.builder()
    .validateAfterParse(true)           // Enable validation
    .throwOnValidationError(false)      // Don't throw, allow manual error checking
    .allowedTokenTypes(TokenType.KEYWORD, TokenType.PHRASE)
    .build();

// Option 3: No automatic validation (maximum performance)
QueryParser performanceParser = QueryParser.builder()
    .validateAfterParse(false)          // Disable validation
    .build();
```

#### Validation Behavior Matrix

| validateAfterParse | throwOnValidationError | Behavior |
|-------------------|----------------------|----------|
| `true` | `true` | **Strict mode**: Validate and throw exception on errors |
| `true` | `false` | **Manual mode**: Validate but allow error inspection |
| `false` | `false` | **Performance mode**: No validation during parsing |
| `false` | `true` | ❌ Invalid combination (ignored) |

#### Manual Validation

```java
// Create a parser that doesn't validate during parsing
QueryParser parser = QueryParser.builder().validateAfterParse(false).build();
Query query = parser.parse("hello");  // Simple valid query

ValidationResult result = query.validate();

if (!result.isValid()) {
    result.errors().forEach(error -> {
        System.err.println(error.message());
        if (error.field() != null) {
            System.err.println("Field: " + error.field());
        }
        if (error.invalidValue() != null) {
            System.err.println("Invalid value: " + error.invalidValue());
        }
    });
} else {
    System.out.println("Query is valid");
}
```

#### Testing Advanced Features Rejection

```java
// Test that advanced features are properly rejected in legacy mode
QueryParser legacyParser = QueryParser.builder()
    .allowedTokenTypes(TokenType.KEYWORD, TokenType.PHRASE, TokenType.OR, TokenType.AND)
    .validateAfterParse(true)
    .throwOnValidationError(false)  // Don't throw for testing
    .build();

Query query = legacyParser.parse("title:spring");  // Field query not allowed
ValidationResult result = QueryValidator.validate(query, 
    Set.of(TokenType.KEYWORD, TokenType.PHRASE, TokenType.OR, TokenType.AND));

assertThat(result.isValid()).isFalse();
assertThat(result.errors().get(0).message()).contains("FIELD");
```

#### Use Cases

- **Production systems**: Use `validateAfterParse(true) + throwOnValidationError(true)` for immediate error detection
- **Testing environments**: Use `validateAfterParse(true) + throwOnValidationError(false)` to inspect validation errors
- **High-performance scenarios**: Use `validateAfterParse(false)` when validation overhead is not acceptable
- **Legacy compatibility**: Combine with `allowedTokenTypes()` to restrict parser to specific feature sets

### Custom Field Parsers

```java
QueryParser parser = QueryParser.builder()
    .fieldParser("date", value -> {
        // Custom parsing logic for date fields
        LocalDate date = LocalDate.parse(value);
        return new TokenNode(TokenType.KEYWORD, date.toString());
    })
    .fieldParser("price", value -> {
        // Custom parsing logic for price fields
        BigDecimal price = new BigDecimal(value);
        return new TokenNode(TokenType.KEYWORD, price.toString());
    })
    .build();

// Note: Custom field parsers may need additional configuration to work properly
```

### Token Type Restrictions

```java
// Allow only specific token types
QueryParser parser = QueryParser.builder()
    .allowedTokenTypes(TokenType.KEYWORD, TokenType.PHRASE, TokenType.AND)
    .throwOnValidationError(true)  // Required to enforce restrictions
    .build();

// Queries with OR, NOT, wildcards, etc. will be rejected during validation
// Note: Token type restrictions may require additional validation setup
```

## Query Analysis

```java
Query query = Query.parse("title:spring AND (java OR kotlin) -deprecated author:john*");

// Extract different components
List<String> keywords = query.extractKeywords();           // ["java", "kotlin"]
List<String> phrases = query.extractPhrases();             // []
List<String> wildcards = query.extractWildcards();         // ["john*"]
List<String> exclusions = query.extractExclusions();       // ["deprecated"]
Map<String, List<String>> fields = query.extractFields();  // {"title": ["spring"], "author": ["john*"]}

// Get metadata
QueryMetadata metadata = query.metadata();
int tokenCount = metadata.tokenCount();
int nodeCount = metadata.nodeCount();
int maxDepth = metadata.maxDepth();
Duration parseTime = metadata.parseTime();
```

## Performance Considerations

- The parser is designed to handle complex queries efficiently
- Query optimization can significantly reduce the complexity of boolean expressions
- Use token type restrictions to improve parsing performance for specific use cases
- The AST structure allows for efficient query analysis and transformation

## Thread Safety

- `QueryParser` instances are thread-safe and can be reused
- `Query` objects are immutable and thread-safe
- All transformation operations return new `Query` instances
- Note: Transformations may return the same instance if no changes are made
- Example: `normalize()` only changes case and whitespace, so "original" stays "original"

## Development

### Building from Source

```bash
git clone https://github.com/making/query-parser.git
cd query-parser
./mvnw clean install
```

### Running Tests

```bash
./mvnw test
```

### Code Formatting

The project uses Spring Java Format. Format code before committing:

```bash
./mvnw spring-javaformat:apply
```

## License

Licensed under the Apache License, Version 2.0. See [LICENSE](LICENSE) file for details.

## v0.1 Compatibility Mode

If you need to restrict the parser to the same feature set as v0.1 (legacy version), you can configure it to only allow basic query syntax:

### Configuring Legacy-Compatible Parser

```java
// Create parser with v0.1 limitations
QueryParser legacyCompatibleParser = QueryParser.builder()
    .allowedTokenTypes(
        TokenType.KEYWORD,    // Basic keywords
        TokenType.PHRASE,     // "quoted phrases"
        TokenType.EXCLUDE,    // -excluded terms
        TokenType.OR,         // OR operator
        TokenType.AND,        // AND operator
        TokenType.NOT,        // NOT operator
        TokenType.LPAREN,     // (
        TokenType.RPAREN,     // )
        TokenType.WHITESPACE, // Whitespace
        TokenType.EOF         // End of file
    )
    .validateAfterParse(true)      // Enable validation
    .throwOnValidationError(true)  // Throw exception on validation error
    .build();
```

### Supported Features in v0.1 Mode

The legacy-compatible parser supports only these features:

```java
// ✅ These work in v0.1 compatibility mode
legacyCompatibleParser.parse("hello world");                    // Keywords (implicit AND)
legacyCompatibleParser.parse("java spring boot");              // Multiple keywords (implicit AND)
legacyCompatibleParser.parse("\"Spring Boot\"");               // Phrases
legacyCompatibleParser.parse("java AND spring");               // Explicit AND operator
legacyCompatibleParser.parse("java OR kotlin");                // OR operator
legacyCompatibleParser.parse("java NOT android");              // NOT operator
legacyCompatibleParser.parse("spring -deprecated");            // Exclusions
legacyCompatibleParser.parse("(java OR kotlin) AND spring");   // Grouping
```

### Rejected Features in v0.1 Mode

These advanced features will throw `QueryValidationException`:

```java
// ❌ These are rejected in v0.1 compatibility mode
legacyCompatibleParser.parse("title:hello");        // Field queries
legacyCompatibleParser.parse("spring*");           // Wildcards
legacyCompatibleParser.parse("hello~2");           // Fuzzy search
legacyCompatibleParser.parse("[1 TO 10]");         // Range queries
legacyCompatibleParser.parse("important^2");       // Boost queries
legacyCompatibleParser.parse("+required");         // Required terms
```

### Use Cases

Use v0.1 compatibility mode when:
- Migrating from the legacy query parser
- Want to restrict users to basic search syntax
- Implementing a simplified search interface where users expect space-separated terms to be AND'ed together
- Building content-only search systems like blog article search

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

## Author

Toshiaki Maki (@making)