package MyClasses;

import syntaxtree.*;
import visitor.GJDepthFirst;
import java.util.Map;
import java.util.HashMap;

public class IRCodeGenVisitor extends GJDepthFirst<String, String[]> {
    // these are used as defined strings
    final static String METHOD = "method";
    final static String CLASS = "class";

    final static String BOOLEAN = "boolean";
    final static String INT = "int";
    final static String INT_ARRAY = "int[]";

    // all the indexes below are incremented to guarantee the different names of local variables, labels, etc.
    // curTabsNum: number of tabs that should be put in front of the IR statement (for printing purposes only)
    // nextMethodCallId: next unique id of MessageSend/method call expression, used as a key in methodCallArgs and methodCallArgsIndexes
    int curLocalVarIndex, curArrayAllocLabelIndex = 0, curAndClauseLabelIndex = 0, curOobLabelindex = 0,
            curLoopLabelIndex = 0, curIfLabelIndex = 0, curTabsNum, nextMethodCallId = 0;
    // curMethodParams: a method's IR parameters separated by a comma
    // curMethodVarDeclarations: a method's IR local variable declarations
    // methodDeclarations: a class's IR method declarations
    // curMethodStatements: a method's IR statements
    // lastObjectType: type of the last object that was previously visited
    String curMethodParams, curMethodVarDeclarations, methodDeclarations, curMethodStatements, lastObjectType;

    // methodCallArgs: Map<unique method call's id, method call's IR arguments string>
    // methodCallArgsIndexes: Map<unique method call's id, index of next method call's argument>
    Map<Integer, String> methodCallArgs;
    Map<Integer, Integer> methodCallArgsIndexes;

    // storeExpType: true  -> visit() of PrimaryExpression should store the object's MiniJava type in lastObjectType variable
    //               false ->visit() of PrimaryExpression should not store the object's MiniJava type in lastObjectType variable
    // retExpMode: true  -> we are currently visiting a return expression or a method call's arguments
    //             false -> we are not currently visiting a return expression or a method call's arguments
    // inMessageSend: true  -> we are currently visiting an expression from the MessageSend's visit()
    //                false -> we are not currently visiting an expression from the MessageSend's visit()
    // inArrayAssignment: true  -> we are currently visiting an expression from the ArrayAssignmentStatement's visit()
    //                    false -> we are not currently visiting an expression from the ArrayAssignmentStatement's visit()
    // inIntExp: true  -> true  -> we are currently visiting an expression from a visit() in which we want to use the expression's result as an int
    //                    false -> we are not currently visiting an expression from a visit() in which we want to use the expression's result as an int
    // inArrayLookUpExp: true  -> true  -> we are currently visiting an expression from ArrayLookup's visit() in which we want to use the expression's result as an int array
    //                   false -> we are not currently visiting an expression from ArrayLookup's visit() in which we want to use the expression's result as an int array
    boolean storeExpType, retExpMode, inMessageSend, inArrayAssignment, inIntExp, inArrayLookUpExp;

    Symbols symbols;

    // MY FUNCTIONS

    public IRCodeGenVisitor(Symbols symbols) {
        this.symbols = symbols;
    }

    // returns the equivalent IR type for the MiniJava type given as argument
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

    // returns IR variable declaration
    String buildVarDeclaration(String id, String type) {
        return "\t%" + id + " = alloca " + getIRType(type) + "\n\n";
    }

    // returns IR method parameter formatted string
    String buildMethodParam(String id, String type) {
        return ", " + getIRType(type) + " %." + id;
    }

    // returns IR method call's argument formatted string
    String buildMethodCallArg(String varName, String type) {
        return ", " + getIRType(type) + " " + varName;
    }

    // returns IR local variable space allocation statements
    String buildLocalVarFromParam(String id, String type) {
        String IRType = getIRType(type);
        return buildStatement("%" + id + " = alloca " + IRType)
                + buildStatement("store " + IRType + " %." + id + ", " + IRType + "* %" + id + "\n");
    }

    // returns IR load statement
    String buildLoad(String resultVar, String varToBeLoaded, String IRType) {
        return buildStatement(resultVar + " = load " + IRType + ", " + IRType + "* " + varToBeLoaded);
    }

    // returns IR bitcast statement
    String buildBitcast(String resultVar, String varToBeBitcasted, String fromType, String toType) {
        return buildStatement(resultVar + " = bitcast " + fromType + " " + varToBeBitcasted + " to " + toType);
    }

    // returns IR getelementptr statement
    String buildGetElementPtr(String resultVar, String arrayVar, String offsetVar, String type) {
        return buildStatement(
                resultVar + " = getelementptr " + type + ", " + type + "* " + arrayVar + ", i32 " + offsetVar);
    }

    // returns IR getelementptr statement
    String buildIcmp(String resultVar, String varName1, String varName2, String operation, String type) {
        return buildStatement(resultVar + " = icmp " + operation + " " + type + " " + varName1 + ", " + varName2);
    }

    // returns IR branch statement
    String buildBranch(String condVar, String label1, String label2) {
        return buildStatement("br i1 " + condVar + ", label %" + label1 + ", label %" + label2);
    }

    // returns IR simple branch statement
    String buildBranch(String label) {
        // br label %oob17
        return buildStatement("br label %" + label);
    }

    // returns IR label declaration
    String buildLabel(String label) {
        return label + ":\n";
    }

    // returns IR add statement
    String buildAdd(String resultVar, String var1, String var2) {
        return buildStatement(resultVar + " = add i32 " + var1 + ", " + var2);
    }

    // returns IR sub statement
    String buildSub(String resultVar, String var1, String var2) {
        return buildStatement(resultVar + " = sub i32 " + var1 + ", " + var2);
    }

    // returns IR mul statement
    String buildMul(String resultVar, String var1, String var2) {
        return buildStatement(resultVar + " = mul i32 " + var1 + ", " + var2);
    }

    // returns IR phi statement
    String buildPhi(String resultVar, String result1, String labelName1, String result2, String labelName2) {
        return buildStatement(resultVar + " = phi i1 [ " + result1 + ", %" + labelName1 + " ], [ " + result2 + ", %"
                + labelName2 + "]");
    }

    // returns IR throw_oob() call
    String buildThrowOob() {
        return buildStatement("call void @throw_oob()");
    }

    // returns IR calloc() call
    String buildCalloc(String resultVar, String arg1, String arg2) {
        return buildStatement(resultVar + " = call i8* @calloc(i32 " + arg1 + ", i32 " + arg2 + ")");
    }

    // returns IR store statement
    String buildStore(String fromVar, String toVar, String type) {
        return buildStatement("store " + type + " " + fromVar + ", " + type + "* " + toVar);
    }

    // returns IR method call statement with or without assignment
    String buildCall(String resultVar, String methodNameVar, String retType, String methodArgs) {
        if (resultVar != null)
            return buildStatement(resultVar + " = call " + retType + " " + methodNameVar + "(" + methodArgs + ")");
        else
            return buildStatement("call " + retType + " " + methodNameVar + "(" + methodArgs + ")");
    }

    // returns properly formatted IR statement
    String buildStatement(String statement) {
        String retValue = "";
        for (int i = 0; i < curTabsNum; i++)
            retValue += "\t";
        retValue += (statement + "\n");
        return retValue;
    }

    // returns real IR object's array offset because first element of IR object is a pointer to its v-table 
    int getObjectArrayOffset(int offset) {
        return offset + 8;
    }

    // returns next IR local variable's unique name
    String getNextLocalVarName() {
        return "%_" + (curLocalVarIndex++);
    }

    // returns next IR array allocation label's unique name
    String getNextArrayAllocLabelName() {
        return "arr_alloc" + (curArrayAllocLabelIndex++);
    }

    // returns next IR "and clause" label's unique name
    String getNextAndClauseLabelName() {
        return "and_clause" + (curAndClauseLabelIndex++);
    }

    // returns next IR oob label's unique name
    String getNextOobLabeName() {
        return "oob" + (curOobLabelindex++);
    }

    // returns next IR loop label's unique name
    String getNextLoopLabelName() {
        return "loop" + (curLoopLabelIndex++);
    }

    // returns next IR if label's unique name
    String getNextIfLabelName() {
        return "if" + (curIfLabelIndex++);
    }

    // returns the proper IR variable's name by first adding the proper statements to the generated IR code
    // isLeftValue: true  -> current identifier(id) is a left value of an assignment statement
    //              false -> current identifier(id) is not a left value of an assignment statement
    // className: context class's name
    // methodName: context method's name
    String createIRVarName(String id, String className, String methodName, boolean isLeftValue) {
        String foundMethodVarType = symbols.getVarType(id, className, methodName, null, false), IRVarName = null;

        if (foundMethodVarType != null) { // variable is local to the current method
            if (isLeftValue || inArrayLookUpExp || inArrayAssignment) { // is left value of assignment or is int array of array lookup expression or is part of an array assignment statement
                IRVarName = "%" + id; // return the id formatted as IR variable
            } else { // not a left value of assignment
                String IRType = getIRType(foundMethodVarType);
                // template
                // %_0 = load i32, i32* %sz
                IRVarName = getNextLocalVarName();
                curMethodStatements += buildLoad(IRVarName, "%" + id, IRType); // should load the variable
            }
        } else { // variable belongs to the class scope
            // load variable from "this" object's array
            foundMethodVarType = symbols.getVarType(id, className, methodName, null, true);
            String IRType = getIRType(foundMethodVarType);
            // template
            // %_1 = getelementptr i8, i8* %this, i32 16
            // %_2 = bitcast i8* %_1 to i32*

            curMethodStatements += buildGetElementPtr(getNextLocalVarName(), "%this",
                    getObjectArrayOffset(symbols.getVarOffset(id, className)) + "", "i8");

            IRVarName = getNextLocalVarName();
            curMethodStatements += buildBitcast(IRVarName, "%_" + (curLocalVarIndex - 2), "i8*", IRType + "*");

            if (((retExpMode && !foundMethodVarType.equals(INT) && !foundMethodVarType.equals(BOOLEAN))
                    || inMessageSend) || (inIntExp || (!isLeftValue && (!foundMethodVarType.equals(INT_ARRAY))))) { // cases in which we need to load the variable
                // template
                // %_2 = load i32, i32* %_1
                // should load the variable
                String prevVarName = IRVarName;
                IRVarName = getNextLocalVarName();
                curMethodStatements += buildLoad(IRVarName, prevVarName, IRType);
            }

        }

        return IRVarName; // return the proper generated IR variable's name
    }

    // creates a new object of class className and returns the proper IR variable name in which the object's array is stored
    String createNewObject(String className) {
        // template
        // %_0 = call i8* @calloc(i32 1, i32 38) ; 30 bytes(fields) + 8 bytes(v-table pointer)
        // %_1 = bitcast i8* %_0 to i8***
        // %_2 = getelementptr [20 x i8*], [20 x i8*]* @.Tree_vtable, i32 0, i32 0
        // store i8** %_2, i8*** %_1

        String var1 = getNextLocalVarName(), var2 = getNextLocalVarName(), var3 = getNextLocalVarName();
        int vTableSize = symbols.classesVTableSizes.get(className);

        curMethodStatements += (var1 + " = call i8* @calloc(i32 1, i32 "
                + (symbols.getSizeOfFieldsInBytes(className) + 8) + ")\n");
        curMethodStatements += (var2 + " = bitcast i8* " + var1 + " to i8***\n");
        curMethodStatements += (var3 + " = getelementptr [" + vTableSize + " x i8*], [" + vTableSize + " x i8*]* @."
                + className + "_vtable, i32 0, i32 0\n");
        curMethodStatements += ("store i8** " + var3 + ", i8*** " + var2 + "\n\n");

        return var1;
    }

    // creates a new int array with size: expResult and returns the proper IR variable name in which the int array is stored
    String createNewArray(String expResult) {
        // template
        //     ; start of array allocation
        //     %_6 = icmp slt i32 %_9, 0
        //     br i1 %_6, label %arr_alloc7, label %arr_alloc8

        // arr_alloc7:
        //     ; out of bounds
        //     call void @throw_oob()
        //     br label %arr_alloc8

        // arr_alloc8:
        //     ; actual array allocation
        //     %_3 = add i32 %_9, 1
        //     %_4 = call i8* @calloc(i32 4, i32 %_3)
        //     %_5 = bitcast i8* %_4 to i32*
        //     store i32 %_9, i32* %_5      ; store size of array in the first position of array
        //     ; end of actual array allocation

        String labelName1 = getNextArrayAllocLabelName(), labelName2 = getNextArrayAllocLabelName();
        String[] varNames = new String[5];
        for (int i = 0; i < 5; i++) {
            varNames[i] = getNextLocalVarName();
        }

        curMethodStatements += buildIcmp(varNames[1], expResult, "0", "slt", "i32");
        curMethodStatements += buildBranch(varNames[1], labelName1, labelName2);
        curMethodStatements += buildLabel(labelName1);
        curMethodStatements += buildThrowOob();
        curMethodStatements += buildBranch(labelName2);

        curMethodStatements += buildLabel(labelName2);
        curMethodStatements += buildAdd(varNames[2], expResult, "1");
        curMethodStatements += buildCalloc(varNames[3], "4", varNames[2]);
        curMethodStatements += buildBitcast(varNames[4], varNames[3], "i8*", "i32*");
        curMethodStatements += buildStore(expResult, varNames[4], "i32");

        return varNames[4]; // IR variable that has contains the array
    }

    // builds a simple expression and returns the proper IR variable name in which the result of the IR expression is stored
    String buildSimpleExpression(String expResult1, String symbol, String expResult2) {
        String IRVarName = getNextLocalVarName();
        switch (symbol) {
        case "+":
            curMethodStatements += buildAdd(IRVarName, expResult1, expResult2);
            break;
        case "-":
            curMethodStatements += buildSub(IRVarName, expResult1, expResult2);
            break;
        case "*":
            curMethodStatements += buildMul(IRVarName, expResult1, expResult2);

            break;
        case "<":
            curMethodStatements += buildIcmp(IRVarName, expResult1, expResult2, "slt", "i32");
            break;
        }

        return IRVarName;
    }

    // builds a not expression and returns the proper IR variable name in which the result of the IR expression is stored
    String buildNotExpression(String expResult) {
        String IRVarName = getNextLocalVarName();
        curMethodStatements += (IRVarName + " = icmp ne i1 " + expResult + ", 1\n");

        return IRVarName;
    }

    // builds an array length expression using expResult as the IR variable in which the int array is stored and returns the proper IR variable name in which the result of the IR expression is stored
    String buildArrayLengthExpression(String expResult) {
        // template
        // %_0 = getelementptr i32, i32* %ptr, i32 %idx
        // %_1 = load i32, i32* %_0

        String varName1 = getNextLocalVarName(), varName2 = getNextLocalVarName();

        curMethodStatements += buildGetElementPtr(varName1, expResult, "0", "i32");
        curMethodStatements += buildLoad(varName2, varName1, "i32");

        return varName2;
    }

    // builds an array lookup expression and returns the proper IR variable name in which the result of the IR expression is stored
    String buildArrayLookupExpression(String expResultArrName, String expResultIndex) {
        // template
        //      %_65 = load i32*, i32** %_64

        //      %_54 = load i32, i32 *%_65
        // 		%_55 = icmp ult i32 %_66, %_54      ; %_66 is the expResultIndex that is already loaded
        // 		br i1 %_55, label %oob60, label %oob61

        // oob60:
        //                 %_56 = add i32 %_66, 1
        //                 %_57 = getelementptr i32, i32* %_65, i32 %_56
        //                 %_58 = load i32, i32* %_57
        //                 br label %oob62

        // oob61:
        //                 call void @throw_oob()
        //                 br label %oob62

        // oob62:

        String[] varNames = new String[6], labelNames = new String[3];
        for (int i = 0; i < 6; i++) {
            if (i < 3) {
                labelNames[i] = getNextOobLabeName();
            }
            varNames[i] = getNextLocalVarName();
        }
        curMethodStatements += buildLoad(varNames[0], expResultArrName, "i32*");

        curMethodStatements += buildLoad(varNames[1], varNames[0], "i32");
        curMethodStatements += buildIcmp(varNames[2], expResultIndex, varNames[1], "ult", "i32");
        curMethodStatements += buildBranch(varNames[2], labelNames[0], labelNames[1]);
        curMethodStatements += buildLabel(labelNames[0]);
        curMethodStatements += buildAdd(varNames[3], expResultIndex, "1");
        curMethodStatements += buildGetElementPtr(varNames[4], varNames[0], varNames[3], "i32");

        curMethodStatements += buildLoad(varNames[5], varNames[4], "i32");
        curMethodStatements += buildBranch(labelNames[2]);
        curMethodStatements += buildLabel(labelNames[1]);
        curMethodStatements += buildThrowOob();
        curMethodStatements += buildBranch(labelNames[2]);
        curMethodStatements += buildLabel(labelNames[2]);

        return varNames[5];
    }

    // builds a message send expression and returns the proper IR variable name in which the result of the IR expression is stored
    // methodArgs: IR arguments with which the method with name methodName is called
    String buildMessageSend(String objectVar, String methodName, String methodArgs) {
        // template
        // %_0 = bitcast i8* %this to i8***
        // %_1 = load i8**, i8*** %_0
        // %_2 = getelementptr i8*, i8** %_1, i32 3
        // %_3 = load i8*, i8** %_2
        // %_4 = bitcast i8* %_3 to i32 (i8*,i32)*
        // %_5 = call i32 %_4(i8* %this, i32 %_6)

        // generate the names of variables that we will use
        String[] varNames = new String[6];
        for (int i = 0; i < 6; i++) {
            varNames[i] = getNextLocalVarName();
        }
        String[] methodOffsetAndClassName = symbols.getMethodOffsetAndClassName(methodName, lastObjectType); // [0] -> method's offset, [1] -> class name in which the method was last declared
        int vTableMethodIndex = Integer.parseInt(methodOffsetAndClassName[0]) / 8;

        // build IR method's signature
        String methodSignature = getIRType(symbols.getMethodType(methodName, lastObjectType, null, false)) + " ( i8*, ";
        for (String methodParamType : symbols.classesMaps.get(methodOffsetAndClassName[1]).methodParamTypes
                .get(methodName)) {
            methodSignature += (getIRType(methodParamType) + ", ");
        }
        methodSignature = methodSignature.substring(0, methodSignature.length() - 2);
        methodSignature += " )*";

        curMethodStatements += buildBitcast(varNames[0], objectVar, "i8*", "i8***");
        curMethodStatements += buildLoad(varNames[1], varNames[0], "i8**");
        curMethodStatements += buildGetElementPtr(varNames[2], varNames[1], vTableMethodIndex + "", "i8*");
        curMethodStatements += buildLoad(varNames[3], varNames[2], "i8*");
        curMethodStatements += buildBitcast(varNames[4], varNames[3], "i8*", methodSignature);
        curMethodStatements += buildCall(varNames[5], varNames[4],
                getIRType(symbols.getMethodType(methodName, lastObjectType, null, false)), methodArgs);

        return varNames[5];
    }

    // builds an assignment statement and returns the proper IR variable name in which the result of the IR statement is stored
    void buildAssignmentStatement(String value, String id, String type) {
        String IRType = getIRType(type);
        curMethodStatements += buildStore(value, id, IRType);

        return;
    }

    // builds an array assignment statement and returns the proper IR variable name in which the result of the IR statement is stored
    void buildArrayAssignmentStatement(String expResultArrName, String expResultIndex, String expResultRValue) {
        // template
        //     %_22 = load i32*, i32** %_21
        //     %_12 = load i32, i32 *%_22

        //     %_13 = icmp ult i32 0, %_12
        //     br i1 %_13, label %oob17, label %oob18

        // oob17:
        //     %_14 = add i32 0, 1
        //     %_15 = getelementptr i32, i32* %_22, i32 %_14
        //     store i32 20, i32* %_15
        //     br label %oob19

        // oob18:
        //     call void @throw_oob()
        //     br label %oob19

        // oob19:

        // generate the names of variables that we will use
        String[] varNames = new String[5], labelNames = new String[3];
        for (int i = 0; i < 5; i++) {
            if (i < 3) {
                labelNames[i] = getNextOobLabeName();
            }
            varNames[i] = getNextLocalVarName();
        }

        curMethodStatements += buildLoad(varNames[0], expResultArrName, "i32*");
        curMethodStatements += buildLoad(varNames[1], varNames[0], "i32");
        curMethodStatements += buildIcmp(varNames[2], expResultIndex, varNames[1], "ult", "i32");
        curMethodStatements += buildBranch(varNames[2], labelNames[0], labelNames[1]);
        curMethodStatements += buildLabel(labelNames[0]);
        curMethodStatements += buildAdd(varNames[3], expResultIndex, "1");
        curMethodStatements += buildGetElementPtr(varNames[4], varNames[0], varNames[3], "i32");
        curMethodStatements += buildStore(expResultRValue, varNames[4], "i32");
        curMethodStatements += buildBranch(labelNames[2]);
        curMethodStatements += buildLabel(labelNames[1]);
        curMethodStatements += buildThrowOob();
        curMethodStatements += buildBranch(labelNames[2]);
        curMethodStatements += buildLabel(labelNames[2]);

        return;
    }

    // END OF MY FUNCTIONS

    /********************************************************************************************************************************************/
    // NOTES: All epxressions' visit() methods return an IR variable's name (String) in which the result of the current expression is stored.
    //        Each expression's and statement's generated IR code is appended to current method declaration's curMethodStatements string which in the end is appended to the whole IR code's string
    /********************************************************************************************************************************************/

    /**
    * f0 -> MainClass()
    * f1 -> ( TypeDeclaration() )*
    * f2 -> <EOF>
    */
    // returns the whole generated IR code
    public String visit(Goal n, String[] argu) {
        // standard global method declarations
        String IRCode = "declare i8* @calloc(i32, i32)\ndeclare i32 @printf(i8*, ...)\ndeclare void @exit(i32)\n\n@_cint = constant [4 x i8] c\"%d\\0a\\00\"\n@_cOOB = constant [15 x i8] c\"Out of bounds\\0a\\00\"\ndefine void @print_int(i32 %i) {\n\t%_str = bitcast [4 x i8]* @_cint to i8*\n\tcall i32 (i8*, ...) @printf(i8* %_str, i32 %i)\n\tret void\n}\n\ndefine void @throw_oob() {\n\t%_str = bitcast [15 x i8]* @_cOOB to i8*\n\tcall i32 (i8*, ...) @printf(i8* %_str)\n\tcall void @exit(i32 1)\n\tret void\n}\n\n";

        methodCallArgs = new HashMap<Integer, String>();
        methodCallArgsIndexes = new HashMap<Integer, Integer>();

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
    public String visit(MainClass n, String[] argu) {
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

        methodDeclarations += "\tret i32 0\n}\n\n";
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
    public String visit(ClassExtendsDeclaration n, String[] argu) {
        String _ret = null;
        n.f0.accept(this, argu);
        String id1 = n.f1.accept(this, argu);
        n.f2.accept(this, argu);
        n.f3.accept(this, argu);

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
    public String visit(VarDeclaration n, String[] argu) {
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
    public String visit(MethodDeclaration n, String[] argu) {
        String _ret = null;

        methodDeclarations += "define ";
        curLocalVarIndex = 0; // reset local variable index
        curTabsNum = 1; // reset tabs number

        n.f0.accept(this, argu);
        String type = n.f1.accept(this, argu);
        String id = n.f2.accept(this, argu);

        String IRType = getIRType(type);

        methodDeclarations += (IRType + " @" + argu[0] + "." + id + "(i8* %this");

        n.f3.accept(this, argu);

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

        n.f9.accept(this, argu);

        retExpMode = true;
        String retExpResult = n.f10.accept(this, new String[] { METHOD, id, CLASS, argu[0] });
        retExpMode = false;

        n.f11.accept(this, argu);
        n.f12.accept(this, argu);

        methodDeclarations += curMethodStatements;
        methodDeclarations += "\tret " + IRType + " " + retExpResult + "\n}\n\n";
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
    public String visit(AssignmentStatement n, String[] argu) {
        String _ret = null;

        String id = n.f0.accept(this, argu);

        String idType = symbols.getVarType(id, argu[3], argu[1], Integer.toString(n.f1.beginLine), true);

        n.f1.accept(this, argu);

        String expResult = n.f2.accept(this, argu);
        n.f3.accept(this, argu);

        String IRId = createIRVarName(id, argu[3], argu[1], true);

        buildAssignmentStatement(expResult, IRId, idType);

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
    public String visit(ArrayAssignmentStatement n, String[] argu) {
        String _ret = null;
        inArrayAssignment = true;
        String expResultId = n.f0.accept(this, argu);
        inArrayAssignment = false;
        n.f1.accept(this, argu);
        String expResultIndex = n.f2.accept(this, argu);
        n.f3.accept(this, argu);

        n.f4.accept(this, argu);
        String expResultRValue = n.f5.accept(this, argu);
        n.f6.accept(this, argu);

        buildArrayAssignmentStatement(expResultId, expResultIndex, expResultRValue);

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
    public String visit(IfStatement n, String[] argu) {
        String _ret = null;
        n.f0.accept(this, argu);
        n.f1.accept(this, argu);
        String expResultCond = n.f2.accept(this, argu);

        String[] labelNames = new String[3];
        for (int i = 0; i < 3; i++) {
            labelNames[i] = getNextIfLabelName();
        }

        // template
        // br i1 %_9, label %if0, label %if1 ; cond check
        // if0:
        curMethodStatements += buildBranch(expResultCond, labelNames[0], labelNames[1]);
        curMethodStatements += buildLabel(labelNames[0]);
        curTabsNum++;

        n.f3.accept(this, argu);
        n.f4.accept(this, argu);
        n.f5.accept(this, argu);

        // template
        // br label %if2
        // if1: ; else
        curMethodStatements += buildBranch(labelNames[2]);
        curMethodStatements += buildLabel(labelNames[1]);

        n.f6.accept(this, argu);

        // template
        // br label %if2
        // if2:
        curMethodStatements += buildBranch(labelNames[2]);
        curMethodStatements += buildLabel(labelNames[2]);
        curTabsNum--;

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
    public String visit(WhileStatement n, String[] argu) {
        String _ret = null;
        n.f0.accept(this, argu);
        n.f1.accept(this, argu);

        String[] labelNames = new String[3];
        for (int i = 0; i < 3; i++) {
            labelNames[i] = getNextLoopLabelName();
        }

        // template
        // br label %loop0
        // loop0:
        curMethodStatements += buildBranch(labelNames[0]);
        curMethodStatements += buildLabel(labelNames[0]);

        String expResultCond = n.f2.accept(this, argu);

        // template
        // br i1 %_7, label %loop1, label %loop2
        // loop1:
        curMethodStatements += buildBranch(expResultCond, labelNames[1], labelNames[2]);
        curMethodStatements += buildLabel(labelNames[1]);
        curTabsNum++;

        n.f3.accept(this, argu);
        n.f4.accept(this, argu); // loop body

        // template
        // br label %loop0
        // loop2:
        curMethodStatements += buildBranch(labelNames[0]);
        curMethodStatements += buildLabel(labelNames[2]);
        curTabsNum--;

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
    public String visit(PrintStatement n, String[] argu) {
        String _ret = null;
        n.f0.accept(this, argu);
        n.f1.accept(this, argu);
        String expResult = n.f2.accept(this, argu);
        n.f3.accept(this, argu);
        n.f4.accept(this, argu);

        curMethodStatements += buildCall(null, "@print_int", "void (i32)", "i32 " + expResult);

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
        // template
        // ; start of and clause
        // br label %andclause13

        // andclause13:
        //     br i1 %_10, label %andclause14, label %andclause16  ; %_10 = clauseResult1

        // andclause14:
        //     %_18 = load i1, i1* %ret_val

        //     br label %andclause15

        // andclause15:
        //     br label %andclause16

        // andclause16:
        //     %_12 = phi i1 [ 0, %andclause13 ], [ %_17, %andclause15 ] ; %_17 = clauseResult2

        String varName = getNextLocalVarName();
        // generate the label names that will be used
        String[] labelNames = new String[4];
        for (int i = 0; i < 4; i++) {
            labelNames[i] = getNextAndClauseLabelName();
        }

        String clauseResult1 = n.f0.accept(this, argu);

        curMethodStatements += buildBranch(labelNames[0]);
        curMethodStatements += buildLabel(labelNames[0]);
        curMethodStatements += buildBranch(clauseResult1, labelNames[1], labelNames[3]);
        curMethodStatements += buildLabel(labelNames[1]);

        n.f1.accept(this, argu);

        String clauseResult2 = n.f2.accept(this, argu);

        curMethodStatements += buildBranch(labelNames[2]);
        curMethodStatements += buildLabel(labelNames[2]);
        curMethodStatements += buildBranch(labelNames[3]);
        curMethodStatements += buildLabel(labelNames[3]);
        curMethodStatements += buildPhi(varName, "0", labelNames[0], clauseResult2, labelNames[2]);

        return varName; // return IR variable that contains the result
    }

    /**
     * f0 -> PrimaryExpression()
     * f1 -> "<"
     * f2 -> PrimaryExpression()
     */
    public String visit(CompareExpression n, String[] argu) {
        inIntExp = true;
        String expResult1 = n.f0.accept(this, argu);
        n.f1.accept(this, argu);
        String expResult2 = n.f2.accept(this, argu);
        inIntExp = false;

        return buildSimpleExpression(expResult1, "<", expResult2);
    }

    /**
     * f0 -> PrimaryExpression()
     * f1 -> "+"
     * f2 -> PrimaryExpression()
     */
    public String visit(PlusExpression n, String[] argu) {
        inIntExp = true;
        String expResult1 = n.f0.accept(this, argu);
        n.f1.accept(this, argu);
        String expResult2 = n.f2.accept(this, argu);
        inIntExp = false;

        return buildSimpleExpression(expResult1, "+", expResult2);

    }

    /**
     * f0 -> PrimaryExpression()
     * f1 -> "-"
     * f2 -> PrimaryExpression()
     */
    public String visit(MinusExpression n, String[] argu) {
        inIntExp = true;
        String expResult1 = n.f0.accept(this, argu);
        n.f1.accept(this, argu);
        String expResult2 = n.f2.accept(this, argu);
        inIntExp = false;

        return buildSimpleExpression(expResult1, "-", expResult2);
    }

    /**
     * f0 -> PrimaryExpression()
     * f1 -> "*"
     * f2 -> PrimaryExpression()
     */
    public String visit(TimesExpression n, String[] argu) {
        inIntExp = true;
        String expResult1 = n.f0.accept(this, argu);
        n.f1.accept(this, argu);
        String expResult2 = n.f2.accept(this, argu);
        inIntExp = false;

        return buildSimpleExpression(expResult1, "*", expResult2);
    }

    /**
     * f0 -> PrimaryExpression()
     * f1 -> "["
     * f2 -> PrimaryExpression()
     * f3 -> "]"
     */
    public String visit(ArrayLookup n, String[] argu) {
        inArrayLookUpExp = true;
        String expResult1 = n.f0.accept(this, argu);
        inArrayLookUpExp = false;

        n.f1.accept(this, argu);

        inIntExp = true;
        String expResult2 = n.f2.accept(this, argu);
        inIntExp = false;

        n.f3.accept(this, argu);

        return buildArrayLookupExpression(expResult1, expResult2);
    }

    /**
     * f0 -> PrimaryExpression()
     * f1 -> "."
     * f2 -> "length"
     */
    public String visit(ArrayLength n, String[] argu) {
        String expResult = n.f0.accept(this, argu);
        n.f1.accept(this, argu);
        n.f2.accept(this, argu);

        return buildArrayLengthExpression(expResult);
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
    public String visit(MessageSend n, String[] argu) {

        storeExpType = true;
        inMessageSend = true;
        String expResult = n.f0.accept(this, argu);
        inMessageSend = false;
        storeExpType = false;

        n.f1.accept(this, argu);

        String methodName = n.f2.accept(this, argu);

        n.f3.accept(this, argu);

        methodCallArgs.put(nextMethodCallId, "i8* " + expResult); // start building current method call's arguments' string
        methodCallArgsIndexes.put(nextMethodCallId, 0); // start index from 0

        int curMethodId = nextMethodCallId++; // unique id for current method
        retExpMode = true; // using retExpMode to declare that we are visiting method call's arguments
        // lastObjectType: PrimaryExpression's type
        n.f4.accept(this, new String[] { METHOD, argu[1], CLASS, argu[3], METHOD, methodName, CLASS, lastObjectType,
                Integer.toString(curMethodId) });
        retExpMode = false;
        n.f5.accept(this, argu);

        return buildMessageSend(expResult, methodName, methodCallArgs.get(curMethodId));
    }

    /**
     * f0 -> Expression()
     * f1 -> ExpressionTail()
     */

    // argu[0]: METHOD, argu[1]: context method's name, argu[2]: CLASS, argu[3]: context class's name
    // argu[4]: METHOD, argu[5]: called method's name, argu[6]: CLASS, argu[7]: called method's class name
    public String visit(ExpressionList n, String[] argu) {
        String _ret = null;
        String expResult = n.f0.accept(this, argu);

        int curMethodId = Integer.parseInt(argu[8]);
        String curExpResult = buildMethodCallArg(expResult,
                symbols.getMethodParamType(argu[7], argu[5], methodCallArgsIndexes.get(curMethodId)));

        methodCallArgs.put(curMethodId, methodCallArgs.get(curMethodId) + curExpResult); // append to current method call's IR arguments
        methodCallArgsIndexes.put(curMethodId, methodCallArgsIndexes.get(curMethodId) + 1); // proceed current method call's next argument index

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
    public String visit(ExpressionTerm n, String[] argu) {
        String _ret = null;
        n.f0.accept(this, argu);
        String expResult = n.f1.accept(this, argu);

        int curMethodId = Integer.parseInt(argu[8]);
        String curExpResult = buildMethodCallArg(expResult,
                symbols.getMethodParamType(argu[7], argu[5], methodCallArgsIndexes.get(curMethodId)));

        methodCallArgs.put(curMethodId, methodCallArgs.get(curMethodId) + curExpResult); // append to current method call's IR arguments
        methodCallArgsIndexes.put(curMethodId, methodCallArgsIndexes.get(curMethodId) + 1); // proceed current method call's next argument index

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
    // returns the proper expression's type according to the choice between the above symbols
    public String visit(PrimaryExpression n, String[] argu) {
        String choice = n.f0.accept(this, argu);
        int choiceId = n.f0.which;

        switch (choiceId) {
        case 3: // identifier
            if (storeExpType) {
                // store variable's MiniJava type
                lastObjectType = symbols.getVarType(choice, argu[3], argu[1], null, true);
            }
            return createIRVarName(choice, argu[3], argu[1], false);

        case 4:
            if (storeExpType) {
                // store variable's MiniJava type
                lastObjectType = argu[3];
            }
            return choice;
        // in case 7 choice is the result of the expression that is inside brackets
        default:
            return choice;
        }
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
        return "1";
    }

    /**
     * f0 -> "false"
     */
    public String visit(FalseLiteral n, String[] argu) {
        return "0";
    }

    /**
     * f0 -> <IDENTIFIER>
     */
    // argu[0]: METHOD, argu[1]: context method's name, argu[2]: CLASS, argu[3]: context class's name
    // argu[4]: METHOD, argu[5]: called method's name, argu[6]: CLASS, argu[7]: called method's class name
    public String visit(Identifier n, String[] argu) {
        String id = n.f0.toString();
        if (inArrayAssignment) {
            // special case, we need to return an IR variable name instead of the original identifier
            return createIRVarName(id, argu[3], argu[1], false);
        }
        return id;
    }

    /**
     * f0 -> "this"
     */
    public String visit(ThisExpression n, String[] argu) {
        return "%" + n.f0.toString();
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
        String expResult = n.f3.accept(this, argu);
        n.f4.accept(this, argu);

        return createNewArray(expResult);
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
        n.f3.accept(this, argu);

        if (storeExpType) {
            lastObjectType = id;
        }
        return createNewObject(id);
    }

    /**
     * f0 -> "!"
     * f1 -> Clause()
     */
    public String visit(NotExpression n, String[] argu) {
        n.f0.accept(this, argu);
        String clauseResult = n.f1.accept(this, argu);
        return buildNotExpression(clauseResult);
    }

    /**
     * f0 -> "("
     * f1 -> Expression()
     * f2 -> ")"
     */
    public String visit(BracketExpression n, String[] argu) {
        n.f0.accept(this, argu);
        String expResult = n.f1.accept(this, argu);
        n.f2.accept(this, argu);
        return expResult;
    }
}
