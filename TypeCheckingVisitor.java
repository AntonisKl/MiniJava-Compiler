import syntaxtree.*;
import visitor.GJDepthFirst;
import java.util.Map;
import java.util.HashMap;

public class TypeCheckingVisitor extends GJDepthFirst<String, String[]> {

   final static String METHOD = "method";
   final static String CLASS = "class";

   final static String BOOLEAN = "boolean";
   final static String INT = "int";
   final static String INT_ARRAY = "int[]";

   int curMethodParamIndex;

   Symbols symbols;

   public TypeCheckingVisitor(Symbols symbols) {
      this.symbols = symbols;
   }

   // private String symbols.getVarType(String id, String className, String methodName, String lineInfo)
   //       throws TypeCheckingException {
   //    Map<String, String> curClassVarTypes = symbols.classesMaps.get(className).varTypes;
   //    Map<String, String> curMethodVarTypes = symbols.classesMaps.get(className).methodVarTypes.get(methodName);
   //    // if (id == "new_node") {
   //    //    System.out.println((new PrettyPrintingMap(curMethodVarTypes)).toString());
   //    // }
   //    String methodScopeVarType = curMethodVarTypes.get(id);
   //    // System.out.println(methodScopeVarType);
   //    String classScopeVarType = curClassVarTypes.get(id);

   //    if (methodScopeVarType == null && classScopeVarType == null) {
   //       Map<String, String> inheritances = symbols.inheritances;
   //       String curClassName = className;
   //       while (inheritances.get(curClassName) != null) {
   //          curClassName = inheritances.get(curClassName);

   //          if (symbols.classesMaps.get(curClassName).varTypes.get(id) != null)
   //             return symbols.classesMaps.get(curClassName).varTypes.get(id);
   //       }

   //       throw new TypeCheckingException("Variable not declared -> Line: " + lineInfo);
   //    }
   //    // if (methodScopeIdType != null)
   //    //    return;

   //    return methodScopeVarType != null ? methodScopeVarType : classScopeVarType;

   //    // classScopeIdType != null
   // }

   // private String symbols.getMethodType(String id, String className, String lineInfo) throws TypeCheckingException {
   //    Map<String, String> curClassMethodTypes = symbols.classesMaps.get(className).methodTypes;
   //    String classScopeMethodType = curClassMethodTypes.get(id);

   //    if (classScopeMethodType == null) {
   //       Map<String, String> inheritances = symbols.inheritances;
   //       String curClassName = className;
   //       while (inheritances.get(curClassName) != null) {
   //          curClassName = inheritances.get(curClassName);

   //          if (symbols.classesMaps.get(curClassName).methodTypes.get(id) != null)
   //             return symbols.classesMaps.get(curClassName).methodTypes.get(id);
   //       }

   //       throw new TypeCheckingException("Method not declared -> Line: " + lineInfo);
   //    }

   //    return classScopeMethodType;
   // }

   private boolean atLeastOneEquals(String s1, String s2, String value) {
      return s1.equals(value) || s2.equals(value);
   }

   private boolean bothEquals(String s1, String s2, String value) {
      return s1.equals(value) && s2.equals(value);
   }

   private String getExpType(String type1, String type2, String operationType, String lineInfo)
         throws TypeCheckingException {

      String expType = INT;
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
      // if ((operationType.equals("<") || operationType.equals(">"))
      //       && (type1.equals(INT_ARRAY) || type2.equals(INT_ARRAY) || type1.equals(BOOLEAN) || type2.equals(BOOLEAN)))
      //    throw new TypeCheckingException("Operation " + operationType + "is undefined for types " + type1 + ", " + type2
      //          + " -> Line: " + lineInfo);

      return expType;
   }

   private void checkParamType(String paramType, String className, String methodName, String lineInfo)
         throws TypeCheckingException {
      // System.out.println("index: " + curMethodParamIndex + "class name: " + className + ", method name: " + methodName);
      String curOriginalParamType = symbols.classesMaps.get(className).methodParamTypes.get(methodName)
            .get(curMethodParamIndex);
      String firstInheritedClassName = symbols.getFirstInheritedClassName(paramType);

      if (firstInheritedClassName == null) {
         if (!curOriginalParamType.equals(paramType)) {
            throw new TypeCheckingException("Invalid parameter -> Line: " + lineInfo);
         }
      } else { // is object

         symbols.checkMatchParentClassTypes(paramType, curOriginalParamType, lineInfo);
         // boolean typeMatched = false;
         // String curParamType = paramType;
         // while (curParamType != null) {
         //    if (curParamType.equals(curOriginalParamType)) {
         //       typeMatched = true;
         //       break;
         //    }

         //    curParamType = symbols.inheritances.get(curParamType);
         // }
         // if (!typeMatched) {
         //    throw new TypeCheckingException("Invalid parameter -> Line: " + lineInfo);
         // }
      }

      return;
   }

   // VISIT FUNCTIONS

   /**
   * f0 -> MainClass()
   * f1 -> ( TypeDeclaration() )*
   * f2 -> <EOF>
   */
   public String visit(Goal n, String[] argu) {
      String _ret = null;
      n.f0.accept(this, argu);
      n.f1.accept(this, argu);
      n.f2.accept(this, argu);
      return _ret;
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
      // n.f14.accept(this, argu);

      String id2 = "main";
      n.f15.accept(this, new String[] { METHOD, id2, CLASS, id1 });
      n.f16.accept(this, argu);
      n.f17.accept(this, argu);
      return _ret;
   }

   /**
    * f0 -> ClassDeclaration()
    *       | ClassExtendsDeclaration()
    */
   public String visit(TypeDeclaration n, String[] argu) {
      return n.f0.accept(this, argu);
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
      String _ret = null;
      n.f0.accept(this, argu);
      String id = n.f1.accept(this, argu);
      n.f2.accept(this, argu);
      // n.f3.accept(this, argu);
      n.f4.accept(this, new String[] { id });
      n.f5.accept(this, argu);
      return _ret;
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
      String _ret = null;
      n.f0.accept(this, argu);
      String id = n.f1.accept(this, argu);
      n.f2.accept(this, argu);
      n.f3.accept(this, argu);
      n.f4.accept(this, argu);
      // n.f5.accept(this, argu);
      n.f6.accept(this, new String[] { id });
      n.f7.accept(this, argu);
      return _ret;
   }

   // /**
   //  * f0 -> Type()
   //  * f1 -> Identifier()
   //  * f2 -> ";"
   //  */
   // public String visit(VarDeclaration n, String[] argu) {
   //    Map<String, String> curVarTypes;

   //    String _ret = null;
   //    n.f0.accept(this, argu);
   //    n.f1.accept(this, argu);
   //    n.f2.accept(this, argu);
   //    return _ret;
   // }

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
   public String visit(MethodDeclaration n, String[] argu) { // argu[0]: class name
      String _ret = null;
      n.f0.accept(this, argu);
      n.f1.accept(this, argu);
      String id = n.f2.accept(this, argu);
      n.f3.accept(this, argu);

      String firstInheritedClassName = symbols.getFirstInheritedClassName(argu[0]);
      String firstInheritedClassMethodName = null;
      if (firstInheritedClassName != null) {
         firstInheritedClassMethodName = symbols.getMethodType(firstInheritedClassName, id);
      }

      if (firstInheritedClassMethodName != null) {
         try {
            if (!symbols.getMethodType(argu[0], id).equals(firstInheritedClassMethodName)) {
               throw new TypeCheckingException("Invalid method type in child class -> Line:" + n.f3.beginLine);
            }
         } catch (TypeCheckingException e) {
            e.printStackTrace();
            System.exit(1);
         }
      }

      if (firstInheritedClassMethodName != null && n.f4.present()) {
         n.f4.accept(this, new String[] { CLASS, argu[0], METHOD, id, CLASS, firstInheritedClassName });
      }
      try {
         if (firstInheritedClassMethodName != null && !n.f4.present()
               && symbols.getMethodParamsNum(firstInheritedClassName, id) > 0) {
            throw new TypeCheckingException("Invalid parameters number -> Line:" + n.f3.beginLine);
         }
      } catch (TypeCheckingException e) {
         e.printStackTrace();
         System.exit(1);
      }

      n.f5.accept(this, argu);
      n.f6.accept(this, argu);
      // n.f7.accept(this, argu);
      n.f8.accept(this, new String[] { METHOD, id, CLASS, argu[0] });
      n.f9.accept(this, argu);
      String retType = n.f10.accept(this, new String[] { METHOD, id, CLASS, argu[0] });

      n.f11.accept(this, argu);

      try {
         if (!symbols.getMethodType(argu[0], id).equals(retType)) {
            throw new TypeCheckingException("Invalid return expression type -> Line:" + n.f11.beginLine);
         }
      } catch (TypeCheckingException e) {
         e.printStackTrace();
         System.exit(1);
      }

      n.f12.accept(this, argu);
      return _ret;
   }

   /**
    * f0 -> FormalParameter()
    * f1 -> FormalParameterTail()
    */
   public String visit(FormalParameterList n, String[] argu) {

      try {
         if (symbols.getMethodParamsNum(argu[5], argu[3]) == 0) {
            throw new TypeCheckingException("Invalid parameters number -> Line: N/A");
         }
      } catch (TypeCheckingException e) {
         e.printStackTrace();
         System.exit(1);
      }

      String _ret = null;

      curMethodParamIndex = 0;

      String paramType = n.f0.accept(this, argu);
      System.out.println("type: " + paramType);

      try {
         checkParamType(paramType, argu[5], argu[3], null);
      } catch (TypeCheckingException e) {
         e.printStackTrace();
         System.exit(1);
      }

      n.f1.accept(this, argu);
      return _ret;
   }

   /**
    * f0 -> Type()
    * f1 -> Identifier()
    */

   // argu[0]: "class", argu[1]: name of class OR argu[0]: "method", argu[1]: name of method argu[2]: "class", argu[3]: name of class
   public String visit(FormalParameter n, String[] argu) {
      String type = n.f0.accept(this, argu);
      n.f1.accept(this, argu);

      // curVarTypes.put(argu[1], new HashMap<String, String>());
      //System.out.println("FORMAL PARAMETER VISIT -> " + curVarTypes.get(id));

      return type;
   }

   /**
    * f0 -> ( FormalParameterTerm() )*
    */
   public String visit(FormalParameterTail n, String[] argu) {
      return n.f0.accept(this, argu);
   }

   /**
    * f0 -> ","
    * f1 -> FormalParameter()
    */
   public String visit(FormalParameterTerm n, String[] argu) {
      curMethodParamIndex++;

      String _ret = null;
      n.f0.accept(this, argu);

      try {
         // System.out.println("method params num: " + symbols.getMethodParamsNum(argu[3], argu[1]));
         if (curMethodParamIndex >= symbols.getMethodParamsNum(argu[5], argu[3])) {
            // System.out.println("here");
            throw new TypeCheckingException("Invalid parameters number -> Line:" + n.f0.beginLine);
         }
      } catch (TypeCheckingException e) {
         e.printStackTrace();
         System.exit(1);
      }

      String expType = n.f1.accept(this, argu);

      try {
         checkParamType(expType, argu[5], argu[3], null);
      } catch (TypeCheckingException e) {
         e.printStackTrace();
         System.exit(1);
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
      //  String _ret=null;
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

   // argu[0]: "method", argu[1]: name of method argu[2]: "class", argu[3]: name of class
   public String visit(Statement n, String[] argu) {
      return n.f0.accept(this, argu);
   }

   /**
    * f0 -> "{"
    * f1 -> ( Statement() )*
    * f2 -> "}"
    */
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
   public String visit(AssignmentStatement n, String[] argu) {
      String _ret = null;

      // HashMap<String, String> curClassVarTypes = symbols.classesMaps.get(argu[3]).varTypes;
      // HashMap<String, String> curMethodVarTypes = symbols.classesMaps.get(argu[3]).methodVarTypes.get(argu[1]);

      // if (curClassVarTypes.get(key) == null)
      //    throw new TypeCHeckingException("Type checking error");

      String id = n.f0.accept(this, argu);

      String idType = null;
      try {
         idType = symbols.getVarType(id, argu[3], argu[1], Integer.toString(n.f1.beginLine), true);

      } catch (TypeCheckingException e) {
         e.printStackTrace();
         System.exit(1);
      }

      n.f1.accept(this, argu);
      String expType = n.f2.accept(this, argu);
      n.f3.accept(this, argu);

      try {
         String firstInheritedClassName = symbols.getFirstInheritedClassName(expType);

         if (firstInheritedClassName == null) {
            if (!expType.equals(idType)) {
               throw new TypeCheckingException("Left and right parts of expression have different types -> Line: "
                     + Integer.toString(n.f3.beginLine));
            }
         } else {
            symbols.checkMatchParentClassTypes(expType, idType, Integer.toString(n.f3.beginLine));
         }
      } catch (TypeCheckingException e) {
         e.printStackTrace();
         System.exit(1);
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
   public String visit(ArrayAssignmentStatement n, String[] argu) {
      String _ret = null;
      String id = n.f0.accept(this, argu);
      n.f1.accept(this, argu);

      String idType = null;
      try {
         idType = symbols.getVarType(id, argu[3], argu[1], Integer.toString(n.f1.beginLine), true);
         if (!idType.equals(INT_ARRAY)) {
            throw new TypeCheckingException("Left part of array assignment statement is not of type int array -> Line: "
                  + Integer.toString(n.f1.beginLine));
         }
      } catch (TypeCheckingException e) {
         e.printStackTrace();
         System.exit(1);
      }

      String expType = n.f2.accept(this, argu);
      n.f3.accept(this, argu);

      try {
         if (!expType.equals(INT)) {
            throw new TypeCheckingException(
                  "Identifier of array index is not of type int -> Line: " + Integer.toString(n.f3.beginLine));
         }
      } catch (TypeCheckingException e) {
         e.printStackTrace();
         System.exit(1);
      }

      n.f4.accept(this, argu);
      expType = n.f5.accept(this, argu);
      n.f6.accept(this, argu);

      try {
         if (!expType.equals(INT)) {
            throw new TypeCheckingException(
                  "Identifier of array index is not of type int -> Line: " + Integer.toString(n.f6.beginLine));
         }
      } catch (TypeCheckingException e) {
         e.printStackTrace();
         System.exit(1);
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
   public String visit(IfStatement n, String[] argu) {
      String _ret = null;
      n.f0.accept(this, argu);
      n.f1.accept(this, argu);
      String expType = n.f2.accept(this, argu);
      n.f3.accept(this, argu);

      System.out.println("line -> " + Integer.toString(n.f3.beginLine));
      try {
         if (!expType.equals(BOOLEAN)) {
            throw new TypeCheckingException(
                  "Expression of if statement is not of type boolean -> Line: " + Integer.toString(n.f3.beginLine));
         }
      } catch (TypeCheckingException e) {
         e.printStackTrace();
         System.exit(1);
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
   public String visit(WhileStatement n, String[] argu) {
      String _ret = null;
      n.f0.accept(this, argu);
      n.f1.accept(this, argu);
      String expType = n.f2.accept(this, argu);
      n.f3.accept(this, argu);

      try {
         if (!expType.equals(BOOLEAN)) {
            throw new TypeCheckingException(
                  "Expression of while statement is not of type boolean -> Line: " + Integer.toString(n.f3.beginLine));
         }
      } catch (TypeCheckingException e) {
         e.printStackTrace();
         System.exit(1);
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
   public String visit(PrintStatement n, String[] argu) {
      String _ret = null;
      n.f0.accept(this, argu);
      n.f1.accept(this, argu);
      n.f2.accept(this, argu);
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
   public String visit(AndExpression n, String[] argu) {
      String type1 = n.f0.accept(this, argu);
      n.f1.accept(this, argu);
      String type2 = n.f2.accept(this, argu);

      String expType = null;
      try {
         expType = getExpType(type1, type2, n.f1.toString(), Integer.toString(n.f1.beginLine));
      } catch (TypeCheckingException e) {
         e.printStackTrace();
         System.exit(1);
      }
      return expType;
   }

   /**
    * f0 -> PrimaryExpression()
    * f1 -> "<"
    * f2 -> PrimaryExpression()
    */
   public String visit(CompareExpression n, String[] argu) {
      String type1 = n.f0.accept(this, argu);
      n.f1.accept(this, argu);
      String type2 = n.f2.accept(this, argu);

      String expType = null;
      try {
         expType = getExpType(type1, type2, n.f1.toString(), Integer.toString(n.f1.beginLine));
      } catch (TypeCheckingException e) {
         e.printStackTrace();
         System.exit(1);
      }
      return expType;
   }

   /**
    * f0 -> PrimaryExpression()
    * f1 -> "+"
    * f2 -> PrimaryExpression()
    */
   public String visit(PlusExpression n, String[] argu) {
      // String _ret = null;
      String type1 = n.f0.accept(this, argu);
      n.f1.accept(this, argu);
      String type2 = n.f2.accept(this, argu);

      String expType = null;
      try {
         expType = getExpType(type1, type2, n.f1.toString(), Integer.toString(n.f1.beginLine));
      } catch (TypeCheckingException e) {
         e.printStackTrace();
         System.exit(1);
      }
      return expType;
   }

   /**
    * f0 -> PrimaryExpression()
    * f1 -> "-"
    * f2 -> PrimaryExpression()
    */
   public String visit(MinusExpression n, String[] argu) {
      // String _ret = null;
      String type1 = n.f0.accept(this, argu);
      n.f1.accept(this, argu);
      String type2 = n.f2.accept(this, argu);

      String expType = null;
      try {
         expType = getExpType(type1, type2, n.f1.toString(), Integer.toString(n.f1.beginLine));
      } catch (TypeCheckingException e) {
         e.printStackTrace();
         System.exit(1);
      }
      return expType;
   }

   /**
    * f0 -> PrimaryExpression()
    * f1 -> "*"
    * f2 -> PrimaryExpression()
    */
   public String visit(TimesExpression n, String[] argu) {
      // String _ret = null;
      String type1 = n.f0.accept(this, argu);
      n.f1.accept(this, argu);
      String type2 = n.f2.accept(this, argu);

      String expType = null;
      try {
         expType = getExpType(type1, type2, n.f1.toString(), Integer.toString(n.f1.beginLine));
      } catch (TypeCheckingException e) {
         e.printStackTrace();
         System.exit(1);
      }
      return expType;
   }

   /**
    * f0 -> PrimaryExpression()
    * f1 -> "["
    * f2 -> PrimaryExpression()
    * f3 -> "]"
    */
   public String visit(ArrayLookup n, String[] argu) {
      String type1 = n.f0.accept(this, argu);
      n.f1.accept(this, argu);

      try {
         if (!type1.equals(INT_ARRAY)) {
            throw new TypeCheckingException(
                  "Identifier is not an int array -> Line: " + Integer.toString(n.f1.beginLine));
         }
      } catch (TypeCheckingException e) {
         e.printStackTrace();
         System.exit(1);
      }

      String type2 = n.f2.accept(this, argu);
      n.f3.accept(this, argu);

      try {
         if (!type2.equals(INT)) {
            throw new TypeCheckingException("Identifier is not an int -> Line: " + Integer.toString(n.f3.beginLine));
         }
      } catch (TypeCheckingException e) {
         e.printStackTrace();
         System.exit(1);
      }

      return INT;
   }

   /**
    * f0 -> PrimaryExpression()
    * f1 -> "."
    * f2 -> "length"
    */
   public String visit(ArrayLength n, String[] argu) {
      String type = n.f0.accept(this, argu);
      n.f1.accept(this, argu);

      try {
         if (!type.equals(INT_ARRAY)) {
            throw new TypeCheckingException(
                  "Identifier is not an int array -> Line: " + Integer.toString(n.f1.beginLine));
         }
      } catch (TypeCheckingException e) {
         e.printStackTrace();
         System.exit(1);
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
   public String visit(MessageSend n, String[] argu) {
      String _ret = null;
      String type = n.f0.accept(this, argu);

      n.f1.accept(this, argu);

      // if (type == INT_ARRAY || type == INT || type == BOOLEAN) {
      //    throw new TypeCheckingException("Identifier is not an object -> Line: " + Integer.toString(n.f1.beginLine));
      // }

      String methodName = n.f2.accept(this, argu);

      String methodType = null;
      try {
         methodType = symbols.getMethodType(methodName, type, Integer.toString(n.f1.beginLine), true);
      } catch (TypeCheckingException e) {
         e.printStackTrace();
         System.exit(1);
      }

      n.f3.accept(this, argu);
      if (n.f4.present()) {
         n.f4.accept(this, new String[] { METHOD, argu[1], CLASS, argu[3], METHOD, methodName, CLASS, type });
      } else {
         try {
            if (symbols.getMethodParamsNum(type, methodName) > 0) {
               throw new TypeCheckingException("Invalid parameters number -> Line:" + n.f3.beginLine);
            }
         } catch (TypeCheckingException e) {
            e.printStackTrace();
            System.exit(1);
         }
      }

      n.f5.accept(this, argu);

      if (!n.f4.present())
         return methodType;

      try {
         if (curMethodParamIndex != symbols.getMethodParamsNum(type, methodName) - 1) {
            throw new TypeCheckingException("Invalid parameters number -> Line:" + Integer.toString(n.f5.beginLine));
         }
      } catch (TypeCheckingException e) {
         e.printStackTrace();
         System.exit(1);
      }

      return methodType;
   }

   /**
    * f0 -> Expression()
    * f1 -> ExpressionTail()
    */

   // argu[0]: METHOD, argu[1]: context method's name, argu[2]: CLASS, argu[3]: context class's name
   // argu[4]: METHOD, argu[5]: called method's name, argu[6]: CLASS, argu[7]: called method's class name
   public String visit(ExpressionList n, String[] argu) {

      try {
         if (symbols.getMethodParamsNum(argu[7], argu[5]) == 0) {
            throw new TypeCheckingException("Invalid parameters number -> Line: N/A");
         }
      } catch (TypeCheckingException e) {
         e.printStackTrace();
         System.exit(1);
      }

      String _ret = null;
      curMethodParamIndex = 0;

      String expType = n.f0.accept(this, argu);

      try {
         checkParamType(expType, argu[7], argu[5], null);
      } catch (TypeCheckingException e) {
         e.printStackTrace();
         System.exit(1);
      }

      n.f1.accept(this, argu);
      return _ret;
   }

   /**
    * f0 -> ( ExpressionTerm() )*
    */
   public String visit(ExpressionTail n, String[] argu) {
      return n.f0.accept(this, argu);
   }

   /**
    * f0 -> ","
    * f1 -> Expression()
    */
   public String visit(ExpressionTerm n, String[] argu) {
      curMethodParamIndex++;

      String _ret = null;
      n.f0.accept(this, argu);

      try {
         // System.out.println("method params num: " + symbols.getMethodParamsNum(argu[3], argu[1]));
         if (curMethodParamIndex >= symbols.getMethodParamsNum(argu[7], argu[5])) {
            // System.out.println("here");
            throw new TypeCheckingException("Invalid parameters number -> Line:" + n.f0.beginLine);
         }
      } catch (TypeCheckingException e) {
         e.printStackTrace();
         System.exit(1);
      }

      String expType = n.f1.accept(this, argu);

      try {
         checkParamType(expType, argu[7], argu[5], null);
      } catch (TypeCheckingException e) {
         e.printStackTrace();
         System.exit(1);
      }

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
   public String visit(PrimaryExpression n, String[] argu) {
      String choice = n.f0.accept(this, argu);
      int choiceId = n.f0.which;
      switch (choiceId) {
      case 0:
         return INT;
      case 1:
      case 2:
         return BOOLEAN;
      case 3:
         String idType = null;
         try {
            idType = symbols.getVarType(choice, argu[3], argu[1], null, true);
         } catch (TypeCheckingException e) {
            e.printStackTrace();
            System.exit(1);
         }

         return idType;
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
      // System.out.println("identifier: " + n.f0.toString());

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
   public String visit(ArrayAllocationExpression n, String[] argu) {
      n.f0.accept(this, argu);
      n.f1.accept(this, argu);
      n.f2.accept(this, argu);
      String expType = n.f3.accept(this, argu);
      n.f4.accept(this, argu);

      try {
         if (!expType.equals(INT)) {
            throw new TypeCheckingException("Array index is not int -> Line: " + Integer.toString(n.f4.beginLine));
         }
      } catch (TypeCheckingException e) {
         e.printStackTrace();
         System.exit(1);
      }

      return INT_ARRAY;
   }

   /**
    * f0 -> "new"
    * f1 -> Identifier()
    * f2 -> "("
    * f3 -> ")"
    */
   public String visit(AllocationExpression n, String[] argu) {
      n.f0.accept(this, argu);
      String id = n.f1.accept(this, argu);
      n.f2.accept(this, argu);

      // String idType = null;
      // try {
      //    idType = symbols.getVarType(id, argu[3], argu[1], Integer.toString(n.f2.beginLine));
      // } catch (TypeCheckingException e) {
      //    // do nothing, continue
      // }

      try {

         symbols.checkClassExists(id, Integer.toString(n.f2.beginLine));
         // if (idType.equals(INT) || idType.equals(INT_ARRAY) || idType.equals(BOOLEAN)) {
         //    throw new TypeCheckingException(
         //          "Cannot create object of a primitive type -> Line: " + Integer.toString(n.f2.beginLine));
         // }

      } catch (TypeCheckingException e) {
         e.printStackTrace();
         System.exit(1);
      }

      n.f3.accept(this, argu);

      return id;
   }

   /**
    * f0 -> "!"
    * f1 -> Clause()
    */
   public String visit(NotExpression n, String[] argu) {
      n.f0.accept(this, argu);
      String clauseType = n.f1.accept(this, argu);
      return clauseType;
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
      return expRet;
   }

}
