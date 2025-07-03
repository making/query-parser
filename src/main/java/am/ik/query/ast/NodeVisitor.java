package am.ik.query.ast;

/**
 * Visitor interface for traversing query AST nodes. Implements the visitor pattern for
 * type-safe node traversal.
 *
 * @param <T> the return type of visit operations
 * @author Toshiaki Maki
 */
public interface NodeVisitor<T> {

	/**
	 * Visits a token node.
	 * @param node the token node to visit
	 * @return the result of visiting the node
	 */
	T visitToken(TokenNode node);

	/**
	 * Visits a root/group node.
	 * @param node the root node to visit
	 * @return the result of visiting the node
	 */
	T visitRoot(RootNode node);

	/**
	 * Visits an AND node.
	 * @param node the AND node to visit
	 * @return the result of visiting the node
	 */
	T visitAnd(AndNode node);

	/**
	 * Visits an OR node.
	 * @param node the OR node to visit
	 * @return the result of visiting the node
	 */
	T visitOr(OrNode node);

	/**
	 * Visits a NOT node.
	 * @param node the NOT node to visit
	 * @return the result of visiting the node
	 */
	T visitNot(NotNode node);

	/**
	 * Visits a field query node.
	 * @param node the field node to visit
	 * @return the result of visiting the node
	 */
	T visitField(FieldNode node);

	/**
	 * Visits a phrase query node.
	 * @param node the phrase node to visit
	 * @return the result of visiting the node
	 */
	T visitPhrase(PhraseNode node);

	/**
	 * Visits a wildcard query node.
	 * @param node the wildcard node to visit
	 * @return the result of visiting the node
	 */
	T visitWildcard(WildcardNode node);

	/**
	 * Visits a fuzzy query node.
	 * @param node the fuzzy node to visit
	 * @return the result of visiting the node
	 */
	T visitFuzzy(FuzzyNode node);

	/**
	 * Visits a range query node.
	 * @param node the range node to visit
	 * @return the result of visiting the node
	 */
	T visitRange(RangeNode node);

}