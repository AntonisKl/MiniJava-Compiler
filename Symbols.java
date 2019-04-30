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

	public String getVarType(String id, String className, String methodName, String lineInfo, boolean checkClassScope) // ADD A MODE FOR DECLARATION AND EXPRESSION
			throws TypeCheckingException {
		Map<String, String> curClassVarTypes = classesMaps.get(className).varTypes;
		Map<String, String> curMethodVarTypes = classesMaps.get(className).methodVarTypes.get(methodName);
		// if (id == "new_node") {
		//    System.out.println((new PrettyPrintingMap(curMethodVarTypes)).toString());
		// }
		String methodScopeVarType = curMethodVarTypes.get(id);

		if (!checkClassScope)
			return methodScopeVarType;

		// System.out.println(methodScopeVarType);
		String classScopeVarType = curClassVarTypes.get(id);

		if (methodScopeVarType == null && classScopeVarType == null) {
			String curClassName = className;
			while (inheritances.get(curClassName) != null) {
				curClassName = inheritances.get(curClassName);

				if (classesMaps.get(curClassName).varTypes.get(id) != null)
					return classesMaps.get(curClassName).varTypes.get(id);
			}

			throw new TypeCheckingException("Variable not declared -> Line: " + lineInfo);

		}
		// if (methodScopeIdType != null)
		//    return;

		return methodScopeVarType != null ? methodScopeVarType : classScopeVarType;

		// classScopeIdType != null
	}

	public String getVarType(String id, String className, String lineInfo)
			throws TypeCheckingException {
		Map<String, String> curClassVarTypes = classesMaps.get(className).varTypes;
		// if (id == "new_node") {
		//    System.out.println((new PrettyPrintingMap(curMethodVarTypes)).toString());
		// }
		// System.out.println(methodScopeVarType);
		String classScopeVarType = curClassVarTypes.get(id);

		// if (!checkParentClasses)
		// 	return classScopeVarType;

		// if (classScopeVarType == null) {
		// 	String curClassName = className;
		// 	while (inheritances.get(curClassName) != null) {
		// 		curClassName = inheritances.get(curClassName);

		// 		if (classesMaps.get(curClassName).varTypes.get(id) != null)
		// 			return classesMaps.get(curClassName).varTypes.get(id);
		// 	}

		// }
		// if (methodScopeIdType != null)
		//    return;

		return classScopeVarType;

		// classScopeIdType != null
	}

	public String getMethodType(String id, String className, String lineInfo, boolean throwNotDeclared)
			throws TypeCheckingException { // ADD A MODE FOR DECLARATION AND EXPRESSION
		Map<String, String> curClassMethodTypes = classesMaps.get(className).methodTypes;
		String classScopeMethodType = curClassMethodTypes.get(id);

		if (classScopeMethodType == null) {
			String curClassName = className;
			while (inheritances.get(curClassName) != null) {
				curClassName = inheritances.get(curClassName);

				if (classesMaps.get(curClassName).methodTypes.get(id) != null)
					return classesMaps.get(curClassName).methodTypes.get(id);
			}

			if (throwNotDeclared) {
				throw new TypeCheckingException("Method not declared -> Line: " + lineInfo);
			}
		}

		return classScopeMethodType;
	}

	public void checkClassDeclared(String id, String lineInfo) throws TypeCheckingException {
		ClassMaps foundClassMaps = classesMaps.get(id);
		// System.out.println("id: " + id);
		if (foundClassMaps != null) {
			throw new TypeCheckingException("Class declared twice -> Line: " + lineInfo);
		}

		return;
	}

	public void checkClassNotDeclared(String id, String lineInfo) throws TypeCheckingException {
		ClassMaps foundClassMaps = classesMaps.get(id);
		// System.out.println("id: " + id);
		if (foundClassMaps == null) {
			throw new TypeCheckingException("Parent class not declared -> Line: " + lineInfo);
		}

		return;
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

	public String getFirstInheritedClassName(String className) {
		String curClassName = className;
		while (curClassName != null) {
			if (inheritances.get(curClassName) != null) {
				curClassName = inheritances.get(curClassName);
			} else {
				if (curClassName == className)
					return null;

				return curClassName;
			}
		}

		return null; // will not happen
	}

	public void checkMatchParentClassTypes(String className, String type, String lineInfo)
			throws TypeCheckingException {
		boolean typeMatched = false;
		String curClassName = className;
		while (curClassName != null) {
			if (curClassName.equals(type)) {
				typeMatched = true;
				break;
			}

			curClassName = inheritances.get(curClassName);
		}
		if (!typeMatched) {
			throw new TypeCheckingException("Invalid parameter -> Line: " + lineInfo);
		}
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