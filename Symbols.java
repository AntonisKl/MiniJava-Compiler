import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;
import java.util.List;

class Symbols {
	public Map<String, ClassMaps> classesMaps;
	public Map<String, String> inheritances;

	public Symbols() {
		classesMaps = new HashMap<String, ClassMaps>();
		inheritances = new HashMap<String, String>();
	}

	public int getMethodParamsNum(String className, String methodName) {
		return classesMaps.get(className).methodParamTypes.get(methodName).size();
	}

	public String getMethodType(String className, String methodName) {
		return classesMaps.get(className).methodTypes.get(methodName);
	}

	public void checkClassExists(String className, String lineInfo) throws TypeCheckingException {
		if (classesMaps.get(className) == null) {
			throw new TypeCheckingException("Class has not been declared -> Line: " + lineInfo);
		}
		return;
	}

	@Override
	public String toString() {
		//   System.out.println("Classes' maps:");
		//   System.out.println((new PrettyPrintingMap(classesMaps)).toString());
		//   System.out.println("Inheritances:");
		//   System.out.println((new PrettyPrintingMap(inheritances)).toString());

		return "Classes' maps:\n" + (new PrettyPrintingMap(classesMaps)).toString() + "\n\nInheritances:\n\t"
				+ (new PrettyPrintingMap(inheritances)).toString();
	}
}