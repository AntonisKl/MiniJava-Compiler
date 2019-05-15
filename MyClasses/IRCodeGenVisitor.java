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
    String curMethodParams, curMethodVarDeclarations, methodDeclarations, curMethodStatements;

    Symbols symbols;

    // MY FUNCTIONS

    public IRCodeGenVisitor(Symbols symbols) {
        this.symbols = symbols;
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

    String buildVarDeclaration(String id, String type) {
        return "\t%" + id + " = alloca " + getIRType(type) + "\n\n";
    }

    String buildMethodParam(String id, String type) {
        return ", " + getIRType(type) + " %." + id;
    }

    String buildLocalVarFromParam(String id, String type) {
        String IRType = getIRType(type);
        return "\t%" + id + " = alloca " + IRType + "\n\tstore " + IRType + " %." + id + ", " + IRType + "* %" + id
                + "\n\n";
    }

    int getVTableOffset(int offset) {
        return offset + 8;
    }

    String createIRVarName(String id, String className, String methodName) {
        String foundMethodVarType = symbols.getVarType(id, className, methodName, null, false), IRVarName = null;

        if (foundMethodVarType != null) { // variable is local
            String IRType = getIRType(foundMethodVarType); //symbols.getVarType(id, className, methodName, null, true)
            // %_0 = load i32, i32* %sz
            IRVarName = "%_" + (curLocalVarIndex++);
            curMethodStatements += ("\t" + IRVarName + " = load " + IRType + ", " + IRType + "* %" + id + "\n");
            // curLocalVarIndex++;
        } else { // variable belongs to the class scope
            foundMethodVarType = symbols.getVarType(id, className, methodName, null, true);
            String IRType = getIRType(foundMethodVarType); //symbols.getVarType(id, className, methodName, null, true)
            // %_1 = getelementptr i8, i8* %this, i32 16
            // %_2 = bitcast i8* %_1 to i32*
            curMethodStatements += ("\t%_" + (curLocalVarIndex++) + " = getelementptr i8, i8* %this, " + IRType + " "
                    + getVTableOffset(symbols.getVarOffset(id, className)) + "\n");
            IRVarName = "%_" + (curLocalVarIndex++);
            curMethodStatements += ("\t" + IRVarName + " = bitcast i8* %_" + (curLocalVarIndex - 2) + " to " + IRType
                    + "\n");
        }

        return IRVarName;
    }

    String buildAssignmentStatement(String value, String id, String type) {
        // String valueToBeStored = stringIsIntOrBoolean(value) ? value : "%" + value;
        // String foundMethodVarType = getVarType(id, className, methodName, null, false), assignmentBlock = "";
        // if (foundMethodVarType != null) { // variable is local
        //     String IRType = getIRType(foundMethodVarType); //symbols.getVarType(id, className, methodName, null, true)
        //     //store i1 1, i1* %cont
        //     assignmentBlock += ("\tstore " + IRType + " " + valueToBeStored + ", " + IRType + "* %" + id + "\n");
        // } else { // variable belongs to the class scope
        //     String IRType = getIRType(type); //symbols.getVarType(id, className, methodName, null, true)
        //     // %_0 = load i32, i32* %sz
        //     // %_1 = getelementptr i8, i8* %this, i32 16
        //     // %_2 = bitcast i8* %_1 to i32*
        //     assignmentBlock += "\tstore " + IRType + " " + valueToBeStored + ", " + IRType + "* %" + id + "\n";
        // }
        String IRType = getIRType(type);

        return "\tstore " + IRType + " " + value + ", " + IRType + "* " + id + "\n";
    }

    boolean stringIsIntOrBoolean(String s) {
        try {
            Integer.parseInt(s);
        } catch (NumberFormatException e) {
            if (!s.equals("true") && !s.equals("false"))
                return false;
        }

        return true;
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
    public String visit(Goal n, String[] argu) {
        String IRCode = "declare i8* @calloc(i32, i32)\ndeclare i32 @printf(i8*, ...)\ndeclare void @exit(i32)\n\n@_cint = constant [4 x i8] c\"%d\0a\00\"\n@_cOOB = constant [15 x i8] c\"Out of bounds\0a\00\"\ndefine void @print_int(i32 %i) {\n\t%_str = bitcast [4 x i8]* @_cint to i8*\n\tcall i32 (i8*, ...) @printf(i8* %_str, i32 %i)\n\tret void\n}\n\ndefine void @throw_oob() {\n\t%_str = bitcast [15 x i8]* @_cOOB to i8*\n\tcall i32 (i8*, ...) @printf(i8* %_str)\n\tcall void @exit(i32 1)\n\tret void\n}\n\n";

        methodDeclarations = "";
        n.f0.accept(this, argu);
        n.f1.accept(this, argu);
        IRCode += methodDeclarations;

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
        methodDeclarations += "define i32 @main() {\n";

        curMethodVarDeclarations = "";
        n.f14.accept(this, new String[] { METHOD, id2, CLASS, id1 }); // pass the necessary arguments
        methodDeclarations += curMethodVarDeclarations;

        curMethodStatements = "";
        n.f15.accept(this, new String[] { METHOD, id2, CLASS, id1 });
        methodDeclarations += curMethodStatements;

        n.f16.accept(this, argu);
        n.f17.accept(this, argu);

        methodDeclarations += "}\n\n";
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
        String _ret = null;
        String type = n.f0.accept(this, argu);
        String id = n.f1.accept(this, argu);
        n.f2.accept(this, argu);

        if (argu[0].equals(CLASS)) { // we are inside a class

        } else if (argu[0].equals(METHOD)) { // we are inside a method which is inside a class
            curMethodVarDeclarations += buildVarDeclaration(id, type);
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

        methodDeclarations += "define ";
        curLocalVarIndex = 0; // reset local variable index

        n.f0.accept(this, argu);
        String type = n.f1.accept(this, argu);
        String id = n.f2.accept(this, argu);

        methodDeclarations += (symbols.getMethodType(argu[0], id) + " @" + argu[0] + "." + id + "(i8* %this");

        n.f3.accept(this, argu);

        if (symbols.inheritances.get(argu[0]) == null) { // no inherited classes

        }

        curMethodParams = "";
        curMethodVarDeclarations = "";
        n.f4.accept(this, new String[] { METHOD, id, CLASS, argu[0] });
        methodDeclarations += (curMethodParams + ") {\n");

        n.f5.accept(this, argu);
        n.f6.accept(this, argu);

        n.f7.accept(this, new String[] { METHOD, id, CLASS, argu[0] });
        methodDeclarations += curMethodVarDeclarations;

        curMethodStatements = "";
        n.f8.accept(this, new String[] { METHOD, id, CLASS, argu[0] });
        methodDeclarations += curMethodStatements;

        n.f9.accept(this, argu);
        n.f10.accept(this, new String[] { METHOD, id, CLASS, argu[0] });
        n.f11.accept(this, argu);
        n.f12.accept(this, argu);

        methodDeclarations += "}\n\n";
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
    public String visit(FormalParameter n, String[] argu) {
        String _ret = null;
        String type = n.f0.accept(this, argu);
        String id = n.f1.accept(this, argu);

        curMethodParams += buildMethodParam(id, type);
        curMethodVarDeclarations += buildLocalVarFromParam(id, type);

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
        String expResult = n.f2.accept(this, argu);
        n.f3.accept(this, argu);

        // String firstInheritedClassName = symbols.getFirstInheritedClassName(expResult);
        String IRId = createIRVarName(id, argu[3], argu[1]);

        // if (stringIsIntOrBoolean(expResult)) { // is a primitive type
        curMethodStatements += buildAssignmentStatement(expResult, IRId, idType);
        // } else { // is a class type
        // 
        // }

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
        // TODO: add check for out of bounds

        String _ret = null;
        String id = n.f0.accept(this, argu);
        n.f1.accept(this, argu);
        String expResult = n.f2.accept(this, argu);
        n.f3.accept(this, argu);

        n.f4.accept(this, argu);
        expResult = n.f5.accept(this, argu);
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
    // argu[0]: "method", argu[1]: name of method, argu[2]: "class", argu[3]: name of class
    public String visit(IfStatement n, String[] argu) throws TypeCheckingException {
        String _ret = null;
        n.f0.accept(this, argu);
        n.f1.accept(this, argu);
        String expResult = n.f2.accept(this, argu);
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
    // argu[0]: "method", argu[1]: name of method, argu[2]: "class", argu[3]: name of class
    public String visit(WhileStatement n, String[] argu) throws TypeCheckingException {
        String _ret = null;
        n.f0.accept(this, argu);
        n.f1.accept(this, argu);
        String expResult = n.f2.accept(this, argu);
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
    // argu[0]: "method", argu[1]: name of method, argu[2]: "class", argu[3]: name of class
    public String visit(PrintStatement n, String[] argu) throws TypeCheckingException {
        String _ret = null;
        n.f0.accept(this, argu);
        n.f1.accept(this, argu);
        String expResult = n.f2.accept(this, argu);
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
        return BOOLEAN;
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

        return BOOLEAN;
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

        return INT;
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

        return INT;
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

        return INT;
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

        String type2 = n.f2.accept(this, argu);
        n.f3.accept(this, argu);

        // TODO: add check for array index out of bounds

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

        // System.out.println("method name: " + methodName + "object: " + type);
        // // get method's return type
        // String methodType = symbols.getMethodType(methodName, type, null, true);

        n.f3.accept(this, argu);
        if (n.f4.present()) {
            n.f4.accept(this, new String[] { METHOD, argu[1], CLASS, argu[3], METHOD, methodName, CLASS, type });
        }

        n.f5.accept(this, argu);

        // if (!n.f4.present())
        //     return methodType;

        // return methodType;
        return null;
    }

    /**
     * f0 -> Expression()
     * f1 -> ExpressionTail()
     */

    // argu[0]: METHOD, argu[1]: context method's name, argu[2]: CLASS, argu[3]: context class's name
    // argu[4]: METHOD, argu[5]: called method's name, argu[6]: CLASS, argu[7]: called method's class name
    public String visit(ExpressionList n, String[] argu) throws TypeCheckingException {
        String _ret = null;
        String expResult = n.f0.accept(this, argu);

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
        String _ret = null;
        n.f0.accept(this, argu);
        String expResult = n.f1.accept(this, argu);

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
        case 3: // identifier
            return createIRVarName(choice, argu[3], argu[1]);
        case 4: // this
            return "%" + choice;
        // 7: choice is the result of the expression that is inside brackets
        default:
            return choice;
        }

        // return null; // will not happen
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
    // argu[0]: METHOD, argu[1]: context method's name, argu[2]: CLASS, argu[3]: context class's name
    // argu[4]: METHOD, argu[5]: called method's name, argu[6]: CLASS, argu[7]: called method's class name
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
        n.f3.accept(this, argu);
        n.f4.accept(this, argu);

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
