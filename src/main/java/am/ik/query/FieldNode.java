package am.ik.query;

import java.util.Objects;
import java.util.function.Consumer;

/**
 * Represents a field query node in the AST. Format: field:value
 *
 * @author Toshiaki Maki
 */
public final class FieldNode implements Node {

	private final String field;

	private final String value;

	private final Token token;

	private Node parent;

	public FieldNode(String field, String value, Token token) {
		this.field = Objects.requireNonNull(field, "field must not be null");
		this.value = Objects.requireNonNull(value, "value must not be null");
		this.token = Objects.requireNonNull(token, "token must not be null");
	}

	public FieldNode(String field, String value) {
		this(field, value, new Token(TokenType.FIELD, field + ":" + value));
	}

	@Override
	public String value() {
		return field + ":" + value;
	}

	@Override
	public <T> T accept(NodeVisitor<T> visitor) {
		return visitor.visitField(this);
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

	public String field() {
		return field;
	}

	public String fieldValue() {
		return value;
	}

	public Token token() {
		return token;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (!(o instanceof FieldNode fieldNode))
			return false;
		return Objects.equals(field, fieldNode.field) && Objects.equals(value, fieldNode.value);
	}

	@Override
	public int hashCode() {
		return Objects.hash(field, value);
	}

	@Override
	public String toString() {
		return "FieldNode{field='" + field + "', value='" + value + "'}";
	}

}