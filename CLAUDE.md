# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this
repository.

**Build Commands:**

```bash
./mvnw clean spring-javaformat:apply compile                    # Compile application
./mvnw spring-javaformat:apply test                             # Run all tests
```

## Project Overview

Query Parser v0.2.0 - A powerful and flexible query parsing library for Java that transforms search queries into structured Abstract Syntax Trees (AST).

**Current Version**: 0.2.0-SNAPSHOT  
**Total Tests**: 195 (all passing)  
**Main Package**: `am.ik.query`

## Architecture

### Core Components

1. **Query Parsing Pipeline**:
   - `QueryLexer` - Tokenization with support for advanced features
   - `QueryParser` - AST generation with configurable validation
   - `Query` - Immutable query object with fluent API

2. **AST Node Types** (Sealed Interface Hierarchy):
   - `RootNode` - Container for top-level expressions
   - `AndNode`, `OrNode`, `NotNode` - Boolean operations
   - `TokenNode` - Basic keywords and operators
   - `PhraseNode` - Quoted phrases ("hello world")
   - `FieldNode` - Field-specific searches (title:value)
   - `WildcardNode` - Pattern matching (spring*, wor?d)
   - `FuzzyNode` - Fuzzy matching (spring~2)
   - `RangeNode` - Range queries ([1 TO 10])

3. **Processing Framework**:
   - `NodeVisitor<T>` - Visitor pattern for AST traversal
   - `QueryTransformer` - Query transformation interface
   - `QueryNormalizer` - Query normalization (lowercase, etc.)
   - `QueryOptimizer` - Query optimization (remove duplicates, etc.)

4. **Validation System**:
   - `QueryValidator` - Comprehensive validation with token type restrictions
   - `ValidationResult` - Validation results with detailed error reporting
   - Flexible validation modes: strict, manual, performance

5. **Builder APIs**:
   - `QueryParser.Builder` - Parser configuration
   - `Query.Builder` - Programmatic query construction

## Implemented Features

### Query Syntax Support
- ✅ Boolean operators (AND, OR, NOT) with proper precedence
- ✅ Phrase queries with quoted strings ("hello world")
- ✅ Field queries (title:spring, author:"John Doe")
- ✅ Wildcard patterns (spring*, wor?d, *boot*)
- ✅ Fuzzy search (spring~, spring~2)
- ✅ Range queries ([1 TO 10], {start TO end})
- ✅ Exclusion operators (-deprecated)
- ✅ Required terms (+required)
- ✅ Boost queries (important^2)
- ✅ Complex nested expressions with parentheses

### Parser Features
- ✅ Configurable default operators (AND/OR)
- ✅ Token type restrictions for legacy compatibility
- ✅ Flexible validation (validateAfterParse, throwOnValidationError)
- ✅ Custom field parsers
- ✅ Thread-safe parser instances

### Query Processing
- ✅ Immutable Query objects
- ✅ Visitor pattern for extensible processing
- ✅ Query transformation framework
- ✅ Normalization and optimization
- ✅ Comprehensive extraction methods (keywords, phrases, fields, etc.)
- ✅ Metadata system with performance tracking

### Validation & Compatibility
- ✅ Comprehensive validation system with error reporting
- ✅ Legacy v0.1 compatibility mode
- ✅ Token type restrictions
- ✅ Validation behavior matrix documentation

### Testing & Examples
- ✅ 195 comprehensive tests covering all features
- ✅ Legacy compatibility tests
- ✅ SQL converter implementation using visitor pattern
- ✅ Blog search practical examples
- ✅ README examples tests

## Key Design Decisions

### Validation Configuration Matrix
| validateAfterParse | throwOnValidationError | Behavior |
|-------------------|----------------------|----------|
| `true` | `true` | **Strict mode**: Validate and throw exception on errors |
| `true` | `false` | **Manual mode**: Validate but allow error inspection |
| `false` | `false` | **Performance mode**: No validation during parsing |
| `false` | `true` | ❌ Invalid combination (ignored) |

### Builder Pattern Usage
- `QueryParser.Builder` - For parser configuration (validation, token types, etc.)
- `Query.Builder` - For programmatic query construction (inner class)

### Legacy Compatibility
- v0.1 compatibility maintained through token type restrictions
- Legacy test suites ensure backward compatibility
- Deprecated methods marked for removal in future versions

## Development Requirements

### Prerequisites

- Java 17+

### Code Standards

- No external dependencies except for testing libraries
- Use builder pattern if the number of arguments is more than two
- Write javadoc and comments in English
- Spring Java Format enforced via Maven plugin
- All code must pass formatting validation before commit
- Use Java 17 compatible features (avoid Java 21+ specific APIs)
- Use modern Java technics like Java Records, Pattern Matching, Text Block etc ...


### Testing Strategy

- JUnit 5 with AssertJ
- All tests must pass before completing tasks

### After Task completion

- Ensure all code is formatted using `./mvnw spring-javaformat:apply`
- Run full test suite with `./mvnw test`
- For every task, notify that the task is complete and ready for review by the following command:

```
osascript -e 'display notification "<Message Body>" with title "<Message Title>"’
```
