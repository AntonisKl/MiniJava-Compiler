import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.ArrayList;
import java.util.List;

class ClassMaps {
	public Map<String, String> varTypes, methodTypes;
	public Map<String, Map<String, String>> methodVarTypes;
	public Map<String, List<String>> methodParamTypes;

	// for offsets
	public Map<String, Integer> varOffsets, methodOffsets;

	public ClassMaps() {
		varTypes = new HashMap<String, String>();
		methodTypes = new HashMap<String, String>();
		methodVarTypes = new HashMap<String, Map<String, String>>();
		methodParamTypes = new HashMap<String, List<String>>();
		varOffsets = new LinkedHashMap<String, Integer>();
		methodOffsets = new LinkedHashMap<String, Integer>();
	}

	@Override
	public String toString() {
		// System.out.println("	Variable types:");
		// System.out.println();
		// System.out.println("	Method types:");
		// System.out.println();
		// System.out.println("	Method varibale types:");
		// System.out.println((new PrettyPrintingMap(methodVarTypes)).toString());

		return "\n\tVariable types:\n\t" + (new PrettyPrintingMap(varTypes)).toString() + "\n\n	Method types:\n\t"
				+ (new PrettyPrintingMap(methodTypes)).toString() + "\n\n\tMethod varibale types:\n\t"
				+ (new PrettyPrintingMap(methodVarTypes)).toString() + "\n\n\tMethod parameter types:\n\t"
				+ (new PrettyPrintingMap(methodParamTypes)).toString() + "\n\n\tVariable offsets:\n\t"
				+ (new PrettyPrintingMap(varOffsets)).toString()+ "\n\n\tMethod offsets:\n\t"
				+ (new PrettyPrintingMap(methodOffsets)).toString() + "\n\n";
	}

	// private String listToString(List<String> list) {
	// 	String retValue = "";

	// 	for (int i = 0; i < list.size(); i++) {
	// 		if (i > 0) {
	// 			retValue += ", ";
	// 		}
	// 		retValue += list.get(i);
	// 	}

	// 	return retValue;
	// }
}