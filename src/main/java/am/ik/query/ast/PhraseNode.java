package am.ik.query.ast;

import am.ik.query.lexer.Token;
import am.ik.query.lexer.TokenType;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Represents a phrase query node in the AST. Phrases are exact matches of multiple terms.
 *
 * @author Toshiaki Maki
 */
public final class PhraseNode implements Node {

	private final String phrase;

	private final Token token;

	private Node parent;

	public PhraseNode(String phrase, Token token) {
		this.phrase = Objects.requireNonNull(phrase, "phrase must not be null");
		this.token = Objects.requireNonNull(token, "token must not be null");
	}

	public PhraseNode(String phrase) {
		this(phrase, new Token(TokenType.PHRASE, phrase));
	}

	@Override
	public String value() {
		return phrase;
	}

	@Override
	public <T> T accept(NodeVisitor<T> visitor) {
		return visitor.visitPhrase(this);
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

	public String phrase() {
		return phrase;
	}

	public Token token() {
		return token;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (!(o instanceof PhraseNode that))
			return false;
		return Objects.equals(phrase, that.phrase);
	}

	@Override
	public int hashCode() {
		return Objects.hash(phrase);
	}

	@Override
	public String toString() {
		return "PhraseNode{phrase='" + phrase + "'}";
	}

}