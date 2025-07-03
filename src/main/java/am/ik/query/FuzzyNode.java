package am.ik.query;

import java.util.Objects;
import java.util.function.Consumer;

/**
 * Represents a fuzzy query node in the AST. Fuzzy queries match terms within a certain
 * edit distance.
 *
 * @author Toshiaki Maki
 */
public final class FuzzyNode implements Node {

	private final String term;

	private final int maxEdits;

	private final Token token;

	private Node parent;

	public FuzzyNode(String term, int maxEdits, Token token) {
		this.term = Objects.requireNonNull(term, "term must not be null");
		if (maxEdits < 0 || maxEdits > 2) {
			throw new IllegalArgumentException("maxEdits must be between 0 and 2");
		}
		this.maxEdits = maxEdits;
		this.token = Objects.requireNonNull(token, "token must not be null");
	}

	public FuzzyNode(String term, int maxEdits) {
		this(term, maxEdits, new Token(TokenType.FUZZY, term + "~" + maxEdits));
	}

	public FuzzyNode(String term) {
		this(term, 2);
	}

	@Override
	public String value() {
		return term + "~" + maxEdits;
	}

	@Override
	public <T> T accept(NodeVisitor<T> visitor) {
		return visitor.visitFuzzy(this);
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

	public String term() {
		return term;
	}

	public int maxEdits() {
		return maxEdits;
	}

	public Token token() {
		return token;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (!(o instanceof FuzzyNode fuzzyNode))
			return false;
		return maxEdits == fuzzyNode.maxEdits && Objects.equals(term, fuzzyNode.term);
	}

	@Override
	public int hashCode() {
		return Objects.hash(term, maxEdits);
	}

	@Override
	public String toString() {
		return "FuzzyNode{term='" + term + "', maxEdits=" + maxEdits + '}';
	}

}