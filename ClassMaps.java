import java.util.HashMap;
import java.util.Map;

class ClassMaps {
	public Map<String, String> varTypes, methodTypes;
	public Map<String, Map<String, String>> methodVarTypes;

	public ClassMaps() {
		varTypes = new HashMap<String, String>();
		methodTypes = new HashMap<String, String>();
		methodVarTypes = new HashMap<String, Map<String, String>>();
	}

	@Override
	public String toString() {
		// System.out.println("	Variable types:");
		// System.out.println();
		// System.out.println("	Method types:");
		// System.out.println();
		// System.out.println("	Method varibale types:");
		// System.out.println((new PrettyPrintingMap(methodVarTypes)).toString());

		return "\n	Variable types:\n" + (new PrettyPrintingMap(varTypes)).toString() + "\n\n	Method types:\n"
				+ (new PrettyPrintingMap(methodTypes)).toString() + "\n\n	Method varibale types:\n"
				+ (new PrettyPrintingMap(methodVarTypes)).toString() + "\n\n";
	}
}