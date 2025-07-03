package am.ik.query.ast;

import java.util.Objects;
import java.util.function.Consumer;

import am.ik.query.visitor.NodeVisitor;

/**
 * Represents a range query node in the AST. Format: [start TO end] or {start TO end}
 *
 * @author Toshiaki Maki
 */
public final class RangeNode implements Node {

	private final String start;

	private final String end;

	private final boolean includeStart;

	private final boolean includeEnd;

	private final String field;

	private Node parent;

	private RangeNode(Builder builder) {
		this.start = builder.start;
		this.end = builder.end;
		this.includeStart = builder.includeStart;
		this.includeEnd = builder.includeEnd;
		this.field = builder.field;
	}

	@Override
	public String value() {
		String startBracket = includeStart ? "[" : "{";
		String endBracket = includeEnd ? "]" : "}";
		String range = startBracket + start + " TO " + end + endBracket;
		return field != null ? field + ":" + range : range;
	}

	@Override
	public <T> T accept(NodeVisitor<T> visitor) {
		return visitor.visitRange(this);
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

	public String start() {
		return start;
	}

	public String end() {
		return end;
	}

	public boolean includeStart() {
		return includeStart;
	}

	public boolean includeEnd() {
		return includeEnd;
	}

	public String field() {
		return field;
	}

	public static Builder builder() {
		return new Builder();
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (!(o instanceof RangeNode rangeNode))
			return false;
		return includeStart == rangeNode.includeStart && includeEnd == rangeNode.includeEnd
				&& Objects.equals(start, rangeNode.start) && Objects.equals(end, rangeNode.end)
				&& Objects.equals(field, rangeNode.field);
	}

	@Override
	public int hashCode() {
		return Objects.hash(start, end, includeStart, includeEnd, field);
	}

	@Override
	public String toString() {
		return "RangeNode{" + "start='" + start + '\'' + ", end='" + end + '\'' + ", includeStart=" + includeStart
				+ ", includeEnd=" + includeEnd + ", field='" + field + '\'' + '}';
	}

	public static class Builder {

		private String start;

		private String end;

		private boolean includeStart = true;

		private boolean includeEnd = true;

		private String field;

		private Builder() {
		}

		public Builder start(String start) {
			this.start = Objects.requireNonNull(start, "start must not be null");
			return this;
		}

		public Builder end(String end) {
			this.end = Objects.requireNonNull(end, "end must not be null");
			return this;
		}

		public Builder includeStart(boolean includeStart) {
			this.includeStart = includeStart;
			return this;
		}

		public Builder includeEnd(boolean includeEnd) {
			this.includeEnd = includeEnd;
			return this;
		}

		public Builder field(String field) {
			this.field = field;
			return this;
		}

		public RangeNode build() {
			Objects.requireNonNull(start, "start must not be null");
			Objects.requireNonNull(end, "end must not be null");
			return new RangeNode(this);
		}

	}

}