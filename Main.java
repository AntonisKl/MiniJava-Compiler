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
			System.err.println("Program parsed successfully.");
			SymbolTableVisitor eval = new SymbolTableVisitor(symbols);

			Goal root = parser.Goal();
			System.out.println(root.accept(eval, null));
			System.out.println(symbols.toString());

			TypeCheckingVisitor typeCheck = new TypeCheckingVisitor(symbols);

			// System.out.println(root.accept(typeCheck, null));

			// System.out.println((new int[1]) < (new int[2]));
			// System.out.println(t && 2);
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
