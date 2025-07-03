package am.ik.query.ast;

import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

import am.ik.query.visitor.NodeVisitor;

/**
 * Represents a NOT operation node in the query AST. The child must not match for this
 * node to match.
 *
 * @author Toshiaki Maki
 */
public final class NotNode implements Node {

	private final Node child;

	private Node parent;

	public NotNode(Node child) {
		this.child = Objects.requireNonNull(child, "child must not be null");
		this.child.setParent(this);
	}

	@Override
	public String value() {
		return "NOT";
	}

	@Override
	public <T> T accept(NodeVisitor<T> visitor) {
		return visitor.visitNot(this);
	}

	@Override
	public void walk(Consumer<Node> consumer) {
		consumer.accept(this);
		child.walk(consumer);
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
		return List.of(child);
	}

	public Node child() {
		return child;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (!(o instanceof NotNode notNode))
			return false;
		return Objects.equals(child, notNode.child);
	}

	@Override
	public int hashCode() {
		return Objects.hash(child);
	}

	@Override
	public String toString() {
		return "NotNode{child=" + child + '}';
	}

}