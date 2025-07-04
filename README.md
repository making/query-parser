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

## Installation

Add the following dependency to your `pom.xml`:

```xml
<dependency>
    <groupId>am.ik.query</groupId>
    <artifactId>query-parser</artifactId>
    <version>0.2.0</version>
</dependency>
```

## Quick Start

### Basic Usage

```java
import am.ik.query.Query;
import am.ik.query.parser.QueryParser;
import am.ik.query.util.QueryPrinter;

// Parse a simple query
Query query = QueryParser.create().parse("java AND (spring OR boot)");

// Extract keywords
List<String> keywords = query.extractKeywords();  // ["java", "spring", "boot"]

System.out.println(QueryPrinter.toPrettyString(query));
// Query: java AND (spring OR boot)
// AST:
// └─ AndNode (2 children)
//   └─ TokenNode[KEYWORD]: "java"
//   └─ OrNode (2 children)
//     └─ TokenNode[KEYWORD]: "spring"
//     └─ TokenNode[KEYWORD]: "boot"
```

### Using the Parser Builder

```java
import am.ik.query.parser.QueryParser;
import am.ik.query.parser.QueryParser.BooleanOperator;

// Create a customized parser
QueryParser parser = QueryParser.builder()
    .defaultOperator(BooleanOperator.AND)  // Default operator between terms (default: AND)
    .validateAfterParse(true)              // Validate query after parsing (default: false)
    .throwOnValidationError(true)          // Throw exception on validation error (default: false)
    .build();

Query query = parser.parse("java spring boot");  // Interpreted as: java AND spring AND boot
```

#### Builder Configuration Options

| Method | Description | Default Value |
|--------|-------------|---------------|
| `defaultOperator(BooleanOperator)` | Operator used between terms when no explicit operator is given | `BooleanOperator.AND` |
| `validateAfterParse(boolean)` | Whether to validate the query after parsing | `false` |
| `throwOnValidationError(boolean)` | Whether to throw exceptions on validation errors | `false` |
| `allowedTokenTypes(TokenType...)` | Token types allowed during validation | All `TokenType` values |
| `fieldParser(String, Function)` | Custom parsing logic for specific field names | No custom parsers |
| `lexer(QueryLexer)` | Custom lexer for tokenization | `QueryLexer.defaultLexer()` |

## Query Syntax

### Boolean Operators

```java
QueryParser queryParser = QueryParser.create();

// Explicit operators
queryParser.parse("java AND spring");
queryParser.parse("java OR kotlin");

// Implicit AND (default behavior)
queryParser.parse("java spring");  // Same as: java AND spring

// Complex boolean expressions
queryParser.parse("(java OR kotlin) AND (spring OR boot)");
```

### Negation: NOT vs - (Exclusion)

There are two ways to exclude terms, with important differences:

```java
// Method 1: NOT operator (can negate complex expressions)
queryParser.parse("java NOT android");              // Single term negation
queryParser.parse("java NOT (android OR ios)");     // Group negation - excludes documents containing EITHER android OR ios
queryParser.parse("java NOT \"mobile development\""); // Phrase negation

// Method 2: - prefix (excludes individual terms only)
queryParser.parse("java -android");                 // Single term exclusion (same as NOT for single terms)
queryParser.parse("java -android -ios");            // Multiple individual exclusions - excludes android AND ios separately
queryParser.parse("java -\"mobile development\"");  // ⚠️ Syntax error - use NOT for phrases

// Key differences:
// - NOT can negate complex expressions in parentheses
// - (minus) can only exclude individual terms or simple phrases
// - For multiple exclusions: NOT (a OR b) ≠ -a -b
//   NOT (a OR b) excludes documents with EITHER a OR b
//   -a -b excludes documents with a AND excludes documents with b
```

### Phrases

```java
// Exact phrase matching
queryParser.parse("\"hello world\"");

// Phrases in complex queries  
queryParser.parse("\"Spring Boot\" AND \"Josh Long\"");

// Note: Field phrases are extracted via extractFields(), not extractPhrases()
```

### Wildcards

```java
// ? matches single character, * matches multiple characters
queryParser.parse("spr?ng");      // Matches: spring, sprang
queryParser.parse("spring*");     // Matches: spring, springframework, springboot
queryParser.parse("*boot*");      // Matches: boot, springboot, bootstrap
```

### Fuzzy Search

```java
// Default edit distance (2)
queryParser.parse("spring~");

// Specific edit distance
queryParser.parse("spring~1");    // Maximum 1 character difference
```

### Field Queries

```java
// Field-specific search
queryParser.parse("title:spring");
queryParser.parse("author:\"John Doe\"");  // Field phrases work correctly
queryParser.parse("date:2024 AND status:published");
```

### Range Queries

```java
// Inclusive ranges
queryParser.parse("[1 TO 10]");          // 1 <= x <= 10

// Exclusive ranges
queryParser.parse("{1 TO 10}");          // 1 < x < 10

// Mixed ranges
queryParser.parse("[1 TO 10}");          // 1 <= x < 10

// Note: Field-specific ranges like "price:[100 TO 500]" are not supported
// Use separate field queries instead
```

### Exclusions (Additional Examples)

```java
// Simple exclusions using - prefix
queryParser.parse("java -android");                    // Java but not Android
queryParser.parse("spring -legacy -deprecated");       // Spring but not legacy and not deprecated

// For complex exclusions, prefer NOT operator
queryParser.parse("java NOT (android OR mobile)");     // Java but not (android OR mobile)
queryParser.parse("spring NOT \"legacy code\"");       // Spring but not the phrase "legacy code"

// These are different:
// "java NOT (android OR ios)" - excludes documents containing EITHER android OR ios  
// "java -android -ios" - excludes documents containing android AND also excludes documents containing ios
```

## Advanced Features

### Query Traversal

```java
Query query = QueryParser.create().parse("java AND (spring OR boot)");

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
Query normalized = query.transform(QueryNormalizer.defaultNormalizer());

// Optimize query (remove duplicates, flatten nested operations)
Query optimized = query.transform(QueryOptimizer.defaultOptimizer());

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

ValidationResult result = QueryValidator.validate(query);

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
    .allowedTokenTypes(TokenType.KEYWORD, TokenType.PHRASE, TokenType.AND)  // (default: all TokenType values)
    .validateAfterParse(true)      // Required to enable validation (default: false)
    .throwOnValidationError(true)  // Required to enforce restrictions (default: false)
    .build();

// Queries with OR, NOT, wildcards, etc. will be rejected during validation
```

## Practical Example: SQL Converter

Here's a simple example of converting queries to parameterized SQL WHERE clauses for content search:

```java
import am.ik.query.ast.*;
import am.ik.query.lexer.TokenType;
import am.ik.query.visitor.NodeVisitor;
import java.util.*;
import java.util.stream.Collectors;

public class SimpleContentSqlConverter implements NodeVisitor<String> {
    
    // Result record containing parameterized SQL and parameters
    public record SqlResult(String whereClause, Map<String, Object> parameters) {}
    
    private final Map<String, Object> parameters = new HashMap<>();
    private int paramCounter = 1;
    
    public SqlResult convertToSql(Query query) {
        parameters.clear();
        paramCounter = 1;
        
        if (query.isEmpty()) {
            return new SqlResult("1=1", Map.of());  // Always true condition
        }
        
        String sql = query.accept(this);
        return new SqlResult(sql, parameters);
    }

    @Override
    public String visitRoot(RootNode node) {
        return node.children().stream()
            .map(child -> child.accept(this))
            .filter(sql -> !sql.isEmpty())
            .collect(Collectors.joining(" AND "));
    }

    @Override
    public String visitAnd(AndNode node) {
        String result = node.children().stream()
            .map(child -> child.accept(this))
            .filter(sql -> !sql.isEmpty())
            .collect(Collectors.joining(" AND "));
        return node.children().size() > 1 ? "(" + result + ")" : result;
    }

    @Override
    public String visitOr(OrNode node) {
        String result = node.children().stream()
            .map(child -> child.accept(this))
            .filter(sql -> !sql.isEmpty())
            .collect(Collectors.joining(" OR "));
        return "(" + result + ")";
    }

    @Override
    public String visitNot(NotNode node) {
        // Handle NOT of TokenNode as exclusion (generates NOT LIKE directly)
        if (node.child() instanceof TokenNode tokenNode && tokenNode.type() == TokenType.KEYWORD) {
            return createLikeClause("content", tokenNode.value(), true);
        }
        
        String childSql = node.child().accept(this);
        return childSql.isEmpty() ? "" : "NOT " + childSql;
    }

    @Override
    public String visitToken(TokenNode node) {
        return switch (node.type()) {
            case KEYWORD -> createLikeClause("content", node.value(), false);
            case EXCLUDE -> createLikeClause("content", node.value(), true);
            default -> "";
        };
    }

    @Override
    public String visitPhrase(PhraseNode node) {
        return createLikeClause("content", node.phrase(), false);
    }
    
    private String createLikeClause(String column, String value, boolean negated) {
        String paramName = "param" + paramCounter++;
        parameters.put(paramName, "%" + value + "%");
        String operator = negated ? "NOT LIKE" : "LIKE";
        return column + " " + operator + " :" + paramName;
    }

    // Ignore field queries, wildcards, etc. for this simple example
    @Override public String visitField(FieldNode node) { return ""; }
    @Override public String visitWildcard(WildcardNode node) { return ""; }
    @Override public String visitFuzzy(FuzzyNode node) { return ""; }
    @Override public String visitRange(RangeNode node) { return ""; }
}

// Usage example:
QueryParser parser = QueryParser.create();
SimpleContentSqlConverter converter = new SimpleContentSqlConverter();

Query query1 = parser.parse("java spring");
SqlResult result1 = converter.convertToSql(query1);
// result1.whereClause(): "(content LIKE :param1 AND content LIKE :param2)"
// result1.parameters(): {"param1": "%java%", "param2": "%spring%"}

Query query2 = parser.parse("(java OR kotlin) -deprecated");  
SqlResult result2 = converter.convertToSql(query2);
// result2.whereClause(): "((content LIKE :param1 OR content LIKE :param2) AND content NOT LIKE :param3)"
// result2.parameters(): {"param1": "%java%", "param2": "%kotlin%", "param3": "%deprecated%"}

Query query3 = parser.parse("\"Spring Boot\"");
SqlResult result3 = converter.convertToSql(query3);
// result3.whereClause(): "content LIKE :param1"
// result3.parameters(): {"param1": "%Spring Boot%"}
```

## Query Analysis

```java
Query query = QueryParser.create().parse("title:spring AND (java OR kotlin) -deprecated author:john*");

// Extract different components
List<String> keywords = query.extractKeywords();           // ["java", "kotlin"]
List<String> phrases = query.extractPhrases();             // []
List<String> wildcards = query.extractWildcards();         // ["john*"]
List<String> exclusions = query.extractExclusions();       // ["deprecated"]
Map<String, List<String>> fields = query.extractFields();  // {"title": ["spring"], "author": ["john*"]}
```

## AST Visualization with QueryPrinter

The `QueryPrinter` utility provides a convenient way to visualize the Abstract Syntax Tree (AST) structure of parsed queries:

```java
import am.ik.query.Query;
import am.ik.query.parser.QueryParser;
import am.ik.query.util.QueryPrinter;

QueryParser parser = QueryParser.create();

// Simple AST example
Query simpleQuery = parser.parse("java spring");
System.out.println(QueryPrinter.toPrettyString(simpleQuery));
```

**Output:**
```
Query: java spring
AST:
└─ AndNode (2 children)
  └─ TokenNode[KEYWORD]: "java"
  └─ TokenNode[KEYWORD]: "spring"
```

### Complex AST Example

```java
// Complex query with nested operations
Query complexQuery = parser.parse("(\"Spring Boot\" OR java*) AND -deprecated AND title:framework NOT (legacy OR old)");
System.out.println(QueryPrinter.toPrettyString(complexQuery));
```

**Output:**
```
Query: ("Spring Boot" OR java*) AND -deprecated AND title:framework NOT (legacy OR old)
AST:
└─ AndNode (2 children)
  └─ AndNode (3 children)
    └─ OrNode (2 children)
      └─ PhraseNode: "Spring Boot"
      └─ WildcardNode: "java*"
    └─ NotNode (1 children)
      └─ TokenNode[KEYWORD]: "deprecated"
    └─ FieldNode: title="framework"
  └─ NotNode (1 children)
    └─ OrNode (2 children)
      └─ TokenNode[KEYWORD]: "legacy"
      └─ TokenNode[KEYWORD]: "old"
```

### Advanced Features AST

```java
// Query with fuzzy search, range, and field queries
Query advancedQuery = parser.parse("spring~2 AND [1 TO 10] AND author:john");
System.out.println(QueryPrinter.toPrettyString(advancedQuery));
```

**Output:**
```
Query: spring~2 AND [1 TO 10] AND author:john
AST:
└─ AndNode (3 children)
  └─ FuzzyNode: "spring" ~2
  └─ RangeNode: [1 TO 10]
  └─ FieldNode: author="john"
```

### Node Types in AST Output

The QueryPrinter displays different node types with specific formatting:

- **TokenNode[TYPE]**: Basic keywords with their token type
- **PhraseNode**: Quoted phrases
- **WildcardNode**: Patterns with * or ? wildcards
- **FuzzyNode**: Terms with fuzzy matching (~)
- **FieldNode**: Field-specific queries (field:value)
- **RangeNode**: Range queries with [start TO end] syntax
- **AndNode/OrNode**: Boolean operations with child count
- **NotNode**: Negation operations

This visualization is particularly useful for:
- **Debugging complex queries** and understanding parse results
- **Learning the query syntax** by seeing how different inputs are structured
- **Developing custom visitors** by understanding the AST hierarchy
- **Query optimization** by identifying redundant or complex structures

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
- Example: normalizer only changes case and whitespace, so "original" stays "original"

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

## License

Licensed under the Apache License, Version 2.0. See [LICENSE](LICENSE) file for details.
