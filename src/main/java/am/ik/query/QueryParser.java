package am.ik.query;

import java.util.List;

public class QueryParser {

	private final List<Token> tokens;

	private int position;

	public QueryParser(List<Token> tokens) {
		this.tokens = tokens;
		this.position = 0;
	}

	public RootNode parse() {
		return parseExpression(new RootNode());
	}

	private RootNode parseExpression(RootNode node) {
		while (position < tokens.size()) {
			Token token = tokens.get(position);
			switch (token.type()) {
				case PHRASE:
				case EXCLUDE:
				case KEYWORD:
				case OR:
					node.children().add(new TokenNode(token.type(), token.value()));
					position++;
					break;
				case LPAREN:
					position++;
					node.children().add(parseExpression(new RootNode()));
					break;
				case RPAREN:
					position++;
					return node;
				case WHITESPACE:
					position++;
					break;
			}
		}
		return node;
	}

	public static RootNode parseQuery(String query) {
		List<Token> tokens = QueryLexer.tokenize(query);
		QueryParser queryParser = new QueryParser(tokens);
		return queryParser.parse();
	}

}
