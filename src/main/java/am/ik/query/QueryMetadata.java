package am.ik.query;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Metadata associated with a parsed query. Provides information about parsing results and
 * query characteristics.
 *
 * @author Toshiaki Maki
 */
public final class QueryMetadata {

	private final int tokenCount;

	private final int nodeCount;

	private final int maxDepth;

	private final long parseTimeNanos;

	private final Instant parsedAt;

	private final Map<String, Object> properties;

	private QueryMetadata(Builder builder) {
		this.tokenCount = builder.tokenCount;
		this.nodeCount = builder.nodeCount;
		this.maxDepth = builder.maxDepth;
		this.parseTimeNanos = builder.parseTimeNanos;
		this.parsedAt = builder.parsedAt;
		this.properties = Collections.unmodifiableMap(new HashMap<>(builder.properties));
	}

	/**
	 * Creates a new metadata builder.
	 * @return a new builder
	 */
	public static Builder builder() {
		return new Builder();
	}

	/**
	 * Gets the total number of tokens in the query.
	 * @return the token count
	 */
	public int tokenCount() {
		return tokenCount;
	}

	/**
	 * Gets the total number of nodes in the AST.
	 * @return the node count
	 */
	public int nodeCount() {
		return nodeCount;
	}

	/**
	 * Gets the maximum depth of the AST.
	 * @return the maximum depth
	 */
	public int maxDepth() {
		return maxDepth;
	}

	/**
	 * Gets the parse time in nanoseconds.
	 * @return the parse time in nanoseconds
	 */
	public long parseTimeNanos() {
		return parseTimeNanos;
	}

	/**
	 * Gets the parse time in milliseconds.
	 * @return the parse time in milliseconds
	 */
	public double parseTimeMillis() {
		return parseTimeNanos / 1_000_000.0;
	}

	/**
	 * Gets the parse time as a Duration.
	 * @return the parse time as Duration
	 */
	public Duration parseTime() {
		return Duration.ofNanos(parseTimeNanos);
	}

	/**
	 * Gets the timestamp when the query was parsed.
	 * @return the parsed timestamp
	 */
	public Instant parsedAt() {
		return parsedAt;
	}

	/**
	 * Gets a custom property value.
	 * @param key the property key
	 * @return the property value, or empty if not present
	 */
	public Optional<Object> getProperty(String key) {
		return Optional.ofNullable(properties.get(key));
	}

	/**
	 * Gets all custom properties.
	 * @return unmodifiable map of properties
	 */
	public Map<String, Object> properties() {
		return properties;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (!(o instanceof QueryMetadata that))
			return false;
		return tokenCount == that.tokenCount && nodeCount == that.nodeCount && maxDepth == that.maxDepth
				&& parseTimeNanos == that.parseTimeNanos && Objects.equals(parsedAt, that.parsedAt)
				&& Objects.equals(properties, that.properties);
	}

	@Override
	public int hashCode() {
		return Objects.hash(tokenCount, nodeCount, maxDepth, parseTimeNanos, parsedAt, properties);
	}

	@Override
	public String toString() {
		return "QueryMetadata{" + "tokenCount=" + tokenCount + ", nodeCount=" + nodeCount + ", maxDepth=" + maxDepth
				+ ", parseTimeMillis=" + parseTimeMillis() + ", parsedAt=" + parsedAt + ", properties=" + properties
				+ '}';
	}

	/**
	 * Builder for QueryMetadata.
	 */
	public static class Builder {

		private int tokenCount;

		private int nodeCount;

		private int maxDepth;

		private long parseTimeNanos;

		private Instant parsedAt = Instant.now();

		private final Map<String, Object> properties = new HashMap<>();

		private Builder() {
		}

		public Builder tokenCount(int tokenCount) {
			this.tokenCount = tokenCount;
			return this;
		}

		public Builder nodeCount(int nodeCount) {
			this.nodeCount = nodeCount;
			return this;
		}

		public Builder maxDepth(int maxDepth) {
			this.maxDepth = maxDepth;
			return this;
		}

		public Builder parseTimeNanos(long parseTimeNanos) {
			this.parseTimeNanos = parseTimeNanos;
			return this;
		}

		public Builder parsedAt(Instant parsedAt) {
			this.parsedAt = Objects.requireNonNull(parsedAt, "parsedAt must not be null");
			return this;
		}

		public Builder property(String key, Object value) {
			this.properties.put(key, value);
			return this;
		}

		public Builder properties(Map<String, Object> properties) {
			this.properties.putAll(properties);
			return this;
		}

		public QueryMetadata build() {
			return new QueryMetadata(this);
		}

	}

}