package am.ik.query.parser;

/**
 * Exception thrown when a query parsing error occurs.
 *
 * @author Toshiaki Maki
 */
public class QueryParseException extends RuntimeException {

	private final int position;

	private final String query;

	public QueryParseException(String message, String query, int position) {
		super(message);
		this.query = query;
		this.position = position;
	}

	public QueryParseException(String message, String query, int position, Throwable cause) {
		super(message, cause);
		this.query = query;
		this.position = position;
	}

	public QueryParseException(String message) {
		this(message, null, -1);
	}

	public QueryParseException(String message, Throwable cause) {
		this(message, null, -1, cause);
	}

	/**
	 * Gets the position in the query where the error occurred.
	 * @return the error position, or -1 if unknown
	 */
	public int getPosition() {
		return position;
	}

	/**
	 * Gets the original query that caused the error.
	 * @return the query string, or null if not available
	 */
	public String getQuery() {
		return query;
	}

	/**
	 * Gets a detailed error message with position indicator.
	 * @return the detailed error message
	 */
	public String getDetailedMessage() {
		if (query == null || position < 0) {
			return getMessage();
		}

		StringBuilder sb = new StringBuilder();
		sb.append(getMessage()).append("\n");
		sb.append("Query: ").append(query).append("\n");
		sb.append("       ");

		for (int i = 0; i < position && i < query.length(); i++) {
			sb.append(" ");
		}
		sb.append("^");

		return sb.toString();
	}

	@Override
	public String toString() {
		return getDetailedMessage();
	}

}