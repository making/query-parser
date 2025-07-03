package am.ik.query;

/**
 * Base implementation of NodeVisitor with default behavior. Subclasses can override only
 * the methods they need.
 *
 * @param <T> the return type of visit operations
 * @author Toshiaki Maki
 */
public abstract class BaseNodeVisitor<T> implements NodeVisitor<T> {

	protected abstract T defaultValue();

	@Override
	public T visitToken(TokenNode node) {
		return defaultValue();
	}

	@Override
	public T visitRoot(RootNode node) {
		for (Node child : node.children()) {
			child.accept(this);
		}
		return defaultValue();
	}

	@Override
	public T visitAnd(AndNode node) {
		for (Node child : node.children()) {
			child.accept(this);
		}
		return defaultValue();
	}

	@Override
	public T visitOr(OrNode node) {
		for (Node child : node.children()) {
			child.accept(this);
		}
		return defaultValue();
	}

	@Override
	public T visitNot(NotNode node) {
		node.child().accept(this);
		return defaultValue();
	}

	@Override
	public T visitField(FieldNode node) {
		return defaultValue();
	}

	@Override
	public T visitPhrase(PhraseNode node) {
		return defaultValue();
	}

	@Override
	public T visitWildcard(WildcardNode node) {
		return defaultValue();
	}

	@Override
	public T visitFuzzy(FuzzyNode node) {
		return defaultValue();
	}

	@Override
	public T visitRange(RangeNode node) {
		return defaultValue();
	}

}