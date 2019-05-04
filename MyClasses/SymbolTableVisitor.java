package MyClasses;

import syntaxtree.*;
import visitor.GJDepthFirst;
import MyClasses.*;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;

// SymbolTableVisitor fills the "symbols" object with information about classes, methods, parameters and variables
// it also checks for double declaration of classes, methods and variables
public class SymbolTableVisitor extends GJDepthFirst<String, String[]> {

   final static String METHOD = "method";
   final static String CLASS = "class";

   Symbols symbols; // local symbols variable

   public SymbolTableVisitor(Symbols symbols) {
      this.symbols = symbols;
   }

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
      return null;
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
   public String visit(MainClass n, String[] argu) throws TypeCheckingException {
      String _ret = null;

      n.f0.accept(this, argu);
      String id1 = n.f1.accept(this, argu);
      n.f2.accept(this, argu);
      n.f3.accept(this, argu);
      n.f4.accept(this, argu);
      n.f7.accept(this, argu);
      n.f8.accept(this, argu);
      n.f9.accept(this, argu);
      n.f10.accept(this, argu);
      n.f11.accept(this, argu);
      n.f12.accept(this, argu);
      n.f13.accept(this, argu);

      // add entries to maps for the main method of the main class
      String id2 = "main", type = "void";
      symbols.classesMaps.put(id1, new ClassMaps());// add class entry
      symbols.classesMaps.get(id1).methodTypes.put(id2, type); // add method entry
      symbols.classesMaps.get(id1).methodVarTypes.put(id2, new HashMap<String, String>()); // add method variables' types entry

      n.f14.accept(this, new String[] { METHOD, id2, CLASS, id1 }); // pass the necessary arguments
      n.f15.accept(this, argu);
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
   public String visit(ClassDeclaration n, String[] argu) throws TypeCheckingException {
      String _ret = null;
      n.f0.accept(this, argu);
      String id = n.f1.accept(this, argu);
      n.f2.accept(this, argu);

      symbols.checkClassDeclared(id, Integer.toString(n.f2.beginLine)); // if class is already declared, throw exception

      symbols.classesMaps.put(id, new ClassMaps()); // add entry for the new class

      n.f3.accept(this, new String[] { CLASS, id });
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
   public String visit(ClassExtendsDeclaration n, String[] argu) throws TypeCheckingException {
      String _ret = null;
      n.f0.accept(this, argu);
      String id1 = n.f1.accept(this, argu);
      n.f2.accept(this, argu);

      symbols.checkClassDeclared(id1, Integer.toString(n.f2.beginLine)); // if class is already declared, throw exception

      symbols.classesMaps.put(id1, new ClassMaps()); // add entry for the new class

      String id2 = n.f3.accept(this, argu);

      symbols.checkClassNotDeclared(id2, Integer.toString(n.f2.beginLine), "Parent class not declared"); // if parent class is NOT declared, throw exception

      symbols.inheritances.put(id1, id2); // add entry for the new inheritance information

      n.f4.accept(this, argu);
      n.f5.accept(this, new String[] { CLASS, id1 });
      n.f6.accept(this, new String[] { id1 });
      n.f7.accept(this, argu);
      return _ret;
   }

   /**
    * f0 -> Type()
    * f1 -> Identifier()
    * f2 -> ";"
    */
   // argu[0]: "class", argu[1]: name of class OR argu[0]: "method", argu[1]: name of method argu[2]: "class", argu[3]: name of class
   public String visit(VarDeclaration n, String[] argu) throws TypeCheckingException {
      Map<String, String> curVarTypes;

      String _ret = null;
      String type = n.f0.accept(this, argu);
      String id = n.f1.accept(this, argu);
      n.f2.accept(this, argu);

      if (argu[0].equals(CLASS)) { // we are inside a class
         // check for double declaration
         if (symbols.getVarType(id, argu[1], null) != null) {
            throw new TypeCheckingException("Variable declared twice -> Line: " + Integer.toString(n.f2.beginLine));
         }

         curVarTypes = symbols.classesMaps.get(argu[1]).varTypes; // add entry for the new class's variable
         curVarTypes.put(id, type);
      } else if (argu[0].equals(METHOD)) { // we are inside a method which is inside a class
         // check for double declaration
         if (symbols.getVarType(id, argu[3], argu[1], null, false) != null) {
            throw new TypeCheckingException("Variable declared twice -> Line: " + Integer.toString(n.f2.beginLine));
         }

         curVarTypes = symbols.classesMaps.get(argu[3]).methodVarTypes.get(argu[1]); // add entry for the new method's variable
         curVarTypes.put(id, type);
      }

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
      String type = n.f1.accept(this, argu);
      String id = n.f2.accept(this, argu);
      n.f3.accept(this, argu);

      if (symbols.inheritances.get(argu[0]) == null) { // no inherited classes
         // check for double declaration
         if (symbols.getMethodType(id, argu[0], null, false) != null) {
            throw new TypeCheckingException("Method declared twice -> Line: " + Integer.toString(n.f3.beginLine));
         }
      }

      // add necessary entries to maps to prepare for the parameters and variables
      symbols.classesMaps.get(argu[0]).methodTypes.put(id, type);
      symbols.classesMaps.get(argu[0]).methodVarTypes.put(id, new HashMap<String, String>());
      symbols.classesMaps.get(argu[0]).methodParamTypes.put(id, new ArrayList<String>());

      n.f4.accept(this, new String[] { METHOD, id, CLASS, argu[0] });
      n.f5.accept(this, argu);
      n.f6.accept(this, argu);
      n.f7.accept(this, new String[] { METHOD, id, CLASS, argu[0] });
      n.f8.accept(this, argu);
      n.f9.accept(this, argu);
      n.f10.accept(this, argu);
      n.f11.accept(this, argu);
      n.f12.accept(this, argu);
      return _ret;
   }

   /**
    * f0 -> FormalParameter()
    * f1 -> FormalParameterTail()
    */
   public String visit(FormalParameterList n, String[] argu) {
      String _ret = null;
      n.f0.accept(this, argu);
      n.f1.accept(this, argu);
      return _ret;
   }

   /**
    * f0 -> Type()
    * f1 -> Identifier()
    */

   // argu[0]: "method", argu[1]: name of method, argu[2]: "class", argu[3]: name of class
   public String visit(FormalParameter n, String[] argu) {
      String _ret = null;
      String type = n.f0.accept(this, argu);
      String id = n.f1.accept(this, argu);

      // add this parameter as a method's variable
      Map<String, String> curVarTypes = symbols.classesMaps.get(argu[3]).methodVarTypes.get(argu[1]);
      curVarTypes.put(id, type);

      // add this parameter's type to the current method's list
      List<String> curParamTypes = symbols.classesMaps.get(argu[3]).methodParamTypes.get(argu[1]);
      curParamTypes.add(type);

      return _ret;
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
      String _ret = null;
      n.f0.accept(this, argu);
      n.f1.accept(this, argu);
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
      return n.f0.accept(this, argu);
   }

   /**
    * f0 -> <IDENTIFIER>
    */
   public String visit(Identifier n, String[] argu) {
      return n.f0.toString();
   }
}
