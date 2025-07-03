package am.ik.query;

import am.ik.query.ast.RootNode;
import am.ik.query.ast.TokenNode;
import am.ik.query.lexer.TokenType;
import am.ik.query.parser.QueryParser;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@SuppressWarnings("removal")
class QueryParserTest {

	@Test
	void singleKeyword() {
		RootNode node = QueryParser.parseQuery("hello");
		assertThat(node.hasChildren()).isTrue();
		assertThat(node.children()).hasSize(1);
		assertThat(node.children().get(0)).isInstanceOf(TokenNode.class);
		assertThat(((TokenNode) node.children().get(0)).type()).isEqualTo(TokenType.KEYWORD);
		assertThat(node.children().get(0).value()).isEqualTo("hello");
	}

	@Test
	void singlePhrase() {
		RootNode node = QueryParser.parseQuery("\"hello world\"");
		assertThat(node.hasChildren()).isTrue();
		assertThat(node.children()).hasSize(1);
		assertThat(node.children().get(0)).isInstanceOf(TokenNode.class);
		assertThat(((TokenNode) node.children().get(0)).type()).isEqualTo(TokenType.PHRASE);
		assertThat(node.children().get(0).value()).isEqualTo("hello world");
	}

	@Test
	void doubleKeyword() {
		RootNode node = QueryParser.parseQuery("hello world");
		assertThat(node.hasChildren()).isTrue();
		assertThat(node.children()).hasSize(2);
		assertThat(node.children().get(0)).isInstanceOf(TokenNode.class);
		assertThat(((TokenNode) node.children().get(0)).type()).isEqualTo(TokenType.KEYWORD);
		assertThat(node.children().get(0).value()).isEqualTo("hello");
		assertThat(node.children().get(1)).isInstanceOf(TokenNode.class);
		assertThat(((TokenNode) node.children().get(1)).type()).isEqualTo(TokenType.KEYWORD);
		assertThat(node.children().get(1).value()).isEqualTo("world");
	}

	@Test
	void or() {
		RootNode node = QueryParser.parseQuery("hello or world");
		assertThat(node.hasChildren()).isTrue();
		assertThat(node.children()).hasSize(3);
		assertThat(node.children().get(0)).isInstanceOf(TokenNode.class);
		assertThat(((TokenNode) node.children().get(0)).type()).isEqualTo(TokenType.KEYWORD);
		assertThat(node.children().get(0).value()).isEqualTo("hello");
		assertThat(node.children().get(1)).isInstanceOf(TokenNode.class);
		assertThat(((TokenNode) node.children().get(1)).type()).isEqualTo(TokenType.OR);
		assertThat(node.children().get(1).value()).isEqualTo("or");
		assertThat(node.children().get(2)).isInstanceOf(TokenNode.class);
		assertThat(((TokenNode) node.children().get(2)).type()).isEqualTo(TokenType.KEYWORD);
		assertThat(node.children().get(2).value()).isEqualTo("world");
	}

	@Test
	void orPhrase() {
		RootNode node = QueryParser.parseQuery("hello \"or\" world");
		assertThat(node.hasChildren()).isTrue();
		assertThat(node.children()).hasSize(3);
		assertThat(node.children().get(0)).isInstanceOf(TokenNode.class);
		assertThat(((TokenNode) node.children().get(0)).type()).isEqualTo(TokenType.KEYWORD);
		assertThat(node.children().get(0).value()).isEqualTo("hello");
		assertThat(node.children().get(1)).isInstanceOf(TokenNode.class);
		assertThat(((TokenNode) node.children().get(1)).type()).isEqualTo(TokenType.PHRASE);
		assertThat(node.children().get(1).value()).isEqualTo("or");
		assertThat(node.children().get(2)).isInstanceOf(TokenNode.class);
		assertThat(((TokenNode) node.children().get(2)).type()).isEqualTo(TokenType.KEYWORD);
		assertThat(node.children().get(2).value()).isEqualTo("world");
	}

	@Test
	void exclude() {
		RootNode node = QueryParser.parseQuery("hello -world");
		assertThat(node.hasChildren()).isTrue();
		assertThat(node.children()).hasSize(2);
		assertThat(node.children().get(0)).isInstanceOf(TokenNode.class);
		assertThat(((TokenNode) node.children().get(0)).type()).isEqualTo(TokenType.KEYWORD);
		assertThat(node.children().get(0).value()).isEqualTo("hello");
		assertThat(node.children().get(1)).isInstanceOf(TokenNode.class);
		assertThat(((TokenNode) node.children().get(1)).type()).isEqualTo(TokenType.EXCLUDE);
		assertThat(node.children().get(1).value()).isEqualTo("world");
	}

	@Test
	void nest() {
		RootNode node = QueryParser.parseQuery("hello (world or java)");
		assertThat(node.hasChildren()).isTrue();
		assertThat(node.children()).hasSize(2);
		assertThat(node.children().get(0)).isInstanceOf(TokenNode.class);
		assertThat(((TokenNode) node.children().get(0)).type()).isEqualTo(TokenType.KEYWORD);
		assertThat(node.children().get(0).value()).isEqualTo("hello");
		assertThat(node.children().get(1)).isInstanceOf(RootNode.class);
		RootNode root = (RootNode) node.children().get(1);
		assertThat(root.hasChildren()).isTrue();
		assertThat(root.children()).hasSize(3);
		assertThat(root.children().get(0)).isInstanceOf(TokenNode.class);
		assertThat(((TokenNode) root.children().get(0)).type()).isEqualTo(TokenType.KEYWORD);
		assertThat(root.children().get(0).value()).isEqualTo("world");
		assertThat(root.children().get(1)).isInstanceOf(TokenNode.class);
		assertThat(((TokenNode) root.children().get(1)).type()).isEqualTo(TokenType.OR);
		assertThat(root.children().get(1).value()).isEqualTo("or");
		assertThat(root.children().get(2)).isInstanceOf(TokenNode.class);
		assertThat(((TokenNode) root.children().get(2)).type()).isEqualTo(TokenType.KEYWORD);
		assertThat(root.children().get(2).value()).isEqualTo("java");
	}

	@Test
	void nestAndKeyword() {
		RootNode node = QueryParser.parseQuery("hello (world or java) test");
		assertThat(node.hasChildren()).isTrue();
		assertThat(node.children()).hasSize(3);
		assertThat(node.children().get(0)).isInstanceOf(TokenNode.class);
		assertThat(((TokenNode) node.children().get(0)).type()).isEqualTo(TokenType.KEYWORD);
		assertThat(node.children().get(0).value()).isEqualTo("hello");
		assertThat(node.children().get(1)).isInstanceOf(RootNode.class);
		assertThat(node.children().get(2)).isInstanceOf(TokenNode.class);
		assertThat(((TokenNode) node.children().get(2)).type()).isEqualTo(TokenType.KEYWORD);
		assertThat(node.children().get(2).value()).isEqualTo("test");
		RootNode root = (RootNode) node.children().get(1);
		assertThat(root.hasChildren()).isTrue();
		assertThat(root.children()).hasSize(3);
		assertThat(root.children().get(0)).isInstanceOf(TokenNode.class);
		assertThat(((TokenNode) root.children().get(0)).type()).isEqualTo(TokenType.KEYWORD);
		assertThat(root.children().get(0).value()).isEqualTo("world");
		assertThat(root.children().get(1)).isInstanceOf(TokenNode.class);
		assertThat(((TokenNode) root.children().get(1)).type()).isEqualTo(TokenType.OR);
		assertThat(root.children().get(1).value()).isEqualTo("or");
		assertThat(root.children().get(2)).isInstanceOf(TokenNode.class);
		assertThat(((TokenNode) root.children().get(2)).type()).isEqualTo(TokenType.KEYWORD);
		assertThat(root.children().get(2).value()).isEqualTo("java");
	}

	@Test
	void deepNest() {
		RootNode node = QueryParser.parseQuery("hello (world or (java or bean))");
		assertThat(node.hasChildren()).isTrue();
		assertThat(node.children()).hasSize(2);
		assertThat(node.children().get(0)).isInstanceOf(TokenNode.class);
		assertThat(((TokenNode) node.children().get(0)).type()).isEqualTo(TokenType.KEYWORD);
		assertThat(node.children().get(0).value()).isEqualTo("hello");
		assertThat(node.children().get(1)).isInstanceOf(RootNode.class);
		RootNode root = (RootNode) node.children().get(1);
		assertThat(root.hasChildren()).isTrue();
		assertThat(root.children()).hasSize(3);
		assertThat(root.children().get(0)).isInstanceOf(TokenNode.class);
		assertThat(((TokenNode) root.children().get(0)).type()).isEqualTo(TokenType.KEYWORD);
		assertThat(root.children().get(0).value()).isEqualTo("world");
		assertThat(root.children().get(1)).isInstanceOf(TokenNode.class);
		assertThat(((TokenNode) root.children().get(1)).type()).isEqualTo(TokenType.OR);
		assertThat(root.children().get(1).value()).isEqualTo("or");
		assertThat(root.children().get(2)).isInstanceOf(RootNode.class);
		RootNode root2 = (RootNode) root.children().get(2);
		assertThat(root2.hasChildren()).isTrue();
		assertThat(root2.children()).hasSize(3);
		assertThat(root2.children().get(0)).isInstanceOf(TokenNode.class);
		assertThat(((TokenNode) root2.children().get(0)).type()).isEqualTo(TokenType.KEYWORD);
		assertThat(root2.children().get(0).value()).isEqualTo("java");
		assertThat(root2.children().get(1)).isInstanceOf(TokenNode.class);
		assertThat(((TokenNode) root2.children().get(1)).type()).isEqualTo(TokenType.OR);
		assertThat(root2.children().get(1).value()).isEqualTo("or");
		assertThat(root2.children().get(2)).isInstanceOf(TokenNode.class);
		assertThat(((TokenNode) root2.children().get(2)).type()).isEqualTo(TokenType.KEYWORD);
		assertThat(root2.children().get(2).value()).isEqualTo("bean");
	}

	@Test
	void hyphen() {
		RootNode node = QueryParser.parseQuery("hello-world");
		assertThat(node.hasChildren()).isTrue();
		assertThat(node.children()).hasSize(1);
		assertThat(node.children().get(0)).isInstanceOf(TokenNode.class);
		assertThat(((TokenNode) node.children().get(0)).type()).isEqualTo(TokenType.KEYWORD);
		assertThat(node.children().get(0).value()).isEqualTo("hello-world");
	}

	@Test
	void quotedString() {
		RootNode node = QueryParser.parseQuery("foo=\"bar\"");
		assertThat(node.hasChildren()).isTrue();
		assertThat(node.children()).hasSize(1);
		assertThat(node.children().get(0)).isInstanceOf(TokenNode.class);
		assertThat(((TokenNode) node.children().get(0)).type()).isEqualTo(TokenType.KEYWORD);
		assertThat(node.children().get(0).value()).isEqualTo("foo=\"bar\"");
	}

}