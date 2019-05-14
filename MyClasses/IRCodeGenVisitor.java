package MyClasses;

import syntaxtree.*;
import visitor.GJDepthFirst;

public class IRCodeGenVisitor extends GJDepthFirst<String, String[]> {
    // these are used as defines
    final static String METHOD = "method";
    final static String CLASS = "class";

    final static String BOOLEAN = "boolean";
    final static String INT = "int";
    final static String INT_ARRAY = "int[]";

    int curLocalVarIndex;

    Symbols symbols;

    // MY FUNCTIONS

    public IRCodeGenVisitor(Symbols symbols) {
       this.symbols = symbols;
    }

    String buildVarDeclaration(String id, String type) {
      String retValue = "%"+id+" = alloca ";

      switch (type) {
        case BOOLEAN:
          retValue+="i1";
        case INT:
          retValue+="i32";
        case INT_ARRAY:
          retValue+="i32*";
        default:
          retValue+="i8*";
      }

      return retValue;
    }

    // String buildVtablesDeclaration() {
    //   Map<String, ClassMaps> classesMaps = symbols.classesMaps;
    //
    //   Iterator<Entry<String, ClassMaps>> iter = map.entrySet().iterator();
  	// 	while (iter.hasNext()) {
  	// 		Entry<String, ClassMaps> entry = iter.next();
  	// 		classesMaps.get(entry.getKey()).
  	// 	}
    //
    // }

    // END OF MY FUNCTIONS

    /**
    * f0 -> MainClass()
    * f1 -> ( TypeDeclaration() )*
    * f2 -> <EOF>
    */
    public R visit(Goal n, String[] argu) {

        // TODO: add V-tables declaration to IRCode
        String IRCode = "declare i8* @calloc(i32, i32)\ndeclare i32 @printf(i8*, ...)\ndeclare void @exit(i32)\n\n@_cint = constant [4 x i8] c\"%d\0a\00\"\n@_cOOB = constant [15 x i8] c\"Out of bounds\0a\00\"\ndefine void @print_int(i32 %i) {\n    %_str = bitcast [4 x i8]* @_cint to i8*\n    call i32 (i8*, ...) @printf(i8* %_str, i32 %i)\n    ret void\n}\n\ndefine void @throw_oob() {\n    %_str = bitcast [15 x i8]* @_cOOB to i8*\n    call i32 (i8*, ...) @printf(i8* %_str)\n    call void @exit(i32 1)\n    ret void\n}\n";

        String mainClass = n.f0.accept(this, argu);
        String otherClasses = n.f1.accept(this, argu);
        n.f2.accept(this, argu);

        return IRCode;
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
       String id2 = n.f3.accept(this, argu);

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
       String retCode = "";
       String type = n.f0.accept(this, argu);
       String id = n.f1.accept(this, argu);
       n.f2.accept(this, argu);

       if (argu[0].equals(CLASS)) { // we are inside a class

       } else if (argu[0].equals(METHOD)) { // we are inside a method which is inside a class

         retCode += buildVarDeclaration(id, type);
       }

       return retCode;

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

       curLocalVarIndex = 0; // reset local variable index

       n.f0.accept(this, argu);
       String type = n.f1.accept(this, argu);
       String id = n.f2.accept(this, argu);
       n.f3.accept(this, argu);

       if (symbols.inheritances.get(argu[0]) == null) { // no inherited classes

       }

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
    public R visit(FormalParameterList n, String[] argu) {
        R _ret = null;
        n.f0.accept(this, argu);
        n.f1.accept(this, argu);
        return _ret;
    }

    /**
     * f0 -> Type()
     * f1 -> Identifier()
     */
    public R visit(FormalParameter n, String[] argu) {
        R _ret = null;
        n.f0.accept(this, argu);
        n.f1.accept(this, argu);
        return _ret;
    }

    /**
     * f0 -> ( FormalParameterTerm() )*
     */
    public R visit(FormalParameterTail n, String[] argu) {
        return n.f0.accept(this, argu);
    }

    /**
     * f0 -> ","
     * f1 -> FormalParameter()
     */
    public R visit(FormalParameterTerm n, String[] argu) {
        R _ret = null;
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
    public R visit(Type n, String[] argu) {
        return n.f0.accept(this, argu);
    }

    /**
     * f0 -> "int"
     * f1 -> "["
     * f2 -> "]"
     */
    public R visit(ArrayType n, String[] argu) {
        R _ret = null;
        n.f0.accept(this, argu);
        n.f1.accept(this, argu);
        n.f2.accept(this, argu);
        return _ret;
    }

    /**
     * f0 -> "boolean"
     */
    public R visit(BooleanType n, String[] argu) {
        return n.f0.accept(this, argu);
    }

    /**
     * f0 -> "int"
     */
    public R visit(IntegerType n, String[] argu) {
        return n.f0.accept(this, argu);
    }

    /**
     * f0 -> Block()
     *       | AssignmentStatement()
     *       | ArrayAssignmentStatement()
     *       | IfStatement()
     *       | WhileStatement()
     *       | PrintStatement()
     */
    public R visit(Statement n, String[] argu) {
        return n.f0.accept(this, argu);
    }

    /**
     * f0 -> "{"
     * f1 -> ( Statement() )*
     * f2 -> "}"
     */
    public R visit(Block n, String[] argu) {
        R _ret = null;
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
    public R visit(AssignmentStatement n, String[] argu) {
        R _ret = null;
        n.f0.accept(this, argu);
        n.f1.accept(this, argu);
        n.f2.accept(this, argu);
        n.f3.accept(this, argu);
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
    public R visit(ArrayAssignmentStatement n, String[] argu) {
        R _ret = null;
        n.f0.accept(this, argu);
        n.f1.accept(this, argu);
        n.f2.accept(this, argu);
        n.f3.accept(this, argu);
        n.f4.accept(this, argu);
        n.f5.accept(this, argu);
        n.f6.accept(this, argu);
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
    public R visit(IfStatement n, String[] argu) {
        R _ret = null;
        n.f0.accept(this, argu);
        n.f1.accept(this, argu);
        n.f2.accept(this, argu);
        n.f3.accept(this, argu);
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
    public R visit(WhileStatement n, String[] argu) {
        R _ret = null;
        n.f0.accept(this, argu);
        n.f1.accept(this, argu);
        n.f2.accept(this, argu);
        n.f3.accept(this, argu);
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
    public R visit(PrintStatement n, String[] argu) {
        R _ret = null;
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
    public R visit(Expression n, String[] argu) {
        return n.f0.accept(this, argu);
    }

    /**
     * f0 -> Clause()
     * f1 -> "&&"
     * f2 -> Clause()
     */
    public R visit(AndExpression n, String[] argu) {
        R _ret = null;
        n.f0.accept(this, argu);
        n.f1.accept(this, argu);
        n.f2.accept(this, argu);
        return _ret;
    }

    /**
     * f0 -> PrimaryExpression()
     * f1 -> "<"
     * f2 -> PrimaryExpression()
     */
    public R visit(CompareExpression n, String[] argu) {
        R _ret = null;
        n.f0.accept(this, argu);
        n.f1.accept(this, argu);
        n.f2.accept(this, argu);
        return _ret;
    }

    /**
     * f0 -> PrimaryExpression()
     * f1 -> "+"
     * f2 -> PrimaryExpression()
     */
    public R visit(PlusExpression n, String[] argu) {
        R _ret = null;
        n.f0.accept(this, argu);
        n.f1.accept(this, argu);
        n.f2.accept(this, argu);
        return _ret;
    }

    /**
     * f0 -> PrimaryExpression()
     * f1 -> "-"
     * f2 -> PrimaryExpression()
     */
    public R visit(MinusExpression n, String[] argu) {
        R _ret = null;
        n.f0.accept(this, argu);
        n.f1.accept(this, argu);
        n.f2.accept(this, argu);
        return _ret;
    }

    /**
     * f0 -> PrimaryExpression()
     * f1 -> "*"
     * f2 -> PrimaryExpression()
     */
    public R visit(TimesExpression n, String[] argu) {
        R _ret = null;
        n.f0.accept(this, argu);
        n.f1.accept(this, argu);
        n.f2.accept(this, argu);
        return _ret;
    }

    /**
     * f0 -> PrimaryExpression()
     * f1 -> "["
     * f2 -> PrimaryExpression()
     * f3 -> "]"
     */
    public R visit(ArrayLookup n, String[] argu) {
        R _ret = null;
        n.f0.accept(this, argu);
        n.f1.accept(this, argu);
        n.f2.accept(this, argu);
        n.f3.accept(this, argu);
        return _ret;
    }

    /**
     * f0 -> PrimaryExpression()
     * f1 -> "."
     * f2 -> "length"
     */
    public R visit(ArrayLength n, String[] argu) {
        R _ret = null;
        n.f0.accept(this, argu);
        n.f1.accept(this, argu);
        n.f2.accept(this, argu);
        return _ret;
    }

    /**
     * f0 -> PrimaryExpression()
     * f1 -> "."
     * f2 -> Identifier()
     * f3 -> "("
     * f4 -> ( ExpressionList() )?
     * f5 -> ")"
     */
    public R visit(MessageSend n, String[] argu) {
        R _ret = null;
        n.f0.accept(this, argu);
        n.f1.accept(this, argu);
        n.f2.accept(this, argu);
        n.f3.accept(this, argu);
        n.f4.accept(this, argu);
        n.f5.accept(this, argu);
        return _ret;
    }

    /**
     * f0 -> Expression()
     * f1 -> ExpressionTail()
     */
    public R visit(ExpressionList n, String[] argu) {
        R _ret = null;
        n.f0.accept(this, argu);
        n.f1.accept(this, argu);
        return _ret;
    }

    /**
     * f0 -> ( ExpressionTerm() )*
     */
    public R visit(ExpressionTail n, String[] argu) {
        return n.f0.accept(this, argu);
    }

    /**
     * f0 -> ","
     * f1 -> Expression()
     */
    public R visit(ExpressionTerm n, String[] argu) {
        R _ret = null;
        n.f0.accept(this, argu);
        n.f1.accept(this, argu);
        return _ret;
    }

    /**
     * f0 -> NotExpression()
     *       | PrimaryExpression()
     */
    public R visit(Clause n, String[] argu) {
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
    public R visit(PrimaryExpression n, String[] argu) {
        return n.f0.accept(this, argu);
    }

    /**
     * f0 -> <INTEGER_LITERAL>
     */
    public R visit(IntegerLiteral n, String[] argu) {
        return n.f0.accept(this, argu);
    }

    /**
     * f0 -> "true"
     */
    public R visit(TrueLiteral n, String[] argu) {
        return n.f0.accept(this, argu);
    }

    /**
     * f0 -> "false"
     */
    public R visit(FalseLiteral n, String[] argu) {
        return n.f0.accept(this, argu);
    }

    /**
     * f0 -> <IDENTIFIER>
     */
    public R visit(Identifier n, String[] argu) {
        return n.f0.accept(this, argu);
    }

    /**
     * f0 -> "this"
     */
    public R visit(ThisExpression n, String[] argu) {
        return n.f0.accept(this, argu);
    }

    /**
     * f0 -> "new"
     * f1 -> "int"
     * f2 -> "["
     * f3 -> Expression()
     * f4 -> "]"
     */
    public R visit(ArrayAllocationExpression n, String[] argu) {
        R _ret = null;
        n.f0.accept(this, argu);
        n.f1.accept(this, argu);
        n.f2.accept(this, argu);
        n.f3.accept(this, argu);
        n.f4.accept(this, argu);
        return _ret;
    }

    /**
     * f0 -> "new"
     * f1 -> Identifier()
     * f2 -> "("
     * f3 -> ")"
     */
    public R visit(AllocationExpression n, String[] argu) {
        R _ret = null;
        n.f0.accept(this, argu);
        n.f1.accept(this, argu);
        n.f2.accept(this, argu);
        n.f3.accept(this, argu);
        return _ret;
    }

    /**
     * f0 -> "!"
     * f1 -> Clause()
     */
    public R visit(NotExpression n, String[] argu) {
        R _ret = null;
        n.f0.accept(this, argu);
        n.f1.accept(this, argu);
        return _ret;
    }

    /**
     * f0 -> "("
     * f1 -> Expression()
     * f2 -> ")"
     */
    public R visit(BracketExpression n, String[] argu) {
        R _ret = null;
        n.f0.accept(this, argu);
        n.f1.accept(this, argu);
        n.f2.accept(this, argu);
        return _ret;
    }
}
