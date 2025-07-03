package am.ik.query;

import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * Pretty printer for queries.
 *
 * @author Toshiaki Maki
 */
public class QueryPrinter {

	private QueryPrinter() {
	}

	/**
	 * Prints a query to stdout.
	 * @param query the query to print
	 */
	public static void print(Query query) {
		System.out.println(toPrettyString(query));
	}

	/**
	 * Prints a node to stdout.
	 * @param node the node to print
	 */
	public static void print(Node node) {
		System.out.println(toPrettyString(node));
	}

	/**
	 * Converts a query to a pretty string.
	 * @param query the query to convert
	 * @return the pretty string
	 */
	public static String toPrettyString(Query query) {
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);
		pw.println("Query: " + query.originalQuery());
		pw.println("AST:");
		printNode(query.rootNode(), pw, 0);
		pw.println();
		pw.println("Metadata: " + query.metadata());
		return sw.toString();
	}

	/**
	 * Converts a node to a pretty string.
	 * @param node the node to convert
	 * @return the pretty string
	 */
	public static String toPrettyString(Node node) {
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);
		printNode(node, pw, 0);
		return sw.toString();
	}

	private static void printNode(Node node, PrintWriter pw, int level) {
		String indent = "  ".repeat(level);
		String type = node.getClass().getSimpleName();
		String value = node.value();

		if (node instanceof TokenNode tokenNode) {
			pw.println(indent + "└─ " + type + "[" + tokenNode.type() + "]: \"" + value + "\"");
		}
		else if (node instanceof FieldNode fieldNode) {
			pw.println(indent + "└─ " + type + ": " + fieldNode.field() + "=\"" + fieldNode.fieldValue() + "\"");
		}
		else if (node instanceof FuzzyNode fuzzyNode) {
			pw.println(indent + "└─ " + type + ": \"" + fuzzyNode.term() + "\" ~" + fuzzyNode.maxEdits());
		}
		else if (node instanceof RangeNode rangeNode) {
			String field = rangeNode.field() != null ? rangeNode.field() + ":" : "";
			String start = rangeNode.includeStart() ? "[" : "{";
			String end = rangeNode.includeEnd() ? "]" : "}";
			pw.println(
					indent + "└─ " + type + ": " + field + start + rangeNode.start() + " TO " + rangeNode.end() + end);
		}
		else if (node.isGroup()) {
			pw.println(indent + "└─ " + type + " (" + node.children().size() + " children)");
			for (Node child : node.children()) {
				printNode(child, pw, level + 1);
			}
		}
		else {
			pw.println(indent + "└─ " + type + ": \"" + value + "\"");
		}
	}

}