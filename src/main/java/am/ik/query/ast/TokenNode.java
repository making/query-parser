package am.ik.query.ast;

import am.ik.query.lexer.Token;
import am.ik.query.lexer.TokenType;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Represents a token node in the query AST. This is a leaf node containing a single
 * token.
 *
 * @author Toshiaki Maki
 */
public final class TokenNode implements Node {

	private final TokenType type;

	private final String value;

	private final Token token;

	private Node parent;

	public TokenNode(TokenType type, String value) {
		this.type = Objects.requireNonNull(type, "type must not be null");
		this.value = Objects.requireNonNull(value, "value must not be null");
		this.token = new Token(type, value);
	}

	public TokenNode(Token token) {
		this.token = Objects.requireNonNull(token, "token must not be null");
		this.type = token.type();
		this.value = token.value();
	}

	@Override
	public String value() {
		return value;
	}

	@Override
	public <T> T accept(NodeVisitor<T> visitor) {
		return visitor.visitToken(this);
	}

	@Override
	public void walk(Consumer<Node> consumer) {
		consumer.accept(this);
	}

	@Override
	public Node parent() {
		return parent;
	}

	@Override
	public void setParent(Node parent) {
		this.parent = parent;
	}

	public TokenType type() {
		return type;
	}

	public Token token() {
		return token;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (!(o instanceof TokenNode tokenNode))
			return false;
		return type == tokenNode.type && Objects.equals(value, tokenNode.value);
	}

	@Override
	public int hashCode() {
		return Objects.hash(type, value);
	}

	@Override
	public String toString() {
		return "TokenNode{type=" + type + ", value='" + value + "'}";
	}

}
