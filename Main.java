import syntaxtree.*;
import visitor.*;
import java.io.*;

class Main {

	public static void main(String[] args) {
		Symbols symbols;

		FileInputStream fis = null;
		for (String arg : args) {
			symbols = new Symbols();
			try {
				String outFilePath = "results/" + arg.replace(".java", ".txt");
				File outFile = new File(outFilePath);
				outFile.getParentFile().mkdirs();
				outFile.createNewFile();

				FileOutputStream outStream = new FileOutputStream(outFilePath);

				fis = new FileInputStream(arg);
				MiniJavaParser parser = new MiniJavaParser(fis);
				System.out.println("Program parsed successfully");

				Goal root = parser.Goal();

				SymbolTableVisitor sybmolTableVisitor = new SymbolTableVisitor(symbols);
				String symbolTableErrorMsg = null;

				try {
					root.accept(sybmolTableVisitor, null);
				} catch (TypeCheckingException e) {
					// e.printStackTrace();

					StringWriter sw = new StringWriter();
					e.printStackTrace(new PrintWriter(sw));
					symbolTableErrorMsg = sw.toString();
				}

				if (symbolTableErrorMsg == null) {
					System.out.println("Symbol table created successfully");

					// System.out.println(symbols.toString());
					// outStream.write(symbols.toString().getBytes());
				} else {
					System.out.println("Symbol table not created, continuing to next file...");

					outStream.write(symbolTableErrorMsg.getBytes());
					continue;
				}


				TypeCheckingVisitor typeCheckingVisitor = new TypeCheckingVisitor(symbols);
				String typeCheckingErrorMsg = null;
				try {
					root.accept(typeCheckingVisitor, null);
				} catch (TypeCheckingException e) {
					// e.printStackTrace();

					StringWriter sw = new StringWriter();
					e.printStackTrace(new PrintWriter(sw));
					typeCheckingErrorMsg = sw.toString();
				}

				if (typeCheckingErrorMsg == null) {
					System.out.println("Semantic check completed successfully");

					// System.out.println(symbols.toString());
					outStream.write('\n');
					outStream.write(symbols.toString().getBytes());
				} else {
					System.out.println("Semantic check completed with an error");

					outStream.write('\n');
					outStream.write(typeCheckingErrorMsg.getBytes());
				}

				outStream.close();
				// System.out.println((new int[1]) < (new int[2]));
				// System.out.println(true);
			} catch (ParseException ex) {
				System.out.println(ex.getMessage());
			} catch (FileNotFoundException ex) {
				System.err.println(ex.getMessage());
			} catch (IOException ex) {
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
}
