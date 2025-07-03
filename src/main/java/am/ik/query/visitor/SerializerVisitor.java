package am.ik.query.visitor;

import am.ik.query.ast.AndNode;
import am.ik.query.ast.FieldNode;
import am.ik.query.ast.FuzzyNode;
import am.ik.query.ast.NotNode;
import am.ik.query.ast.OrNode;
import am.ik.query.ast.PhraseNode;
import am.ik.query.ast.RangeNode;
import am.ik.query.ast.RootNode;
import am.ik.query.ast.TokenNode;
import am.ik.query.ast.WildcardNode;

public class SerializerVisitor implements NodeVisitor<String> {

	@Override
	public String visitToken(TokenNode node) {
		switch (node.type()) {
			case EXCLUDE:
				return "-" + node.value();
			case REQUIRED:
				return "+" + node.value();
			default:
				return node.value();
		}
	}

	@Override
	public String visitRoot(RootNode node) {
		return String.join(" ", node.children().stream().map(child -> child.accept(this)).toList());
	}

	@Override
	public String visitAnd(AndNode node) {
		String content = String.join(" AND ", node.children().stream().map(child -> child.accept(this)).toList());
		return node.parent() != null ? "(" + content + ")" : content;
	}

	@Override
	public String visitOr(OrNode node) {
		String content = String.join(" OR ", node.children().stream().map(child -> child.accept(this)).toList());
		return node.parent() != null ? "(" + content + ")" : content;
	}

	@Override
	public String visitNot(NotNode node) {
		String child = node.child().accept(this);
		if (node.child() instanceof TokenNode) {
			return "-" + child;
		}
		return "NOT " + child;
	}

	@Override
	public String visitField(FieldNode node) {
		String value = node.fieldValue();
		// Quote value if it contains spaces
		if (value.contains(" ")) {
			value = "\"" + value + "\"";
		}
		return node.field() + ":" + value;
	}

	@Override
	public String visitPhrase(PhraseNode node) {
		return "\"" + node.phrase() + "\"";
	}

	@Override
	public String visitWildcard(WildcardNode node) {
		return node.pattern();
	}

	@Override
	public String visitFuzzy(FuzzyNode node) {
		return node.term() + "~" + (node.maxEdits() != 2 ? node.maxEdits() : "");
	}

	@Override
	public String visitRange(RangeNode node) {
		String startBracket = node.includeStart() ? "[" : "{";
		String endBracket = node.includeEnd() ? "]" : "}";
		String range = startBracket + node.start() + " TO " + node.end() + endBracket;
		return node.field() != null ? node.field() + ":" + range : range;
	}

}
