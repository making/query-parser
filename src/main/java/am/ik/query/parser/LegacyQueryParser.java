package am.ik.query.parser;

import java.util.List;

import am.ik.query.Query;
import am.ik.query.QueryMetadata;
import am.ik.query.ast.RootNode;
import am.ik.query.ast.TokenNode;
import am.ik.query.lexer.LegacyQueryLexer;
import am.ik.query.lexer.Token;

/**
 * Legacy query parser for backward compatibility.
 */
@Deprecated(since = "0.2.0", forRemoval = true)
class LegacyQueryParser {

	private final List<Token> tokens;

	private int position;

	public LegacyQueryParser(List<Token> tokens) {
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
				default:
					position++;
					break;
			}
		}
		return node;
	}

}