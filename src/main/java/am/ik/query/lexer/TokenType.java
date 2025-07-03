package am.ik.query.lexer;

/**
 * Enumeration of token types supported by the query parser.
 *
 * @author Toshiaki Maki
 */
public enum TokenType {

	/**
	 * A quoted phrase like "hello world"
	 */
	PHRASE,

	/**
	 * An excluded term prefixed with - like -world
	 */
	EXCLUDE,

	/**
	 * The OR boolean operator
	 */
	OR,

	/**
	 * The AND boolean operator
	 */
	AND,

	/**
	 * The NOT boolean operator
	 */
	NOT,

	/**
	 * A regular keyword
	 */
	KEYWORD,

	/**
	 * A field:value pair like title:hello
	 */
	FIELD,

	/**
	 * A wildcard character * or ?
	 */
	WILDCARD,

	/**
	 * A fuzzy search indicator ~
	 */
	FUZZY,

	/**
	 * A boost indicator ^
	 */
	BOOST,

	/**
	 * A range query indicator [ or ]
	 */
	RANGE_START,

	/**
	 * A range query end indicator [ or ]
	 */
	RANGE_END,

	/**
	 * The TO keyword in range queries
	 */
	RANGE_TO,

	/**
	 * Whitespace characters
	 */
	WHITESPACE,

	/**
	 * Left parenthesis (
	 */
	LPAREN,

	/**
	 * Right parenthesis )
	 */
	RPAREN,

	/**
	 * Plus sign for required terms +
	 */
	REQUIRED,

	/**
	 * Colon separator for field queries :
	 */
	COLON,

	/**
	 * End of input marker
	 */
	EOF;

	/**
	 * Checks if this token type represents a boolean operator.
	 * @return true if this is OR, AND, or NOT
	 */
	public boolean isBooleanOperator() {
		return this == OR || this == AND || this == NOT;
	}

	/**
	 * Checks if this token type represents a term modifier.
	 * @return true if this is EXCLUDE, REQUIRED, WILDCARD, FUZZY, or BOOST
	 */
	public boolean isModifier() {
		return this == EXCLUDE || this == REQUIRED || this == WILDCARD || this == FUZZY || this == BOOST;
	}

	/**
	 * Checks if this token type represents actual content.
	 * @return true if this is KEYWORD, PHRASE, or FIELD
	 */
	public boolean isContent() {
		return this == KEYWORD || this == PHRASE || this == FIELD;
	}

	/**
	 * Checks if this token type is structural.
	 * @return true if this is LPAREN, RPAREN, or WHITESPACE
	 */
	public boolean isStructural() {
		return this == LPAREN || this == RPAREN || this == WHITESPACE || this == COLON || this == EOF;
	}

}
