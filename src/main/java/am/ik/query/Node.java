package am.ik.query;

import java.util.List;
import java.util.function.Consumer;

/**
 * Base interface for all AST nodes in a query. This is a sealed interface to ensure type
 * safety and exhaustive pattern matching.
 *
 * @author Toshiaki Maki
 */
public sealed interface Node permits RootNode, TokenNode, AndNode, OrNode, NotNode, FieldNode, PhraseNode, WildcardNode,
		FuzzyNode, RangeNode {

	/**
	 * Gets the string value representation of this node.
	 * @return the node value
	 */
	String value();

	/**
	 * Accepts a visitor for this node.
	 * @param visitor the visitor to accept
	 * @param <T> the return type of the visitor
	 * @return the result of the visitor
	 */
	<T> T accept(NodeVisitor<T> visitor);

	/**
	 * Walks through this node and all its children.
	 * @param consumer the consumer to apply to each node
	 */
	void walk(Consumer<Node> consumer);

	/**
	 * Gets the parent node of this node.
	 * @return the parent node, or null if this is the root
	 */
	Node parent();

	/**
	 * Sets the parent node of this node.
	 * @param parent the parent node
	 */
	void setParent(Node parent);

	/**
	 * Checks if this is a leaf node (has no children).
	 * @return true if this is a leaf node
	 */
	default boolean isLeaf() {
		return this instanceof TokenNode || this instanceof FieldNode || this instanceof PhraseNode
				|| this instanceof WildcardNode || this instanceof FuzzyNode;
	}

	/**
	 * Checks if this is a group node (has children).
	 * @return true if this is a group node
	 */
	default boolean isGroup() {
		return !isLeaf();
	}

	/**
	 * Gets the children of this node.
	 * @return list of child nodes, or empty list if leaf node
	 */
	default List<Node> children() {
		return List.of();
	}

	/**
	 * Gets the depth of this node in the AST.
	 * @return the depth (0 for root)
	 */
	default int depth() {
		int depth = 0;
		Node current = parent();
		while (current != null) {
			depth++;
			current = current.parent();
		}
		return depth;
	}

	/**
	 * Utility method to print a node tree.
	 * @param node the root node to print
	 * @deprecated Use Query.prettyPrint() instead
	 */
	@Deprecated(since = "2.0", forRemoval = true)
	static void print(RootNode node) {
		print(node, 0);
	}

	/**
	 * Utility method to print a node with indentation.
	 * @param node the node to print
	 * @param level the indentation level
	 * @deprecated Use Query.prettyPrint() instead
	 */
	@Deprecated(since = "2.0", forRemoval = true)
	static void print(Node node, int level) {
		if (node instanceof TokenNode) {
			System.out.println("\t".repeat(level) + node);
		}
		else if (node instanceof RootNode) {
			System.out.println("\t".repeat(level) + node);
			List<Node> children = ((RootNode) node).children();
			if (!children.isEmpty()) {
				children.forEach(c -> print(c, level + 1));
			}
		}
	}

}
