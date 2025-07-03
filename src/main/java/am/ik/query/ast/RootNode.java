package am.ik.query.ast;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

import am.ik.query.visitor.NodeVisitor;

/**
 * Represents the root node of a query AST. Acts as a container for top-level query
 * elements.
 *
 * @author Toshiaki Maki
 */
public final class RootNode implements Node {

	private final List<Node> children;

	private Node parent;

	public RootNode() {
		this.children = new ArrayList<>();
	}

	public RootNode(List<Node> children) {
		this.children = new ArrayList<>(Objects.requireNonNull(children, "children must not be null"));
		this.children.forEach(child -> child.setParent(this));
	}

	@Override
	public String value() {
		return "root";
	}

	@Override
	public <T> T accept(NodeVisitor<T> visitor) {
		return visitor.visitRoot(this);
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
		return this.children;
	}

	public boolean hasChildren() {
		return !this.children.isEmpty();
	}

	public void addChild(Node child) {
		Objects.requireNonNull(child, "child must not be null");
		this.children.add(child);
		child.setParent(this);
	}

	public void addChildren(List<Node> children) {
		Objects.requireNonNull(children, "children must not be null");
		children.forEach(this::addChild);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (!(o instanceof RootNode rootNode))
			return false;
		return Objects.equals(children, rootNode.children);
	}

	@Override
	public int hashCode() {
		return Objects.hash(children);
	}

	@Override
	public String toString() {
		return "RootNode{children=" + children + '}';
	}

}
