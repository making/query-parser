package am.ik.query;

import java.util.List;

public sealed interface Node permits RootNode, TokenNode {

	String value();

	static void print(RootNode node) {
		print(node, 0);
	}

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
