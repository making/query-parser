package am.ik.query;

import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Represents an AND operation node in the query AST. All children must match for this
 * node to match.
 *
 * @author Toshiaki Maki
 */
public final class AndNode implements Node {

	private final List<Node> children;

	private Node parent;

	public AndNode(List<Node> children) {
		this.children = List.copyOf(Objects.requireNonNull(children, "children must not be null"));
		this.children.forEach(child -> child.setParent(this));
	}

	@Override
	public String value() {
		return "AND";
	}

	@Override
	public <T> T accept(NodeVisitor<T> visitor) {
		return visitor.visitAnd(this);
	}

	@Override
	public void walk(Consumer<Node> consumer) {
		consumer.accept(this);
		children.forEach(child -> child.walk(consumer));
	}

	@Override
	public Node parent() {
		return parent;
	}

	@Override
	public void setParent(Node parent) {
		this.parent = parent;
	}

	@Override
	public List<Node> children() {
		return children;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (!(o instanceof AndNode andNode))
			return false;
		return Objects.equals(children, andNode.children);
	}

	@Override
	public int hashCode() {
		return Objects.hash(children);
	}

	@Override
	public String toString() {
		return "AndNode{children=" + children + '}';
	}

}