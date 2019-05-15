import syntaxtree.*;
import visitor.*;
import java.io.*;
import MyClasses.Symbols;
import MyClasses.IRCodeGenVisitor;
import MyClasses.TypeCheckingVisitor;
import MyClasses.SymbolTableVisitor;
import MyClasses.TypeCheckingException;

class Main {

	public static void main(String[] args) {
		Symbols symbols;

		FileInputStream inputStream = null;
		FileOutputStream outStream = null;
		for (String arg : args) {
			symbols = new Symbols();
			try {
				// construct output file path and create it if it does not exist
				String[] filePathParts = arg.split("/");
				String outFilePath = "results/" + filePathParts[filePathParts.length - 1].replace(".java", ".ll");
				File outFile = new File(outFilePath);
				outFile.getParentFile().mkdirs();
				outFile.createNewFile();

				outStream = new FileOutputStream(outFilePath);

				inputStream = new FileInputStream(arg);

				System.out.println("Handling file with name: " + arg);
				MiniJavaParser parser = new MiniJavaParser(inputStream);
				System.out.println("Program parsed successfully");

				Goal root = parser.Goal();

				SymbolTableVisitor sybmolTableVisitor = new SymbolTableVisitor(symbols);
				String symbolTableErrorMsg = null;

				try {
					// create symbol table
					root.accept(sybmolTableVisitor, null);
				} catch (TypeCheckingException e) {
					StringWriter sw = new StringWriter();
					e.printStackTrace(new PrintWriter(sw));
					symbolTableErrorMsg = sw.toString();
				}

				if (symbolTableErrorMsg == null) { // all went OK
					System.out.println("Symbol table created successfully");
				} else { // error
					System.out.println("Symbol table not created, continuing to next file...");

					outStream.write(symbolTableErrorMsg.getBytes());
					continue;
				}

				TypeCheckingVisitor typeCheckingVisitor = new TypeCheckingVisitor(symbols);
				String typeCheckingErrorMsg = null, IRCode = "";
				try {
					// do type-checking
					IRCode += root.accept(typeCheckingVisitor, null) + "\n\n";
				} catch (TypeCheckingException e) {
					StringWriter sw = new StringWriter();
					e.printStackTrace(new PrintWriter(sw));
					typeCheckingErrorMsg = sw.toString();
				}

				if (typeCheckingErrorMsg == null) { // all went OK
					System.out.println("Semantic check completed successfully");

					// // print offsets to file
					// outStream.write(symbols.toString().getBytes());
					
				} else { // error
					System.out.println("Semantic check completed with an error");

					outStream.write(typeCheckingErrorMsg.getBytes());
					continue;
				}

				// System.out.println(IRCode);

				IRCodeGenVisitor irCodeGenVisitor = new IRCodeGenVisitor(symbols);
				IRCode += root.accept(irCodeGenVisitor, null);

				outStream.write(IRCode.getBytes());

				System.out.println(IRCode);


			} catch (ParseException ex) {
				System.out.println(ex.getMessage());
			} catch (FileNotFoundException ex) {
				System.err.println(ex.getMessage());
			} catch (IOException ex) {
				System.err.println(ex.getMessage());
			} finally {
				try {
					if (outStream != null) {
						outStream.close();
					}
					if (inputStream != null) {
						inputStream.close();
					}
				} catch (IOException ex) {
					System.err.println(ex.getMessage());
				}
			}
		}
	}
}
