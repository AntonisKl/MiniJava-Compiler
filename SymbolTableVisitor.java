import syntaxtree.*;
import visitor.GJDepthFirst;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;

public class SymbolTableVisitor extends GJDepthFirst<String, String[]> {

   final static String METHOD = "method";
   final static String CLASS = "class";

   Symbols symbols;

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

      // try {
         // symbols.checkClassDeclared(id1, Integer.toString(n.f2.beginLine));
      // } catch (TypeCheckingException e) {
      //    e.printStackTrace();
      //    System.exit(1);
      // }

      n.f3.accept(this, argu);
      n.f4.accept(this, argu);
      n.f7.accept(this, argu);
      n.f8.accept(this, argu);
      n.f9.accept(this, argu);
      n.f10.accept(this, argu);
      n.f11.accept(this, argu);
      n.f12.accept(this, argu);
      n.f13.accept(this, argu);

      String id2 = "main", type = "void";
      symbols.classesMaps.put(id1, new ClassMaps());
      symbols.classesMaps.get(id1).methodTypes.put(id2, type);
      symbols.classesMaps.get(id1).methodVarTypes.put(id2, new HashMap<String, String>());

      n.f14.accept(this, new String[] { METHOD, id2, CLASS, id1 });
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
   public String visit(ClassDeclaration n, String[] argu) throws TypeCheckingException{
      String _ret = null;
      n.f0.accept(this, argu);
      String id = n.f1.accept(this, argu);
      n.f2.accept(this, argu);

      // try {
         symbols.checkClassDeclared(id, Integer.toString(n.f2.beginLine));
      // } catch (TypeCheckingException e) {
      //    e.printStackTrace();
      //    System.exit(1);
      // }

      symbols.classesMaps.put(id, new ClassMaps());

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
   public String visit(ClassExtendsDeclaration n, String[] argu)throws TypeCheckingException {
      String _ret = null;
      n.f0.accept(this, argu);
      String id1 = n.f1.accept(this, argu);
      n.f2.accept(this, argu);

      // try {
         symbols.checkClassDeclared(id1, Integer.toString(n.f2.beginLine));
      // } catch (TypeCheckingException e) {
      //    e.printStackTrace();
      //    System.exit(1);
      // }

      

      symbols.classesMaps.put(id1, new ClassMaps());

      String id2 = n.f3.accept(this, argu);

      // try {
         symbols.checkClassNotDeclared(id2, Integer.toString(n.f2.beginLine));
      // } catch (TypeCheckingException e) {
      //    e.printStackTrace();
      //    System.exit(1);
      // }

      symbols.inheritances.put(id1, id2);

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

      if (argu[0].equals(CLASS)) {
         // try {
            if (symbols.getVarType(id, argu[1], null) != null) {
               throw new TypeCheckingException("Variable declared twice -> Line: " + Integer.toString(n.f2.beginLine));
            }
         // } catch (TypeCheckingException e) {
         //    e.printStackTrace();
         //    System.exit(1);
         // }

         curVarTypes = symbols.classesMaps.get(argu[1]).varTypes;
         curVarTypes.put(id, type);
         // System.out.println("he");
      } else if (argu[0].equals(METHOD)) {
         // try {
            if (symbols.getVarType(id, argu[3], argu[1], null, false) != null) {
               throw new TypeCheckingException("Variable declared twice -> Line: " + Integer.toString(n.f2.beginLine));
            }
         // } catch (TypeCheckingException e) {
         //    e.printStackTrace();
         //    System.exit(1);
         // }

         curVarTypes = symbols.classesMaps.get(argu[3]).methodVarTypes.get(argu[1]);
         // curVarTypes.put(argu[1], new HashMap<String, String>());
         // curVarTypes.get(argu[1]).put(n.f1.toString(), n.f0.toString());
         curVarTypes.put(id, type);
         // System.out.println("ho -> " + curVarTypes.get(id));
      }

      //System.out.println("declaration: " + n.f1 + "," + n.f0);

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
   public String visit(MethodDeclaration n, String[] argu) throws TypeCheckingException{ // argu[0]: class name
      String _ret = null;
      n.f0.accept(this, argu);
      String type = n.f1.accept(this, argu);
      String id = n.f2.accept(this, argu);
      n.f3.accept(this, argu);

      if (symbols.inheritances.get(argu[0]) == null) { // no inherited classes
         // try {
            if (symbols.getMethodType(id, argu[0], null, false) != null) {
               throw new TypeCheckingException("Method declared twice -> Line: " + Integer.toString(n.f3.beginLine));
            }
         // } catch (TypeCheckingException e) {
         //    e.printStackTrace();
         //    System.exit(1);
         // }
      }

      // System.out.println("hi: " + argu[0]);
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

      // try {
      //    if (symbols.getVarType(id, argu[3], argu[1], null, false) != null) {
      //       throw new TypeCheckingException("Variable declared twice -> Line: N/A");
      //    }
      // } catch (TypeCheckingException e) {
      //    e.printStackTrace();
      //    System.exit(1);
      // }

      Map<String, String> curVarTypes = symbols.classesMaps.get(argu[3]).methodVarTypes.get(argu[1]);
      curVarTypes.put(id, type);

      List<String> curParamTypes = symbols.classesMaps.get(argu[3]).methodParamTypes.get(argu[1]);
      curParamTypes.add(type);

      // List<String> curParamTypes = symbols.classesMaps.get(argu[3]).methodParamTypes;
      // curParamTypes.add(type);

      //System.out.println("FORMAL PARAMETER VISIT -> " + curVarTypes.get(id));

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

   // /**
   //  * f0 -> Block()
   //  *       | AssignmentStatement()
   //  *       | ArrayAssignmentStatement()
   //  *       | IfStatement()
   //  *       | WhileStatement()
   //  *       | PrintStatement()
   //  */
   // public String visit(Statement n, String[] argu) {
   //    return n.f0.accept(this, argu);
   // }

   // /**
   //  * f0 -> "{"
   //  * f1 -> ( Statement() )*
   //  * f2 -> "}"
   //  */
   // public String visit(Block n, String[] argu) {
   //    String _ret = null;
   //    n.f0.accept(this, argu);
   //    n.f1.accept(this, argu);
   //    n.f2.accept(this, argu);
   //    return _ret;
   // }

   // /**
   //  * f0 -> Identifier()
   //  * f1 -> "="
   //  * f2 -> Expression()
   //  * f3 -> ";"
   //  */
   // public String visit(AssignmentStatement n, String[] argu) {
   //    String _ret = null;
   //    n.f0.accept(this, argu);
   //    n.f1.accept(this, argu);
   //    n.f2.accept(this, argu);
   //    n.f3.accept(this, argu);
   //    return _ret;
   // }

   // /**
   //  * f0 -> Identifier()
   //  * f1 -> "["
   //  * f2 -> Expression()
   //  * f3 -> "]"
   //  * f4 -> "="
   //  * f5 -> Expression()
   //  * f6 -> ";"
   //  */
   // public String visit(ArrayAssignmentStatement n, String[] argu) {
   //    String _ret = null;
   //    n.f0.accept(this, argu);
   //    n.f1.accept(this, argu);
   //    n.f2.accept(this, argu);
   //    n.f3.accept(this, argu);
   //    n.f4.accept(this, argu);
   //    n.f5.accept(this, argu);
   //    n.f6.accept(this, argu);
   //    return _ret;
   // }

   // /**
   //  * f0 -> "if"
   //  * f1 -> "("
   //  * f2 -> Expression()
   //  * f3 -> ")"
   //  * f4 -> Statement()
   //  * f5 -> "else"
   //  * f6 -> Statement()
   //  */
   // public String visit(IfStatement n, String[] argu) {
   //    String _ret = null;
   //    n.f0.accept(this, argu);
   //    n.f1.accept(this, argu);
   //    n.f2.accept(this, argu);
   //    n.f3.accept(this, argu);
   //    n.f4.accept(this, argu);
   //    n.f5.accept(this, argu);
   //    n.f6.accept(this, argu);
   //    return _ret;
   // }

   // /**
   //  * f0 -> "while"
   //  * f1 -> "("
   //  * f2 -> Expression()
   //  * f3 -> ")"
   //  * f4 -> Statement()
   //  */
   // public String visit(WhileStatement n, String[] argu) {
   //    String _ret = null;
   //    n.f0.accept(this, argu);
   //    n.f1.accept(this, argu);
   //    n.f2.accept(this, argu);
   //    n.f3.accept(this, argu);
   //    n.f4.accept(this, argu);
   //    return _ret;
   // }

   // /**
   //  * f0 -> "System.out.println"
   //  * f1 -> "("
   //  * f2 -> Expression()
   //  * f3 -> ")"
   //  * f4 -> ";"
   //  */
   // public String visit(PrintStatement n, String[] argu) {
   //    String _ret = null;
   //    n.f0.accept(this, argu);
   //    n.f1.accept(this, argu);
   //    n.f2.accept(this, argu);
   //    n.f3.accept(this, argu);
   //    n.f4.accept(this, argu);
   //    return _ret;
   // }

   // /**
   //  * f0 -> AndExpression()
   //  *       | CompareExpression()
   //  *       | PlusExpression()
   //  *       | MinusExpression()
   //  *       | TimesExpression()
   //  *       | ArrayLookup()
   //  *       | ArrayLength()
   //  *       | MessageSend()
   //  *       | Clause()
   //  */
   // public String visit(Expression n, String[] argu) {
   //    return n.f0.accept(this, argu);
   // }

   // /**
   //  * f0 -> Clause()
   //  * f1 -> "&&"
   //  * f2 -> Clause()
   //  */
   // public String visit(AndExpression n, String[] argu) {
   //    String _ret = null;
   //    n.f0.accept(this, argu);
   //    n.f1.accept(this, argu);
   //    n.f2.accept(this, argu);
   //    return _ret;
   // }

   // /**
   //  * f0 -> PrimaryExpression()
   //  * f1 -> "<"
   //  * f2 -> PrimaryExpression()
   //  */
   // public String visit(CompareExpression n, String[] argu) {
   //    String _ret = null;
   //    n.f0.accept(this, argu);
   //    n.f1.accept(this, argu);
   //    n.f2.accept(this, argu);
   //    return _ret;
   // }

   // /**
   //  * f0 -> PrimaryExpression()
   //  * f1 -> "+"
   //  * f2 -> PrimaryExpression()
   //  */
   // public String visit(PlusExpression n, String[] argu) {
   //    String _ret = null;
   //    n.f0.accept(this, argu);
   //    n.f1.accept(this, argu);
   //    n.f2.accept(this, argu);
   //    return _ret;
   // }

   // /**
   //  * f0 -> PrimaryExpression()
   //  * f1 -> "-"
   //  * f2 -> PrimaryExpression()
   //  */
   // public String visit(MinusExpression n, String[] argu) {
   //    String _ret = null;
   //    n.f0.accept(this, argu);
   //    n.f1.accept(this, argu);
   //    n.f2.accept(this, argu);
   //    return _ret;
   // }

   // /**
   //  * f0 -> PrimaryExpression()
   //  * f1 -> "*"
   //  * f2 -> PrimaryExpression()
   //  */
   // public String visit(TimesExpression n, String[] argu) {
   //    String _ret = null;
   //    n.f0.accept(this, argu);
   //    n.f1.accept(this, argu);
   //    n.f2.accept(this, argu);
   //    return _ret;
   // }

   // /**
   //  * f0 -> PrimaryExpression()
   //  * f1 -> "["
   //  * f2 -> PrimaryExpression()
   //  * f3 -> "]"
   //  */
   // public String visit(ArrayLookup n, String[] argu) {
   //    String _ret = null;
   //    n.f0.accept(this, argu);
   //    n.f1.accept(this, argu);
   //    n.f2.accept(this, argu);
   //    n.f3.accept(this, argu);
   //    return _ret;
   // }

   // /**
   //  * f0 -> PrimaryExpression()
   //  * f1 -> "."
   //  * f2 -> "length"
   //  */
   // public String visit(ArrayLength n, String[] argu) {
   //    String _ret = null;
   //    n.f0.accept(this, argu);
   //    n.f1.accept(this, argu);
   //    n.f2.accept(this, argu);
   //    return _ret;
   // }

   // /**
   //  * f0 -> PrimaryExpression()
   //  * f1 -> "."
   //  * f2 -> Identifier()
   //  * f3 -> "("
   //  * f4 -> ( ExpressionList() )?
   //  * f5 -> ")"
   //  */
   // public String visit(MessageSend n, String[] argu) {
   //    String _ret = null;
   //    n.f0.accept(this, argu);
   //    n.f1.accept(this, argu);
   //    n.f2.accept(this, argu);
   //    n.f3.accept(this, argu);
   //    n.f4.accept(this, argu);
   //    n.f5.accept(this, argu);
   //    return _ret;
   // }

   // /**
   //  * f0 -> Expression()
   //  * f1 -> ExpressionTail()
   //  */
   // public String visit(ExpressionList n, String[] argu) {
   //    String _ret = null;
   //    n.f0.accept(this, argu);
   //    n.f1.accept(this, argu);
   //    return _ret;
   // }

   // /**
   //  * f0 -> ( ExpressionTerm() )*
   //  */
   // public String visit(ExpressionTail n, String[] argu) {
   //    return n.f0.accept(this, argu);
   // }

   // /**
   //  * f0 -> ","
   //  * f1 -> Expression()
   //  */
   // public String visit(ExpressionTerm n, String[] argu) {
   //    String _ret = null;
   //    n.f0.accept(this, argu);
   //    n.f1.accept(this, argu);
   //    return _ret;
   // }

   // /**
   //  * f0 -> NotExpression()
   //  *       | PrimaryExpression()
   //  */
   // public String visit(Clause n, String[] argu) {
   //    return n.f0.accept(this, argu);
   // }

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
      // System.out.println("---------------------------------->" + n.f0.which);
      return n.f0.accept(this, argu);
   }

   // /**
   //  * f0 -> <INTEGER_LITERAL>
   //  */
   // public String visit(IntegerLiteral n, String[] argu) {
   //    return n.f0.accept(this, argu);
   // }

   // /**
   //  * f0 -> "true"
   //  */
   // public String visit(TrueLiteral n, String[] argu) {
   //    return n.f0.toString();
   // }

   // /**
   //  * f0 -> "false"
   //  */
   // public String visit(FalseLiteral n, String[] argu) {
   //    return n.f0.toString();
   // }

   /**
    * f0 -> <IDENTIFIER>
    */
   public String visit(Identifier n, String[] argu) {
      // System.out.println("identifier: " + n.f0.toString());
      return n.f0.toString();
   }

   // /**
   //  * f0 -> "this"
   //  */
   // public String visit(ThisExpression n, String[] argu) {
   //    return n.f0.toString();
   // }

   // /**
   //  * f0 -> "new"
   //  * f1 -> "int"
   //  * f2 -> "["
   //  * f3 -> Expression()
   //  * f4 -> "]"
   //  */
   // public String visit(ArrayAllocationExpression n, String[] argu) {
   //    String _ret = null;
   //    n.f0.accept(this, argu);
   //    n.f1.accept(this, argu);
   //    n.f2.accept(this, argu);
   //    n.f3.accept(this, argu);
   //    n.f4.accept(this, argu);
   //    return _ret;
   // }

   // /**
   //  * f0 -> "new"
   //  * f1 -> Identifier()
   //  * f2 -> "("
   //  * f3 -> ")"
   //  */
   // public String visit(AllocationExpression n, String[] argu) {
   //    String _ret = null;
   //    n.f0.accept(this, argu);
   //    n.f1.accept(this, argu);
   //    n.f2.accept(this, argu);
   //    n.f3.accept(this, argu);
   //    return _ret;
   // }

   // /**
   //  * f0 -> "!"
   //  * f1 -> Clause()
   //  */
   // public String visit(NotExpression n, String[] argu) {
   //    String _ret = null;
   //    n.f0.accept(this, argu);
   //    n.f1.accept(this, argu);
   //    return _ret;
   // }

   // /**
   //  * f0 -> "("
   //  * f1 -> Expression()
   //  * f2 -> ")"
   //  */
   // public String visit(BracketExpression n, String[] argu) {
   //    String _ret = null;
   //    n.f0.accept(this, argu);
   //    n.f1.accept(this, argu);
   //    n.f2.accept(this, argu);
   //    return _ret;
   // }

   //     @OverrideO

   //    /**
   //     * f0 -> Exp()
   //     * f1 -> <EOF>
   //     */
   //     public String visit(Goal n, String[] argu){
   //         return n.f0.accept(this, null);
   //     }

   //    /**
   //     * f0 -> Term()
   //     * f1 -> [ Exp2() ]
   //     */
   //     public String visit(Exp n, String[] argu){
   //     	String term = n.f0.accept(this, null);
   //     	if(n.f1.present())
   //     	    return n.f1.accept(this, term);
   //     	else
   //     	    return term;
   //     }

   //    /**
   //     * f0 -> "+"
   //     * f1 -> Term()
   //     * f2 -> [ Exp2() ]
   //     */
   //     public String visit(PlusExp n, String lTerm){
   //     	String rTerm = n.f1.accept(this, null);
   //     	String rv = lTerm + rTerm;
   //     	if(n.f2.present()){
   //     	    rv = n.f2.accept(this, rv);
   //     	}
   //     	return rv;
   //     }

   //    /**
   //     * f0 -> "-"
   //     * f1 -> Term()
   //     * f2 -> [ Exp2() ]
   //     */
   //     public String visit(MinusExp n, String lTerm){
   //     	String rTerm = n.f1.accept(this, null);
   //     	String rv = lTerm - rTerm;
   //     	if(n.f2.present()){
   //     	    rv = n.f2.accept(this, rv);
   //     	}
   //     	return rv;
   //     }

   //    /**
   //     * f0 -> Factor()
   //     * f1 -> [ Term2() ]
   //     */
   //     public String visit(Term n, String[] argu){
   //     	String factor = n.f0.accept(this, null);
   //     	if(n.f1.present())
   //     	    return n.f1.accept(this, factor);
   //     	else
   //     	    return factor;
   //     }

   //    /**
   //     * f0 -> "*"
   //     * f1 -> Factor()
   //     * f2 -> [ Term2() ]
   //     */
   //     public String visit(TimesExp n, String lFactor){
   //     	String rFactor = n.f1.accept(this, null);
   //     	String rv = lFactor * rFactor;
   //     	if(n.f2.present())
   //     	    rv = n.f2.accept(this, rv);
   //     	return rv;
   //     }

   //     /**
   //     * f0 -> "/"
   //     * f1 -> Factor()
   //     * f2 -> [ Term2() ]
   //     */
   //     public String visit(DivExp n, String lFactor){
   //     	String rFactor = n.f1.accept(this, null);
   //     	String rv = lFactor / rFactor;
   //     	if(n.f2.present())
   //     	    rv = n.f2.accept(this, rv);
   //     	return rv;
   //     }

   //    /**
   //     * f0 -> <NUMBER>
   //     */
   //     public String visit(Num n, String[] argu){
   // 	   return String.parseInt(n.f0.toString());

   //     }

   //    /**
   //     * f0 -> "("
   //     * f1 -> Exp()
   //     * f2 -> ")"
   //     */
   //     public String visit(ParExp n, String[] argu) {
   //         return n.f1.accept(this, null);
   //    }

}
