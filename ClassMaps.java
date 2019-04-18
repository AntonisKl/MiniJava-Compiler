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
}