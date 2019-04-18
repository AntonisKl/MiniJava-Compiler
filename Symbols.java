import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;
import java.util.List;

class Symbols {
	public List<ClassMaps> classesMaps;
	public Map<String, String> inheritances;

	public Symbols() {
		classesMaps = new ArrayList<ClassMaps>();
		inheritances = new HashMap<String, String>();
	}
}