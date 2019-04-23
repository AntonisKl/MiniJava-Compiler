import syntaxtree.*;
import visitor.*;
import java.io.*;

class Main {

	public static void main(String[] args) {

		Symbols symbols = new Symbols();

		if (args.length != 1) {
			System.err.println("Usage: java Driver <inputFile>");
			System.exit(1);
		}
		FileInputStream fis = null;
		try {
			fis = new FileInputStream(args[0]);
			MiniJavaParser parser = new MiniJavaParser(fis);
			System.out.println("Program parsed successfully");
			SymbolTableVisitor sybmolTableVisitor = new SymbolTableVisitor(symbols);

			Goal root = parser.Goal();
			root.accept(sybmolTableVisitor, null);
			System.out.println(symbols.toString());

			TypeCheckingVisitor typeCheckingVisitor = new TypeCheckingVisitor(symbols);

			root.accept(typeCheckingVisitor, null);

			System.out.println("Semantic check completed successfully");

			// System.out.println((new int[1]) < (new int[2]));
			// System.out.println(true);
		} catch (ParseException ex) {
			System.out.println(ex.getMessage());
		} catch (FileNotFoundException ex) {
			System.err.println(ex.getMessage());
		} finally {
			try {
				if (fis != null)
					fis.close();
			} catch (IOException ex) {
				System.err.println(ex.getMessage());
			}
		}
	}
}
