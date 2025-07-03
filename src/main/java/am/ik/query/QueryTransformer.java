package am.ik.query;

import java.util.function.Function;

/**
 * Interface for transforming queries. Implementations can modify the query structure.
 *
 * @author Toshiaki Maki
 */
@FunctionalInterface
public interface QueryTransformer extends Function<Query, Query> {

	/**
	 * Transforms the given query.
	 * @param query the query to transform
	 * @return the transformed query
	 */
	Query transform(Query query);

	@Override
	default Query apply(Query query) {
		return transform(query);
	}

	/**
	 * Chains this transformer with another.
	 * @param after the transformer to apply after this one
	 * @return a combined transformer
	 */
	default QueryTransformer andThen(QueryTransformer after) {
		return query -> after.transform(this.transform(query));
	}

	/**
	 * Creates a transformer from a node visitor.
	 * @param visitor the visitor that transforms nodes
	 * @return a query transformer
	 */
	static QueryTransformer fromVisitor(NodeVisitor<Node> visitor) {
		return query -> {
			Node transformedRoot = query.rootNode().accept(visitor);
			return new Query(query.originalQuery(), transformedRoot, query.metadata());
		};
	}

}