package MyClasses;

import syntaxtree.*;
import visitor.GJDepthFirst;
import java.util.Map;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Iterator;

// TypeCheckingVisitor does type-checking and creates and returns the IR v-table declarations
public class TypeCheckingVisitor extends GJDepthFirst<String, String[]> {

   // these are used as defines
   final static String METHOD = "method";
   final static String CLASS = "class";

   final static String BOOLEAN = "boolean";
   final static String INT = "int";
   final static String INT_ARRAY = "int[]";

   // int curMethodParamIndex: curent index of method's parameter -> used for type checking of method's parameters and arguments
   // int curOffset: current offset's value
   int curMethodParamIndex, curOffset;
   String vTableDeclarations, vTableMethodDeclarations, vTableCurMethodParamTypes;
   Map<String, String> classVtableDeclerations; // used when we build the v-table declaration for a child class

   Symbols symbols;

   // MY FUNCTIONS

   public TypeCheckingVisitor(Symbols symbols) {
      this.symbols = symbols;
      classVtableDeclerations = new HashMap<String, String>();
   }

   // returns the proper offset according to type argument
   private int getOffsetPerType(String type) {
      switch (type) {
      case BOOLEAN:
         return 1;
      case INT:
         return 4;
      default:
         return 8;
      }
   }

   // returns true if s1 or s2 are equal with value
   // returns false otherwise
   private boolean atLeastOneEquals(String s1, String s2, String value) {
      return s1.equals(value) || s2.equals(value);
   }

   // returns true if s1 and s2 are equal with value
   // returns false otherwise
   private boolean bothEquals(String s1, String s2, String value) {
      return s1.equals(value) && s2.equals(value);
   }

   // returns the proper expression's type according to the types (type1, type2) of the two operands of the expression 
   private String getExpType(String type1, String type2, String operationType, String lineInfo)
         throws TypeCheckingException {

      String expType = INT; // default return type
      switch (operationType) {
      case "<":
         expType = BOOLEAN;
      case "+":
      case "-":
      case "*":
         if (atLeastOneEquals(type1, type2, INT_ARRAY) || atLeastOneEquals(type1, type2, BOOLEAN))
            throw new TypeCheckingException("Operation " + operationType + " is undefined for types " + type1 + ", "
                  + type2 + " -> Line: " + lineInfo);
         break;
      case "&&":
         expType = BOOLEAN;
         if (atLeastOneEquals(type1, type2, INT_ARRAY) || atLeastOneEquals(type1, type2, INT))
            throw new TypeCheckingException("Operation " + operationType + " is undefined for types " + type1 + ", "
                  + type2 + " -> Line: " + lineInfo);
         break;
      }

      return expType;
   }

   // String paramType: current parameter's type to be checked
   // checks if paramType is the same type with current method's current parameter
   private void checkParamType(String paramType, String className, String methodName, String lineInfo)
         throws TypeCheckingException {
      String curOriginalParamType = symbols.classesMaps.get(className).methodParamTypes.get(methodName)
            .get(curMethodParamIndex); // current parameter's original declared type
      String firstInheritedClassName = symbols.getFirstInheritedClassName(paramType);

      if (firstInheritedClassName == null) { // is a primitive type
         if (!curOriginalParamType.equals(paramType)) {
            throw new TypeCheckingException("Invalid parameter -> Line: " + lineInfo);
         }
      } else { // is a class type
         // check if one of the parent classes' name match
         symbols.checkMatchParentClassTypes(paramType, curOriginalParamType, lineInfo);
      }

      return;
   }

   // checks if type is the same as originalType or a sub-type of originalType
   private void checkReturnType(String type, String originalType, String lineInfo) throws TypeCheckingException {
      String firstInheritedClassName = symbols.getFirstInheritedClassName(type);

      if (firstInheritedClassName == null) { // is a primitive type
         if (!originalType.equals(type)) {
            throw new TypeCheckingException("Invalid return expression type -> Line:" + lineInfo);
         }
      } else { // is a class type
         // check if one of the parent classes' name match
         symbols.checkMatchParentClassTypes(type, originalType, lineInfo);
      }

      return;
   }

   String getIRType(String type) {
      switch (type) {
      case BOOLEAN:
         return "i1";
      case INT:
         return "i32";
      case INT_ARRAY:
         return "i32*";
      default:
         return "i8*";
      }
   }

   // END OF MY FUNCTIONS

   /**
   * f0 -> MainClass()
   * f1 -> ( TypeDeclaration() )*
   * f2 -> <EOF>
   */
   public String visit(Goal n, String[] argu) {
      vTableDeclarations = "";
      n.f0.accept(this, argu);
      n.f1.accept(this, argu);
      n.f2.accept(this, argu);
      return vTableDeclarations;
   }

   /**
    * f0 -> "class"
    * f1 -> Identifier()
    * f2 -> "{"
    * f3 -> "public"
    * f4 -> "static"
    * f5 -> "void"
    * f6 -> "main"
    * f7 -> "("
    * f8 -> "String"
    * f9 -> "["
    * f10 -> "]"
    * f11 -> Identifier()
    * f12 -> ")"
    * f13 -> "{"
    * f14 -> ( VarDeclaration() )*
    * f15 -> ( Statement() )*
    * f16 -> "}"
    * f17 -> "}"
    */
   public String visit(MainClass n, String[] argu) {
      String _ret = null;
      n.f0.accept(this, argu);
      String id1 = n.f1.accept(this, argu);
      n.f2.accept(this, argu);
      n.f3.accept(this, argu);
      n.f4.accept(this, argu);
      n.f5.accept(this, argu);
      n.f6.accept(this, argu);
      n.f7.accept(this, argu);
      n.f8.accept(this, argu);
      n.f9.accept(this, argu);
      n.f10.accept(this, argu);
      n.f11.accept(this, argu);
      n.f12.accept(this, argu);
      n.f13.accept(this, argu);
      // n.f14.accept(this, argu); // not needed

      String id2 = "main";
      n.f15.accept(this, new String[] { METHOD, id2, CLASS, id1 });
      n.f16.accept(this, argu);
      n.f17.accept(this, argu);

      String curVtableDecl = "@." + id1 + "_vtable = global [0 x i8*] []\n";
      vTableDeclarations += curVtableDecl;
      classVtableDeclerations.put(id1, curVtableDecl);

      return _ret;
   }

   /**
    * f0 -> ClassDeclaration()
    *       | ClassExtendsDeclaration()
    */
   public String visit(TypeDeclaration n, String[] argu) {
      vTableDeclarations += (n.f0.accept(this, argu) + "\n");
      return null;
   }

   /**
    * f0 -> "class"
    * f1 -> Identifier()
    * f2 -> "{"
    * f3 -> ( VarDeclaration() )*
    * f4 -> ( MethodDeclaration() )*
    * f5 -> "}"
    */
   public String visit(ClassDeclaration n, String[] argu) {
      n.f0.accept(this, argu);
      String id = n.f1.accept(this, argu);
      n.f2.accept(this, argu);
      curOffset = 0;
      n.f3.accept(this, new String[] { id });
      curOffset = 0;
      vTableMethodDeclarations = "";
      n.f4.accept(this, new String[] { id });
      if (n.f4.present()) {
         vTableMethodDeclarations = vTableMethodDeclarations.substring(0, vTableMethodDeclarations.length() - 2); // remove last two characters: ", "
      }

      n.f5.accept(this, argu);
      String curVtableDecl = "@." + id + "_vtable = global [" + n.f4.size() + " x i8*] [" + vTableMethodDeclarations
            + "]";
      classVtableDeclerations.put(id, curVtableDecl);

      return curVtableDecl;
   }

   /**
    * f0 -> "class"
    * f1 -> Identifier()
    * f2 -> "extends"
    * f3 -> Identifier()
    * f4 -> "{"
    * f5 -> ( VarDeclaration() )*
    * f6 -> ( MethodDeclaration() )*
    * f7 -> "}"
    */
   public String visit(ClassExtendsDeclaration n, String[] argu) {
      n.f0.accept(this, argu);
      String id = n.f1.accept(this, argu);
      n.f2.accept(this, argu);
      String id1 = n.f3.accept(this, argu);
      n.f4.accept(this, argu);

      // calculate current variable's offset and update curOffset variable
      Map<String, Integer> varOffsets = symbols.classesMaps.get(id1).varOffsets;
      Iterator<Map.Entry<String, Integer>> entries = null;
      Map.Entry<String, Integer> lastEntry = null;
      if (varOffsets.size() > 0) {
         entries = varOffsets.entrySet().iterator();
         lastEntry = null;
         // get last entry of map
         while (entries.hasNext()) {
            lastEntry = entries.next();
         }
         curOffset = varOffsets.get(lastEntry.getKey())
               + getOffsetPerType(symbols.classesMaps.get(id1).varTypes.get(lastEntry.getKey()));
      } else {
         curOffset = 0;
      }
      n.f5.accept(this, new String[] { id });

      // calculate current method's offset and update curOffset variable
      Map<String, Integer> methodOffsets = symbols.classesMaps.get(id1).methodOffsets;
      if (methodOffsets.size() > 0) {

         entries = methodOffsets.entrySet().iterator();
         while (entries.hasNext()) {
            lastEntry = entries.next();
         }
         curOffset = methodOffsets.get(lastEntry.getKey()) + getOffsetPerType("method");
      } else {
         curOffset = 0;
      }

      vTableMethodDeclarations = ""; // declarations of methods that only exist in the current (child) class and NOT in the parent class
      n.f6.accept(this, new String[] { id });
      if (!vTableMethodDeclarations.equals("")) {
         vTableMethodDeclarations = vTableMethodDeclarations.substring(0, vTableMethodDeclarations.length() - 2); // remove last two characters: ", "
      }

      String parentVTableDecleration = classVtableDeclerations.get(id1);
      String curVtableDecl = "";

      curVtableDecl = parentVTableDecleration.replace(id1 + "_vtable", id + "_vtable");
      String parentName = "@" + id1 + ".";
      if (curVtableDecl.contains(parentName)) {
         curVtableDecl = curVtableDecl.replaceAll(parentName, "@" + id + ".");
      }

      if (!vTableMethodDeclarations.equals("")) {
         curVtableDecl = curVtableDecl.substring(0, curVtableDecl.length() - 1); // remove last character: "]"
         curVtableDecl += (", " + vTableMethodDeclarations + "]");
         curVtableDecl = curVtableDecl.replaceAll("([0-9]+) x i8*",
               (vTableMethodDeclarations.split("\\),").length + symbols.classesMaps.get(id1).methodTypes.size())
                     + " x i8*");
      }

      n.f7.accept(this, argu);
      return curVtableDecl;
   }

   /**
    * f0 -> Type()
    * f1 -> Identifier()
    * f2 -> ";"
    */
   // argu[0]: class name
   public String visit(VarDeclaration n, String[] argu) {

      String _ret = null;
      String type = n.f0.accept(this, argu);
      String id = n.f1.accept(this, argu);

      // update variable offsets' map
      symbols.classesMaps.get(argu[0]).varOffsets.put(id, curOffset);
      // update curOffset variable to prepare it for a potential next variable declaration
      curOffset += getOffsetPerType(type);

      n.f2.accept(this, argu);
      return _ret;
   }

   /**
    * f0 -> "public"
    * f1 -> Type()
    * f2 -> Identifier()
    * f3 -> "("
    * f4 -> ( FormalParameterList() )?
    * f5 -> ")"
    * f6 -> "{"
    * f7 -> ( VarDeclaration() )*
    * f8 -> ( Statement() )*
    * f9 -> "return"
    * f10 -> Expression()
    * f11 -> ";"
    * f12 -> "}"
    */
   // argu[0]: class name
   public String visit(MethodDeclaration n, String[] argu) throws TypeCheckingException {
      String _ret = null;
      n.f0.accept(this, argu);
      n.f1.accept(this, argu);
      String id = n.f2.accept(this, argu);
      n.f3.accept(this, argu);

      boolean handleOffset = true;
      String[] foundClassNameAndMethodType = symbols.getLastInheritedMethodType(argu[0], id);
      String foundInheritedClassName = foundClassNameAndMethodType == null ? null : foundClassNameAndMethodType[0];
      String foundInheritedClassMethodType = foundClassNameAndMethodType == null ? null
            : foundClassNameAndMethodType[1];
      // if (foundInheritedClassName != null) {
      //    foundInheritedClassMethodType = symbols.getMethodType(foundInheritedClassName, id);
      // }

      if (foundInheritedClassMethodType != null) {
         // compare method's type with the original method's declaration type 
         if (!symbols.getMethodType(argu[0], id).equals(foundInheritedClassMethodType)) {
            throw new TypeCheckingException("Invalid method type in child class -> Line:" + n.f3.beginLine);
         }
         // should not add an entry to method offsets' map
         handleOffset = false;
      }

      vTableCurMethodParamTypes = "i8*, "; // for "this" pointer
      if (n.f4.present()) {
         // check parameters' number and types and gather param types for v-table declaration
         n.f4.accept(this, new String[] { CLASS, argu[0], METHOD, id, CLASS, foundInheritedClassName });
      }
      vTableCurMethodParamTypes = vTableCurMethodParamTypes.substring(0, vTableCurMethodParamTypes.length() - 2); // remove last two characters: ", "

      // check parameters' number
      if (foundInheritedClassMethodType != null && !n.f4.present()
            && symbols.getMethodParamsNum(foundInheritedClassName, id, Integer.toString(n.f5.beginLine)) > 0) {
         throw new TypeCheckingException("Invalid parameters number -> Line:" + n.f3.beginLine);
      }

      n.f5.accept(this, argu);
      n.f6.accept(this, argu);
      // n.f7.accept(this, argu); // not needed
      n.f8.accept(this, new String[] { METHOD, id, CLASS, argu[0] });
      n.f9.accept(this, argu);
      String retType = n.f10.accept(this, new String[] { METHOD, id, CLASS, argu[0] });

      n.f11.accept(this, argu);

      // check validity of return expression's type
      checkReturnType(retType, symbols.getMethodType(argu[0], id), Integer.toString(n.f11.beginLine));

      n.f12.accept(this, argu);

      if (handleOffset) {
         // update method offsets' map
         symbols.classesMaps.get(argu[0]).methodOffsets.put(id, curOffset);
         // update curOffset variable to prepare it for a potential next method declaration
         curOffset += getOffsetPerType("method");
      }

      if (foundInheritedClassMethodType == null) {
         vTableMethodDeclarations += "i8* bitcast (" + getIRType(retType) + " (" + vTableCurMethodParamTypes + ")* @"
               + argu[0] + "." + id + " to i8*), "; // add entry to v-table string
         System.out.println("hi :" + vTableMethodDeclarations);
      }

      return _ret;
   }

   /**
    * f0 -> FormalParameter()
    * f1 -> FormalParameterTail()
    */
   // argu[0]: "class", argu[1]: name of current class, argu[2]: "method", argu[3]: name of declared method, argu[4]: "class", argu[5]: name of first inherited class
   public String visit(FormalParameterList n, String[] argu) throws TypeCheckingException {

      if (argu[5] != null && symbols.getMethodParamsNum(argu[5], argu[3], null) == 0) {
         throw new TypeCheckingException("Invalid parameters number -> Line: N/A");
      }

      String _ret = null;

      curMethodParamIndex = 0; // reset current index

      String paramType = n.f0.accept(this, argu);

      if (argu[5] != null) {
         // do type-checking for the first parameter
         checkParamType(paramType, argu[5], argu[3], null);
      }

      vTableCurMethodParamTypes += (getIRType(paramType) + ", ");

      n.f1.accept(this, argu);
      return _ret;
   }

   /**
    * f0 -> Type()
    * f1 -> Identifier()
    */
   // argu[0]: "class", argu[1]: name of current class, argu[2]: "method", argu[3]: name of declared method, argu[4]: "class", argu[5]: name of first inherited class
   public String visit(FormalParameter n, String[] argu) {
      String type = n.f0.accept(this, argu);
      n.f1.accept(this, argu);

      return type; // returns the type of current parameter
   }

   /**
    * f0 -> ( FormalParameterTerm() )*
    */
   // argu[0]: "class", argu[1]: name of current class, argu[2]: "method", argu[3]: name of declared method, argu[4]: "class", argu[5]: name of first inherited class
   public String visit(FormalParameterTail n, String[] argu) {
      return n.f0.accept(this, argu);
   }

   /**
    * f0 -> ","
    * f1 -> FormalParameter()
    */
   // argu[0]: "class", argu[1]: name of current class, argu[2]: "method", argu[3]: name of declared method, argu[4]: "class", argu[5]: name of first inherited class
   public String visit(FormalParameterTerm n, String[] argu) throws TypeCheckingException {
      curMethodParamIndex++;

      String _ret = null;
      n.f0.accept(this, argu);

      // check if parameter's number exceeded the correct one
      if (argu[5] != null /* if there is at a valid first inherited method */ && curMethodParamIndex >= symbols.getMethodParamsNum(argu[5], argu[3], Integer.toString(n.f0.beginLine))) {
         throw new TypeCheckingException("Invalid parameters number -> Line:" + n.f0.beginLine);
      }

      String expType = n.f1.accept(this, argu);

      if (argu[5] != null) { // if there is at a valid first inherited method
         // do type-checking for current parameter
         checkParamType(expType, argu[5], argu[3], null);
      }

      return _ret;
   }

   /**
    * f0 -> ArrayType()
    *       | BooleanType()
    *       | IntegerType()
    *       | Identifier()
    */
   public String visit(Type n, String[] argu) {
      return n.f0.accept(this, argu);
   }

   /**
    * f0 -> "int"
    * f1 -> "["
    * f2 -> "]"
    */
   public String visit(ArrayType n, String[] argu) {
      n.f0.accept(this, argu);
      n.f1.accept(this, argu);
      n.f2.accept(this, argu);
      return n.f0.toString() + n.f1.toString() + n.f2.toString();
   }

   /**
    * f0 -> "boolean"
    */
   public String visit(BooleanType n, String[] argu) {
      return n.f0.toString();
   }

   /**
    * f0 -> "int"
    */
   public String visit(IntegerType n, String[] argu) {
      return n.f0.toString();
   }

   /**
    * f0 -> Block()
    *       | AssignmentStatement()
    *       | ArrayAssignmentStatement()
    *       | IfStatement()
    *       | WhileStatement()
    *       | PrintStatement()
    */

   // argu[0]: "method", argu[1]: name of method, argu[2]: "class", argu[3]: name of class
   public String visit(Statement n, String[] argu) {
      return n.f0.accept(this, argu);
   }

   /**
    * f0 -> "{"
    * f1 -> ( Statement() )*
    * f2 -> "}"
    */
   // argu[0]: "method", argu[1]: name of method, argu[2]: "class", argu[3]: name of class
   public String visit(Block n, String[] argu) {
      String _ret = null;
      n.f0.accept(this, argu);
      n.f1.accept(this, argu);
      n.f2.accept(this, argu);
      return _ret;
   }

   /**
    * f0 -> Identifier()
    * f1 -> "="
    * f2 -> Expression()
    * f3 -> ";"
    */
   // argu[0]: "method", argu[1]: name of method, argu[2]: "class", argu[3]: name of class
   public String visit(AssignmentStatement n, String[] argu) throws TypeCheckingException {
      String _ret = null;

      String id = n.f0.accept(this, argu);

      String idType = symbols.getVarType(id, argu[3], argu[1], Integer.toString(n.f1.beginLine), true);

      n.f1.accept(this, argu);
      String expType = n.f2.accept(this, argu);
      n.f3.accept(this, argu);

      String firstInheritedClassName = symbols.getFirstInheritedClassName(expType);

      if (firstInheritedClassName == null) { // is a primitive type
         // do type-checking
         if (!expType.equals(idType)) {
            throw new TypeCheckingException("Left and right parts of expression have different types -> Line: "
                  + Integer.toString(n.f3.beginLine));
         }
      } else { // is a class type
         // do type-checking by going up in the inheritance chain
         symbols.checkMatchParentClassTypes(expType, idType, Integer.toString(n.f3.beginLine));
      }

      return _ret;
   }

   /**
    * f0 -> Identifier()
    * f1 -> "["
    * f2 -> Expression()
    * f3 -> "]"
    * f4 -> "="
    * f5 -> Expression()
    * f6 -> ";"
    */
   // argu[0]: "method", argu[1]: name of method, argu[2]: "class", argu[3]: name of class
   public String visit(ArrayAssignmentStatement n, String[] argu) throws TypeCheckingException {
      String _ret = null;
      String id = n.f0.accept(this, argu);
      n.f1.accept(this, argu);

      // type-check id
      String idType = symbols.getVarType(id, argu[3], argu[1], Integer.toString(n.f1.beginLine), true);
      if (!idType.equals(INT_ARRAY)) {
         throw new TypeCheckingException("Left part of array assignment statement is not of type int array -> Line: "
               + Integer.toString(n.f1.beginLine));
      }

      String expType = n.f2.accept(this, argu);
      n.f3.accept(this, argu);

      // type-check index expression
      if (!expType.equals(INT)) {
         throw new TypeCheckingException(
               "Identifier of array index is not of type int -> Line: " + Integer.toString(n.f3.beginLine));
      }

      n.f4.accept(this, argu);
      expType = n.f5.accept(this, argu);
      n.f6.accept(this, argu);

      // type-check right part of array assignment
      if (!expType.equals(INT)) {
         throw new TypeCheckingException(
               "Right part of array assignment is not of type int -> Line: " + Integer.toString(n.f6.beginLine));
      }
      return _ret;
   }

   /**
    * f0 -> "if"
    * f1 -> "("
    * f2 -> Expression()
    * f3 -> ")"
    * f4 -> Statement()
    * f5 -> "else"
    * f6 -> Statement()
    */
   // argu[0]: "method", argu[1]: name of method, argu[2]: "class", argu[3]: name of class
   public String visit(IfStatement n, String[] argu) throws TypeCheckingException {
      String _ret = null;
      n.f0.accept(this, argu);
      n.f1.accept(this, argu);
      String expType = n.f2.accept(this, argu);
      n.f3.accept(this, argu);

      // type-check condition expression
      if (!expType.equals(BOOLEAN)) {
         throw new TypeCheckingException(
               "Condition of if statement is not of type boolean -> Line: " + Integer.toString(n.f3.beginLine));
      }

      n.f4.accept(this, argu);
      n.f5.accept(this, argu);
      n.f6.accept(this, argu);
      return _ret;
   }

   /**
    * f0 -> "while"
    * f1 -> "("
    * f2 -> Expression()
    * f3 -> ")"
    * f4 -> Statement()
    */
   // argu[0]: "method", argu[1]: name of method, argu[2]: "class", argu[3]: name of class
   public String visit(WhileStatement n, String[] argu) throws TypeCheckingException {
      String _ret = null;
      n.f0.accept(this, argu);
      n.f1.accept(this, argu);
      String expType = n.f2.accept(this, argu);
      n.f3.accept(this, argu);

      // type-check condition expression
      if (!expType.equals(BOOLEAN)) {
         throw new TypeCheckingException(
               "Condition of while statement is not of type boolean -> Line: " + Integer.toString(n.f3.beginLine));
      }

      n.f4.accept(this, argu);
      return _ret;
   }

   /**
    * f0 -> "System.out.println"
    * f1 -> "("
    * f2 -> Expression()
    * f3 -> ")"
    * f4 -> ";"
    */
   // argu[0]: "method", argu[1]: name of method, argu[2]: "class", argu[3]: name of class
   public String visit(PrintStatement n, String[] argu) throws TypeCheckingException {
      String _ret = null;
      n.f0.accept(this, argu);
      n.f1.accept(this, argu);
      String expType = n.f2.accept(this, argu);

      // type-check print expression
      if (!expType.equals(BOOLEAN) && !expType.equals(INT)) {
         throw new TypeCheckingException(
               "Expression of print statement is of reference type -> Line: " + Integer.toString(n.f3.beginLine));
      }

      n.f3.accept(this, argu);
      n.f4.accept(this, argu);
      return _ret;
   }

   /**
    * f0 -> AndExpression()
    *       | CompareExpression()
    *       | PlusExpression()
    *       | MinusExpression()
    *       | TimesExpression()
    *       | ArrayLookup()
    *       | ArrayLength()
    *       | MessageSend()
    *       | Clause()
    */
   public String visit(Expression n, String[] argu) {
      return n.f0.accept(this, argu);
   }

   /**
    * f0 -> Clause()
    * f1 -> "&&"
    * f2 -> Clause()
    */
   public String visit(AndExpression n, String[] argu) throws TypeCheckingException {
      String type1 = n.f0.accept(this, argu);
      n.f1.accept(this, argu);
      String type2 = n.f2.accept(this, argu);

      // return proper expression's type
      return getExpType(type1, type2, n.f1.toString(), Integer.toString(n.f1.beginLine));
   }

   /**
    * f0 -> PrimaryExpression()
    * f1 -> "<"
    * f2 -> PrimaryExpression()
    */
   public String visit(CompareExpression n, String[] argu) throws TypeCheckingException {
      String type1 = n.f0.accept(this, argu);
      n.f1.accept(this, argu);
      String type2 = n.f2.accept(this, argu);

      return getExpType(type1, type2, n.f1.toString(), Integer.toString(n.f1.beginLine));
   }

   /**
    * f0 -> PrimaryExpression()
    * f1 -> "+"
    * f2 -> PrimaryExpression()
    */
   public String visit(PlusExpression n, String[] argu) throws TypeCheckingException {
      String type1 = n.f0.accept(this, argu);
      n.f1.accept(this, argu);
      String type2 = n.f2.accept(this, argu);

      return getExpType(type1, type2, n.f1.toString(), Integer.toString(n.f1.beginLine));
   }

   /**
    * f0 -> PrimaryExpression()
    * f1 -> "-"
    * f2 -> PrimaryExpression()
    */
   public String visit(MinusExpression n, String[] argu) throws TypeCheckingException {
      String type1 = n.f0.accept(this, argu);
      n.f1.accept(this, argu);
      String type2 = n.f2.accept(this, argu);

      return getExpType(type1, type2, n.f1.toString(), Integer.toString(n.f1.beginLine));
   }

   /**
    * f0 -> PrimaryExpression()
    * f1 -> "*"
    * f2 -> PrimaryExpression()
    */
   public String visit(TimesExpression n, String[] argu) throws TypeCheckingException {
      String type1 = n.f0.accept(this, argu);
      n.f1.accept(this, argu);
      String type2 = n.f2.accept(this, argu);

      return getExpType(type1, type2, n.f1.toString(), Integer.toString(n.f1.beginLine));
   }

   /**
    * f0 -> PrimaryExpression()
    * f1 -> "["
    * f2 -> PrimaryExpression()
    * f3 -> "]"
    */
   public String visit(ArrayLookup n, String[] argu) throws TypeCheckingException {
      String type1 = n.f0.accept(this, argu);
      n.f1.accept(this, argu);

      // type-check array id
      if (!type1.equals(INT_ARRAY)) {
         throw new TypeCheckingException("Identifier is not an int array -> Line: " + Integer.toString(n.f1.beginLine));
      }

      String type2 = n.f2.accept(this, argu);
      n.f3.accept(this, argu);

      // type-check array index
      if (!type2.equals(INT)) {
         throw new TypeCheckingException("Identifier is not an int -> Line: " + Integer.toString(n.f3.beginLine));
      }

      return INT;
   }

   /**
    * f0 -> PrimaryExpression()
    * f1 -> "."
    * f2 -> "length"
    */
   public String visit(ArrayLength n, String[] argu) throws TypeCheckingException {
      String type = n.f0.accept(this, argu);
      n.f1.accept(this, argu);

      // type-check array id
      if (!type.equals(INT_ARRAY)) {
         throw new TypeCheckingException("Identifier is not an int array -> Line: " + Integer.toString(n.f1.beginLine));
      }

      n.f2.accept(this, argu);
      return INT;
   }

   /**
    * f0 -> PrimaryExpression()
    * f1 -> "."
    * f2 -> Identifier()
    * f3 -> "("
    * f4 -> ( ExpressionList() )?
    * f5 -> ")"
    */
   // argu[0]: METHOD, argu[1]: context method's name, argu[2]: CLASS, argu[3]: context class's name
   // argu[4]: METHOD, argu[5]: called method's name, argu[6]: CLASS, argu[7]: called method's class name
   public String visit(MessageSend n, String[] argu) throws TypeCheckingException {
      String _ret = null;
      String type = n.f0.accept(this, argu);

      n.f1.accept(this, argu);

      String methodName = n.f2.accept(this, argu);

      // get method's return type
      String methodType = symbols.getMethodType(methodName, type, Integer.toString(n.f1.beginLine), true);

      n.f3.accept(this, argu);
      if (n.f4.present()) {
         // type-check arguments
         n.f4.accept(this, new String[] { METHOD, argu[1], CLASS, argu[3], METHOD, methodName, CLASS, type });
      } else {
         // check if original method's declaration had >0 parameters
         if (symbols.getMethodParamsNum(type, methodName, Integer.toString(n.f3.beginLine)) > 0) {
            throw new TypeCheckingException("Invalid parameters number -> Line:" + n.f3.beginLine);
         }
      }

      n.f5.accept(this, argu);

      if (!n.f4.present())
         return methodType;

      // check parameters number
      if (curMethodParamIndex != symbols.getMethodParamsNum(type, methodName, Integer.toString(n.f5.beginLine)) - 1) {
         throw new TypeCheckingException("Invalid parameters number -> Line:" + Integer.toString(n.f5.beginLine));
      }

      return methodType;
   }

   /**
    * f0 -> Expression()
    * f1 -> ExpressionTail()
    */

   // argu[0]: METHOD, argu[1]: context method's name, argu[2]: CLASS, argu[3]: context class's name
   // argu[4]: METHOD, argu[5]: called method's name, argu[6]: CLASS, argu[7]: called method's class name
   public String visit(ExpressionList n, String[] argu) throws TypeCheckingException {

      if (symbols.getMethodParamsNum(argu[7], argu[5], null) == 0) {
         throw new TypeCheckingException("Invalid parameters number -> Line: N/A");
      }

      String _ret = null;

      curMethodParamIndex = 0; // reset current argument's index

      String expType = n.f0.accept(this, argu);

      // check first argument's type
      checkParamType(expType, argu[7], argu[5], null);

      n.f1.accept(this, argu);
      return _ret;
   }

   /**
    * f0 -> ( ExpressionTerm() )*
    */
   // argu[0]: METHOD, argu[1]: context method's name, argu[2]: CLASS, argu[3]: context class's name
   // argu[4]: METHOD, argu[5]: called method's name, argu[6]: CLASS, argu[7]: called method's class name
   public String visit(ExpressionTail n, String[] argu) {
      return n.f0.accept(this, argu);
   }

   /**
    * f0 -> ","
    * f1 -> Expression()
    */
   // argu[0]: METHOD, argu[1]: context method's name, argu[2]: CLASS, argu[3]: context class's name
   // argu[4]: METHOD, argu[5]: called method's name, argu[6]: CLASS, argu[7]: called method's class name
   public String visit(ExpressionTerm n, String[] argu) throws TypeCheckingException {
      curMethodParamIndex++;

      String _ret = null;
      n.f0.accept(this, argu);

      // check if arguments's number exceeded the correct one
      if (curMethodParamIndex >= symbols.getMethodParamsNum(argu[7], argu[5], Integer.toString(n.f0.beginLine))) {
         throw new TypeCheckingException("Invalid parameters number -> Line:" + n.f0.beginLine);
      }

      String expType = n.f1.accept(this, argu);

      // check current argument's type
      checkParamType(expType, argu[7], argu[5], null);

      return _ret;
   }

   /**
    * f0 -> NotExpression()
    *       | PrimaryExpression()
    */
   public String visit(Clause n, String[] argu) {
      return n.f0.accept(this, argu);
   }

   /**
    * f0 -> IntegerLiteral()
    *       | TrueLiteral()
    *       | FalseLiteral()
    *       | Identifier()
    *       | ThisExpression()
    *       | ArrayAllocationExpression()
    *       | AllocationExpression()
    *       | BracketExpression()
    */
   // argu[0]: METHOD, argu[1]: context method's name, argu[2]: CLASS, argu[3]: context class's name
   // argu[4]: METHOD, argu[5]: called method's name, argu[6]: CLASS, argu[7]: called method's class name
   // returns the proper expression's type according to the choice between the above symbols
   public String visit(PrimaryExpression n, String[] argu) throws TypeCheckingException {
      String choice = n.f0.accept(this, argu);
      int choiceId = n.f0.which;
      switch (choiceId) {
      case 0:
         return INT;
      case 1:
      case 2:
         return BOOLEAN;
      case 3: // identifier
         return symbols.getVarType(choice, argu[3], argu[1], null, true);
      case 4:
         return argu[3]; // this class's name
      case 5:
         return INT_ARRAY;
      case 6:
      case 7:
         // 6: choice is the type constructor identifier (a class's name)
         // 7: choice is the result of the expression that is inside brackets
         return choice;
      }

      return null; // will not happen
   }

   /**
    * f0 -> <INTEGER_LITERAL>
    */
   public String visit(IntegerLiteral n, String[] argu) {
      return n.f0.toString();
   }

   /**
    * f0 -> "true"
    */
   public String visit(TrueLiteral n, String[] argu) {
      return n.f0.toString();
   }

   /**
    * f0 -> "false"
    */
   public String visit(FalseLiteral n, String[] argu) {
      return n.f0.toString();
   }

   /**
    * f0 -> <IDENTIFIER>
    */
   public String visit(Identifier n, String[] argu) {
      return n.f0.toString();
   }

   /**
    * f0 -> "this"
    */
   public String visit(ThisExpression n, String[] argu) {
      return n.f0.toString();
   }

   /**
    * f0 -> "new"
    * f1 -> "int"
    * f2 -> "["
    * f3 -> Expression()
    * f4 -> "]"
    */
   public String visit(ArrayAllocationExpression n, String[] argu) throws TypeCheckingException {
      n.f0.accept(this, argu);
      n.f1.accept(this, argu);
      n.f2.accept(this, argu);
      String expType = n.f3.accept(this, argu);
      n.f4.accept(this, argu);

      // type-check array elements' number
      if (!expType.equals(INT)) {
         throw new TypeCheckingException(
               "Array elements' number is not int -> Line: " + Integer.toString(n.f4.beginLine));
      }

      return INT_ARRAY;
   }

   /**
    * f0 -> "new"
    * f1 -> Identifier()
    * f2 -> "("
    * f3 -> ")"
    */
   public String visit(AllocationExpression n, String[] argu) throws TypeCheckingException {
      n.f0.accept(this, argu);
      String id = n.f1.accept(this, argu);
      n.f2.accept(this, argu);

      // if class with name id does not exist, throw exception
      symbols.checkClassNotDeclared(id, Integer.toString(n.f2.beginLine), "Class not declared");

      n.f3.accept(this, argu);

      return id;
   }

   /**
    * f0 -> "!"
    * f1 -> Clause()
    */
   public String visit(NotExpression n, String[] argu) {
      n.f0.accept(this, argu);
      return n.f1.accept(this, argu); // return clause's type
   }

   /**
    * f0 -> "("
    * f1 -> Expression()
    * f2 -> ")"
    */
   public String visit(BracketExpression n, String[] argu) {
      n.f0.accept(this, argu);
      String expRet = n.f1.accept(this, argu);
      n.f2.accept(this, argu);
      return expRet; // return expression's type
   }

}
