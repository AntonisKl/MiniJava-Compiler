package MyClasses;

import java.util.LinkedHashMap;
import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;
import java.util.List;
import java.util.Iterator;
import java.util.Map.*;

public class Symbols {
	public Map<String, ClassMaps> classesMaps; // <class's name, object of type ClassMaps that contains all the useful maps for the class>
	public Map<String, String> inheritances; // <class's name, inherited class's name>

	public Symbols() {
		classesMaps = new LinkedHashMap<String, ClassMaps>(); // LinkedHashMap because we want to maintain the insertion order of elements for printing purposes
		inheritances = new HashMap<String, String>();
	}

	// String id: name of the variable
	// String className: we are in this class's context
	// String methodName: we are in this method's context
	// boolean checkClassScope: true -> check the variable type by searching into both the method's scope and the class's scope
	//							false -> check the variable type by searching into the method's scope only
	public String getVarType(String id, String className, String methodName, String lineInfo, boolean checkClassScope)
			throws TypeCheckingException {
		Map<String, String> curClassVarTypes = classesMaps.get(className).varTypes;
		Map<String, String> curMethodVarTypes = classesMaps.get(className).methodVarTypes.get(methodName);
		String methodScopeVarType = curMethodVarTypes.get(id);

		if (!checkClassScope)
			// checked only in current method's scope
			return methodScopeVarType;

		// continue checking in the class's scope as well
		String classScopeVarType = curClassVarTypes.get(id);

		if (methodScopeVarType == null && classScopeVarType == null) {
			// check in inherited classes' scopes
			String curClassName = className;
			while (inheritances.get(curClassName) != null) {
				curClassName = inheritances.get(curClassName);

				if (classesMaps.get(curClassName).varTypes.get(id) != null)
					// found the variable
					return classesMaps.get(curClassName).varTypes.get(id);
			}

			// variable not found
			throw new TypeCheckingException("Variable not declared -> Line: " + lineInfo);

		}

		// return the variable's type giving priority to method's scope
		return methodScopeVarType != null ? methodScopeVarType : classScopeVarType;

	}

	// String id: name of the variable
	// returns the variable's type that is found in the current classe's scope
	public String getVarType(String id, String className, String lineInfo) throws TypeCheckingException {
		return classesMaps.get(className).varTypes.get(id);
	}

	// String id: name of the method
	// returns the method's type
	public String getMethodType(String id, String className, String lineInfo, boolean throwNotDeclared)
			throws TypeCheckingException {
		Map<String, String> curClassMethodTypes = classesMaps.get(className).methodTypes;
		String classScopeMethodType = curClassMethodTypes.get(id);

		if (classScopeMethodType == null) {
			// search into the inherited classes' scopes
			String curClassName = className;
			while (inheritances.get(curClassName) != null) {
				curClassName = inheritances.get(curClassName);

				if (classesMaps.get(curClassName).methodTypes.get(id) != null)
					// method found
					return classesMaps.get(curClassName).methodTypes.get(id);
			}

			if (throwNotDeclared) {
				throw new TypeCheckingException("Method not declared -> Line: " + lineInfo);
			}
		}

		return classScopeMethodType;
	}

	// String id: name of the class
	// throws custom exception if the class is declared
	public void checkClassDeclared(String id, String lineInfo) throws TypeCheckingException {
		ClassMaps foundClassMaps = classesMaps.get(id);
		if (foundClassMaps != null) {
			throw new TypeCheckingException("Class declared twice -> Line: " + lineInfo);
		}

		return;
	}

	// String id: name of the class
	// throws custom exception if the class is not declared
	public void checkClassNotDeclared(String id, String lineInfo, String msg) throws TypeCheckingException {
		ClassMaps foundClassMaps = classesMaps.get(id);
		if (foundClassMaps == null) {
			throw new TypeCheckingException(msg + " -> Line: " + lineInfo);
		}

		return;
	}

	// String className: name of the class in which the method is declared
	// returns the number of parameters of method with name methodName
	public int getMethodParamsNum(String className, String methodName, String lineInfo) throws TypeCheckingException {
		Map<String, List<String>> curClassMethodParamTypes = classesMaps.get(className).methodParamTypes;
		List<String> classScopeMethodParamTypes = curClassMethodParamTypes.get(methodName);

		if (classScopeMethodParamTypes == null) {
			// search into the inherited classes' scopes
			String curClassName = className;
			while (inheritances.get(curClassName) != null) {
				curClassName = inheritances.get(curClassName);

				if (classesMaps.get(curClassName).methodParamTypes.get(methodName) != null)
					// method found
					return classesMaps.get(curClassName).methodParamTypes.get(methodName).size();
			}

			throw new TypeCheckingException("Method not declared -> Line: " + lineInfo);
		}

		return classScopeMethodParamTypes.size();
	}

	// returns method type just by accessing the maps
	public String getMethodType(String className, String methodName) {
		return classesMaps.get(className).methodTypes.get(methodName);
	}

	// String className: name of class of the inheritance hierarchy from which we will begin the search
	// returns the name of the first class in the inheritance hierarchy
	public String getFirstInheritedClassName(String className) {
		String curClassName = className;
		while (curClassName != null) {
			if (inheritances.get(curClassName) != null) {
				// continue searching
				curClassName = inheritances.get(curClassName);
			} else {
				if (curClassName == className)
					// no inherited classes
					return null;

				// found top class
				return curClassName;
			}
		}

		return null; // will not happen
	}

	// String className: name of class to check its parent classes
	// String type: type to be matched
	// compares the names of the parent classes with the type given as argument
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

	// for offset printing
	private String classesOffsetsToString(Map<String, ClassMaps> map) {
		String returnValue = "";
		Iterator<Entry<String, ClassMaps>> iter = map.entrySet().iterator();
		while (iter.hasNext()) {
			Entry<String, ClassMaps> entry = iter.next();
			returnValue += entry.getValue().offsetsToString(entry.getKey());
		}

		return returnValue;
	}

	@Override
	public String toString() {
		// uncomment for debbuging
		// return "Classes' maps:\n" + (new PrettyPrintingMap(classesMaps)).toString() + "\n\nInheritances:\n\t"
		// 		+ (new PrettyPrintingMap(inheritances)).toString();

		return classesOffsetsToString(classesMaps);
	}
}