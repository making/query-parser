package am.ik.query;

public class Main {

	public static void main(String[] args) {
		Query query = QueryParser.create().parse("hello (world or java)");
		System.out.println(query);
		QueryPrinter.print(query);
	}

}
