package am.ik.query;

import java.util.Objects;
import java.util.function.Consumer;

/**
 * Represents a wildcard query node in the AST. Supports * (zero or more chars) and ?
 * (single char) wildcards.
 *
 * @author Toshiaki Maki
 */
public final class WildcardNode implements Node {

	private final String pattern;

	private final Token token;

	private Node parent;

	public WildcardNode(String pattern, Token token) {
		this.pattern = Objects.requireNonNull(pattern, "pattern must not be null");
		this.token = Objects.requireNonNull(token, "token must not be null");
	}

	public WildcardNode(String pattern) {
		this(pattern, new Token(TokenType.WILDCARD, pattern));
	}

	@Override
	public String value() {
		return pattern;
	}

	@Override
	public <T> T accept(NodeVisitor<T> visitor) {
		return visitor.visitWildcard(this);
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

	public String pattern() {
		return pattern;
	}

	public Token token() {
		return token;
	}

	/**
	 * Checks if this wildcard pattern contains any wildcard characters.
	 * @return true if pattern contains * or ?
	 */
	public boolean hasWildcards() {
		return pattern.contains("*") || pattern.contains("?");
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (!(o instanceof WildcardNode that))
			return false;
		return Objects.equals(pattern, that.pattern);
	}

	@Override
	public int hashCode() {
		return Objects.hash(pattern);
	}

	@Override
	public String toString() {
		return "WildcardNode{pattern='" + pattern + "'}";
	}

}