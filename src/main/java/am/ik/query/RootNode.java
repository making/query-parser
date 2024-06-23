package am.ik.query;

import java.util.ArrayList;
import java.util.List;

public final class RootNode implements Node {

	private final List<Node> children = new ArrayList<>();

	@Override
	public String value() {
		return "root";
	}

	public List<Node> children() {
		return this.children;
	}

	public boolean hasChildren() {
		return !this.children.isEmpty();
	}

	@Override
	public String toString() {
		return RootNode.class.getSimpleName();
	}

}
