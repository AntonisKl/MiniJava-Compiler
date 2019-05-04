package MyClasses;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.ArrayList;
import java.util.List;
import java.util.Iterator;
import java.util.Map.*;

class ClassMaps {
	// varTypes: <variable's name, variable's type>
	// methodTypes: <method's name, method's type>
	public Map<String, String> varTypes, methodTypes;
	public Map<String, Map<String, String>> methodVarTypes; // < method's name, <variable's name, varibale's type>>
	public Map<String, List<String>> methodParamTypes; // < method's name, list of method's parameter types>

	// for offsets
	// varOffsets: <variable's name, variable's offset>
	// methodOffsets: <method's name, method's offset>
	public Map<String, Integer> varOffsets, methodOffsets;

	public ClassMaps() {
		varTypes = new HashMap<String, String>();
		methodTypes = new HashMap<String, String>();
		methodVarTypes = new HashMap<String, Map<String, String>>();
		methodParamTypes = new HashMap<String, List<String>>();
		varOffsets = new LinkedHashMap<String, Integer>(); // LinkedHashMap because we want to maintain the insertion order of elements
		methodOffsets = new LinkedHashMap<String, Integer>(); // LinkedHashMap because we want to maintain the insertion order of elements
	}

	// for offset printing
	private String offsetsMapToString(Map<String, Integer> map, String className) {
		StringBuilder sb = new StringBuilder();
		Iterator<Entry<String, Integer>> iter = map.entrySet().iterator();
		while (iter.hasNext()) {
			Entry<String, Integer> entry = iter.next();
			sb.append(className);
			sb.append('.');
			sb.append(entry.getKey());
			sb.append(" : ");
			sb.append(entry.getValue());
			sb.append('\n');
		}
		return sb.toString();
	}

	// for offset printing
	public String offsetsToString(String className) {
		String returnValue = "";
		if (varOffsets.size() == 0 && methodOffsets.size() == 0)
			return returnValue;

		returnValue += "-----------Class " + className + "-----------\n";

		returnValue += "--Variables---\n";
		if (varOffsets.size() > 0) {
			returnValue += offsetsMapToString(varOffsets, className);
		}

		returnValue += "---Methods---\n";
		if (methodOffsets.size() > 0) {
			returnValue += offsetsMapToString(methodOffsets, className);
		}

		returnValue += "\n";
		return returnValue;
	}

	// for debugging purposes (not currently used)
	@Override
	public String toString() {
		return "\n\tVariable types:\n\t" + (new PrettyPrintingMap(varTypes)).toString() + "\n\n	Method types:\n\t"
				+ (new PrettyPrintingMap(methodTypes)).toString() + "\n\n\tMethod varibale types:\n\t"
				+ (new PrettyPrintingMap(methodVarTypes)).toString() + "\n\n\tMethod parameter types:\n\t"
				+ (new PrettyPrintingMap(methodParamTypes)).toString() + "\n\n\tVariable offsets:\n\t"
				+ (new PrettyPrintingMap(varOffsets)).toString() + "\n\n\tMethod offsets:\n\t"
				+ (new PrettyPrintingMap(methodOffsets)).toString() + "\n\n";
	}
}