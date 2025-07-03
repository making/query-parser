package am.ik.query.ast;

import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

import am.ik.query.visitor.NodeVisitor;

/**
 * Represents an OR operation node in the query AST. At least one child must match for
 * this node to match.
 *
 * @author Toshiaki Maki
 */
public final class OrNode implements Node {

	private final List<Node> children;

	private Node parent;

	public OrNode(List<Node> children) {
		this.children = List.copyOf(Objects.requireNonNull(children, "children must not be null"));
		this.children.forEach(child -> child.setParent(this));
	}

	@Override
	public String value() {
		return "OR";
	}

	@Override
	public <T> T accept(NodeVisitor<T> visitor) {
		return visitor.visitOr(this);
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
		if (!(o instanceof OrNode orNode))
			return false;
		return Objects.equals(children, orNode.children);
	}

	@Override
	public int hashCode() {
		return Objects.hash(children);
	}

	@Override
	public String toString() {
		return "OrNode{children=" + children + '}';
	}

}