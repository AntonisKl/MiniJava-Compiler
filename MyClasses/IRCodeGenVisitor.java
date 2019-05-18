package MyClasses;

import syntaxtree.*;
import visitor.GJDepthFirst;

public class IRCodeGenVisitor extends GJDepthFirst<String, String[]> {
    // these are used as defines
    final static String METHOD = "method";
    final static String CLASS = "class";
    // final static String MESSAGE_SEND = "message_send";

    final static String BOOLEAN = "boolean";
    final static String INT = "int";
    final static String INT_ARRAY = "int[]";

    int curLocalVarIndex, curArrayAllocLabelIndex = 0, curAndClauseLabelIndex = 0, curOobLabelindex = 0,
            curMethodCallArgIndex, curTabsNum;
    String curMethodParams, curMethodVarDeclarations, methodDeclarations, curMethodStatements, curMethodCallArgs,
            lastObjectType;

    boolean storeExpType;
    // boolean curExpIsRightOfAndExp = false;

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

    String buildMethodCallArg(String varName, String type) {
        return ", " + getIRType(type) + " " + varName;
    }

    String buildLocalVarFromParam(String id, String type) {
        String IRType = getIRType(type);
        return "\t%" + id + " = alloca " + IRType + "\n\tstore " + IRType + " %." + id + ", " + IRType + "* %" + id
                + "\n\n";
    }

    String buildLoad(String resultVar, String varToBeLoaded, String IRType) {
        return buildStatement(resultVar + " = load " + IRType + ", " + IRType + "* " + varToBeLoaded);
    }

    String buildBitcast(String resultVar, String varToBeBitcasted, String fromType, String toType) {
        // %_32 = bitcast i8* %_31 to i32**
        return buildStatement(resultVar + " = bitcast " + fromType + " " + varToBeBitcasted + " to " + toType);
    }

    String buildGetElementPtr(String resultVar, String arrayVar, String offsetVar, String type) {
        // 	%_31 = getelementptr i8, i8* %this, i32 8
        return buildStatement(
                resultVar + " = getelementptr " + type + ", " + type + "* " + arrayVar + ", i32 " + offsetVar);
    }

    String buildIcmp(String resultVar, String varName1, String varName2, String operation, String type) {
        //     %_13 = icmp ult i32 0, %_12
        return buildStatement(resultVar + " = icmp " + operation + " " + type + " " + varName1 + ", " + varName2);
    }

    String buildBranch(String condVar, String label1, String label2) {
        // br i1 %_13, label %oob17, label %oob18
        return buildStatement("br i1 " + condVar + ", label " + label1 + ", label " + label2);
    }

    String buildBranch(String label) {
        // br label %oob17
        return buildStatement("br label " + label);
    }

    String buildLabel(String label) {
        return label + ":\n";
    }

    String buildAdd(String resultVar, String var1, String var2) {
        //     %_14 = add i32 0, 1
        return buildStatement(resultVar + " = add i32 " + var1 + ", " + var2);
    }

    String buildSub(String resultVar, String var1, String var2) {
        //     %_14 = add i32 0, 1
        return buildStatement(resultVar + " = sub i32 " + var1 + ", " + var2);
    }

    String buildMul(String resultVar, String var1, String var2) {
        //     %_14 = add i32 0, 1
        return buildStatement(resultVar + " = mul i32 " + var1 + ", " + var2);
    }

    String buildThrowOob() {
        return buildStatement("call void @throw_oob()");
    }

    String buildCalloc(String resultVar, String arg1, String arg2) {
        //     %_4 = call i8* @calloc(i32 4, i32 %_3)
        return buildStatement(resultVar + " = call i8* @calloc(i32 " + arg1 + ", i32 " + arg2);
    }

    String buildStore(String fromVar, String toVar, String type) {
        //     store i32 %_9, i32* %_5
        return buildStatement("store " + type + " " + fromVar + ", " + type + "* " + toVar);
    }

    String buildCall(String resultVar, String methodNameVar, String retType, String methodArgs) {
        // %_5 = call i32 %_4(i8* %this, i32 %_6)
        if (resultVar != null)
            return buildStatement(resultVar + " = call " + retType + " " + methodNameVar + "(" + methodArgs + ")");
        else
            return buildStatement("call " + retType + " " + methodNameVar + "(" + methodArgs + ")");
    }

    String buildStatement(String statement) {
        String retValue = "";
        for (int i = 0; i < curTabsNum; i++)
            retValue += "\t";
        retValue += (statement + "\n");
        return retValue;
    }

    int getVTableOffset(int offset) {
        return offset + 8;
    }

    String getNextLocalVarName() {
        return "%_" + (curLocalVarIndex++);
    }

    String getNextArrayAllocLabelName() {
        return "arr_alloc" + (curArrayAllocLabelIndex++);
    }

    String getNextAndClauseLabelName() {
        return "and_clause" + (curAndClauseLabelIndex++);
    }

    String getNextOobLabeName() {
        return "oob" + (curOobLabelindex++);
    }

    String createIRVarName(String id, String className, String methodName, boolean isLeftValue) {
        String foundMethodVarType = symbols.getVarType(id, className, methodName, null, false), IRVarName = null;

        if (foundMethodVarType != null) { // variable is local
            if (isLeftValue) { // left value of assignment
                IRVarName = "%_" + id;
            } else { // not a left value of assignment
                String IRType = getIRType(foundMethodVarType); //symbols.getVarType(id, className, methodName, null, true)
                // %_0 = load i32, i32* %sz
                IRVarName = getNextLocalVarName();
                // curMethodStatements += ("\t" + IRVarName + " = load " + IRType + ", " + IRType + "* %" + id + "\n");
                curMethodStatements += buildLoad(IRVarName, "%" + id, IRType);
                // curLocalVarIndex++;
            }
        } else { // variable belongs to the class scope
            foundMethodVarType = symbols.getVarType(id, className, methodName, null, true);
            String IRType = getIRType(foundMethodVarType); //symbols.getVarType(id, className, methodName, null, true)
            // %_1 = getelementptr i8, i8* %this, i32 16
            // %_2 = bitcast i8* %_1 to i32*
            curMethodStatements += ("\t" + getNextLocalVarName() + " = getelementptr i8, i8* %this, " + IRType + " "
                    + getVTableOffset(symbols.getVarOffset(id, className)) + "\n");
            IRVarName = getNextLocalVarName();
            // curMethodStatements += ("\t" + IRVarName + " = bitcast i8* %_" + (curLocalVarIndex - 2) + " to " + IRType
            //         + "*\n");
            curMethodStatements += buildBitcast(IRVarName, "%_" + (curLocalVarIndex - 2), "i8*", IRType + "*");
        }

        return IRVarName;
    }

    String createNewObject(String className) {
        // %_0 = call i8* @calloc(i32 1, i32 38) ; 30 bytes(fields) + 8 bytes(v-table pointer)
        // %_1 = bitcast i8* %_0 to i8***
        // %_2 = getelementptr [20 x i8*], [20 x i8*]* @.Tree_vtable, i32 0, i32 0
        // store i8** %_2, i8*** %_1

        String var1 = getNextLocalVarName(), var2 = getNextLocalVarName(), var3 = getNextLocalVarName();
        int vTableSize = symbols.classesVTableSizes.get(className);

        curMethodStatements += (var1 + " = call i8* @calloc(i32 1, i32 " + symbols.getSizeOfFieldsInBytes(className)
                + ")\n");
        curMethodStatements += (var2 + " = bitcast i8* " + var1 + " to i8***\n");
        curMethodStatements += (var3 + " = getelementptr [" + vTableSize + " x i8*], [" + vTableSize + " x i8*]* @."
                + className + "_vtable, i32 0, i32 0\n");
        curMethodStatements += ("store i8** " + var3 + ", i8*** " + var2 + "\n\n");

        return var1;
    }

    String createNewArray(String expResult) {
        //     ; start of array allocation
        //     %_9 = load i32, i32* %sz
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

        // curMethodStatements += (varNames[0] + " = load i32, i32* " + expResult + "\n");
        // curMethodStatements += (varNames[1] + " = icmp slt i32 " + varNames[0] + ", 0\n");
        // curMethodStatements += ("br i1 " + varNames[1] + ", label %" + labelName1 + ", label %" + labelName2 + "\n\n");
        // curMethodStatements += (labelName1 + ":\n");
        // curMethodStatements += "\tcall void @throw_oob()\n";
        // curMethodStatements += ("\tbr label " + labelName2 + "\n\n");

        // curMethodStatements += (labelName2 + ":\n");
        // curMethodStatements += ("\t" + varNames[2] + " = add i32 " + varNames[0] + ", 1\n");
        // curMethodStatements += ("\t" + varNames[3] + " = call i8* @calloc(i32 4, i32 " + varNames[2] + "\n");
        // curMethodStatements += ("\t" + varNames[4] + " = bitcast i8* " + varNames[3] + " to i32*");
        // curMethodStatements += ("\tstore i32 " + varNames[0] + ", i32* " + varNames[4] + "\n");

        curMethodStatements += buildLoad(varNames[0], expResult, "i32");
        curMethodStatements += buildIcmp(varNames[1], varNames[0], "0", "slt", "i32");
        curMethodStatements += buildBranch(varNames[1], labelName1, labelName2);
        curMethodStatements += buildLabel(labelName1);
        curMethodStatements += buildThrowOob();
        curMethodStatements += buildBranch(labelName2);

        curMethodStatements += buildLabel(labelName2);
        curMethodStatements += buildAdd(varNames[2], varNames[0], "1");
        curMethodStatements += buildCalloc(varNames[3], "4", varNames[2]);
        curMethodStatements += buildBitcast(varNames[4], varNames[3], "i8*", "i32*");
        curMethodStatements += buildStore(varNames[0], varNames[4], "i32");

        return varNames[4]; // IR variable that has contains the array
    }

    String buildSimpleExpression(String expResult1, String symbol, String expResult2) {
        String IRVarName = getNextLocalVarName();
        switch (symbol) {
        case "+":
            // %_1 = add i32 %_0, 1
            // curMethodStatements += (IRVarName + " = add i32 " + expResult1 + ", " + expResult2 + "\n");
            curMethodStatements += buildAdd(IRVarName, expResult1, expResult2);

            break;
        case "-":
            // curMethodStatements += (IRVarName + " = sub i32 " + expResult1 + ", " + expResult2 + "\n");
            curMethodStatements += buildSub(IRVarName, expResult1, expResult2);
            break;
        case "*":
            // curMethodStatements += (IRVarName + " = mul i32 " + expResult1 + ", " + expResult2 + "\n");
            curMethodStatements += buildMul(IRVarName, expResult1, expResult2);

            break;
        case "<":
            // %_7 = icmp slt i32 %_5, %_6
            // curMethodStatements += (IRVarName + " = icmp slt i32 " + expResult1 + ", " + expResult2 + "\n");

            curMethodStatements += buildIcmp(IRVarName, expResult1, expResult2, "slt", "i32");
            break;
        }

        return IRVarName;
    }

    String buildNotExpression(String expResult) {
        String IRVarName = getNextLocalVarName();
        curMethodStatements += (IRVarName + " = icmp ne i1 " + expResult + ", 1\n");

        return IRVarName;
    }

    String buildArrayLengthExpression(String expResult) {
        // %_0 = getelementptr i32, i32* %ptr, i32 %idx
        // %_1 = load i32, i32* %_0

        String varName1 = getNextLocalVarName(), varName2 = getNextLocalVarName();
        // curMethodStatements += (varName1 + " = getelementptr i32, i32* " + expResult + ", i32 0\n");
        // curMethodStatements += (varName2 + " = load i32, i32* " + varName1 + "\n");

        curMethodStatements += buildGetElementPtr(varName1, expResult, "0", "i32");
        curMethodStatements += buildLoad(varName2, varName1, "i32");

        return varName2;
    }

    String buildArrayLookupExpression(String expResultArrName, String expResultIndex) {
        //     %_22 = load i32*, i32** %_21
        //     %_12 = load i32, i32 *%_22

        //     %_13 = icmp ult i32 0, %_12
        //     br i1 %_13, label %oob17, label %oob18

        // oob17:
        //     %_14 = add i32 0, 1
        //     %_15 = getelementptr i32, i32* %_22, i32 %_14

        //     br label %oob19

        // oob18:
        //     call void @throw_oob()
        //     br label %oob19

        // oob19:
        //     store i32 20, i32* %_15 ; assignment function (will be called after this java function)

        String[] varNames = new String[5], labelNames = new String[3];
        for (int i = 0; i < 5; i++) {
            if (i < 3) {
                labelNames[i] = getNextOobLabeName();
            }
            varNames[i] = getNextLocalVarName();
        }

        curMethodStatements += buildLoad(varNames[0], expResultArrName, "i32*");
        curMethodStatements += buildLoad(varNames[1], varNames[0], "i32");
        curMethodStatements += buildIcmp(varNames[2], "0", varNames[1], "ult", "i32");
        curMethodStatements += buildBranch(varNames[2], labelNames[0], labelNames[1]);
        curMethodStatements += buildLabel(labelNames[0]);
        curMethodStatements += buildAdd(varNames[3], expResultIndex, "1");
        curMethodStatements += buildGetElementPtr(varNames[4], varNames[0], varNames[3], "i32");
        curMethodStatements += buildBranch(labelNames[2]);
        curMethodStatements += buildLabel(labelNames[1]);
        curMethodStatements += buildThrowOob();
        curMethodStatements += buildBranch(labelNames[2]);

        return varNames[4];
    }

    String buildMessageSend(String objectVar, String methodName, String methodArgs) {
        // %_0 = bitcast i8* %this to i8***
        // %_1 = load i8**, i8*** %_0
        // %_2 = getelementptr i8*, i8** %_1, i32 3
        // %_3 = load i8*, i8** %_2
        // %_4 = bitcast i8* %_3 to i32 (i8*,i32)*
        // %_6 = load i32, i32* %sz ; has already happened by previous visit function ??????????????????????????????
        // %_5 = call i32 %_4(i8* %this, i32 %_6)

        String[] varNames = new String[6];
        for (int i = 0; i < 6; i++) {
            varNames[i] = getNextLocalVarName();
        }
        // System.out.println("last object type: " + lastObjectType);

        int vTableMethodIndex = symbols.getMethodOffset(methodName, lastObjectType) / 8;
        // System.out.println("                                                index: " + vTableMethodIndex);

        curMethodStatements += buildBitcast(varNames[0], objectVar, "i8*", "i8***");
        curMethodStatements += buildLoad(varNames[1], varNames[0], "i8**");
        curMethodStatements += buildGetElementPtr(varNames[2], varNames[1], vTableMethodIndex + "", "i8*");
        curMethodStatements += buildLoad(varNames[3], varNames[2], "i8*");
        curMethodStatements += buildBitcast(varNames[4], varNames[3], "i8*",
                symbols.classesVTableMethodTypes.get(lastObjectType).get(vTableMethodIndex));
        // curMethodStatements += buildLoad(varNames[5], varToBeLoaded, IRType)
        curMethodStatements += buildCall(varNames[5], varNames[4],
                getIRType(symbols.getMethodType(methodName, lastObjectType, null, false)), methodArgs);

        return varNames[5];
    }

    // String buildAndExpression(String expResult1, String expResult2) {
    //     // %_10 = xor i1 1, %_11 ; %_10 = expResult1

    //     // ; start of and clause
    //     // br label %andclause13

    //     // andclause13:
    //     //     br i1 %_10, label %andclause14, label %andclause16

    //     // andclause14:
    //     //     %_18 = load i1, i1* %ret_val
    //     //     %_17 = xor i1 1, %_18  ; %_17 expResult2

    //     //     br label %andclause15

    //     // andclause15:
    //     //     br label %andclause16

    //     // andclause16:
    //     //     %_12 = phi i1 [ 0, %andclause13 ], [ %_17, %andclause15 ]
    //     //     br i1 %_12, label %loop8, label %loop9

    //     String varName = getNextLocalVarName();
    //     String[] labelNames = new String[4];
    //     for (int i = 0; i < 4; i++) {
    //         labelNames = getNextAndClauseLabelName()
    //     }

    //     curMethodStatements += ("br label %" + labelNames[0] + "\n\n");
    //     curMethodStatements += (labelNames[0] + ":\n");
    //     curMethodStatements += ("\tbr i1 " + expResult1 + ", label %" + labelNames[1] + ", label %" + labelNames[3] + "\n\n");
    //     curMethodStatements += (labelNames[1] + ":\n");
    //     curMethodStatements += ("\t" + varNames[0] + " = " )
    // }

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
        curTabsNum = 1;

        n.f0.accept(this, argu);
        String type = n.f1.accept(this, argu);
        String id = n.f2.accept(this, argu);

        methodDeclarations += (getIRType(symbols.getMethodType(argu[0], id)) + " @" + argu[0] + "." + id
                + "(i8* %this");

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
    public String visit(AssignmentStatement n, String[] argu) {
        String _ret = null;

        String id = n.f0.accept(this, argu);

        String idType = symbols.getVarType(id, argu[3], argu[1], Integer.toString(n.f1.beginLine), true);

        n.f1.accept(this, argu);
        String expResult = n.f2.accept(this, argu);
        n.f3.accept(this, argu);

        // String firstInheritedClassName = symbols.getFirstInheritedClassName(expResult);
        String IRId = createIRVarName(id, argu[3], argu[1], true);

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
    public String visit(ArrayAssignmentStatement n, String[] argu) {
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
    public String visit(IfStatement n, String[] argu) {
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
    public String visit(WhileStatement n, String[] argu) {
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
    public String visit(PrintStatement n, String[] argu) {
        String _ret = null;
        n.f0.accept(this, argu);
        n.f1.accept(this, argu);
        String expResult = n.f2.accept(this, argu);
        n.f3.accept(this, argu);
        n.f4.accept(this, argu);

        // call void (i32) @print_int(i32 %_8)

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
        // %_10 = xor i1 1, %_11 ; %_10 = expResult1

        // ; start of and clause
        // br label %andclause13

        // andclause13:
        //     br i1 %_10, label %andclause14, label %andclause16

        // andclause14:
        //     %_18 = load i1, i1* %ret_val
        //     %_17 = xor i1 1, %_18  ; %_17 expResult2

        //     br label %andclause15

        // andclause15:
        //     br label %andclause16

        // andclause16:
        //     %_12 = phi i1 [ 0, %andclause13 ], [ %_17, %andclause15 ]

        String varName = getNextLocalVarName();
        String[] labelNames = new String[4];
        for (int i = 0; i < 4; i++) {
            labelNames[i] = getNextAndClauseLabelName();
        }

        String clauseResult1 = n.f0.accept(this, argu);

        curMethodStatements += ("br label %" + labelNames[0] + "\n\n");
        curMethodStatements += (labelNames[0] + ":\n");
        curMethodStatements += ("\tbr i1 " + clauseResult1 + ", label %" + labelNames[1] + ", label %" + labelNames[3]
                + "\n\n");
        curMethodStatements += (labelNames[1] + ":\n");

        n.f1.accept(this, argu);

        // curExpIsRightOfAndExp = true;

        String clauseResult2 = n.f2.accept(this, argu);

        curMethodStatements += ("\tbr label " + labelNames[2] + "\n");
        curMethodStatements += (labelNames[2] + ":\n");
        curMethodStatements += ("\tbr label " + labelNames[3] + "\n");
        curMethodStatements += (labelNames[3] + ":\n");
        curMethodStatements += ("\t" + varName + " = phi i1 [ 0, %" + labelNames[0] + " ], [ " + clauseResult2 + ", %"
                + labelNames[2] + "\n");

        return varName; // return IR variable that contains the result
    }

    /**
     * f0 -> PrimaryExpression()
     * f1 -> "<"
     * f2 -> PrimaryExpression()
     */
    public String visit(CompareExpression n, String[] argu) {
        String expResult1 = n.f0.accept(this, argu);
        n.f1.accept(this, argu);
        String expResult2 = n.f2.accept(this, argu);

        return buildSimpleExpression(expResult1, "<", expResult2);
    }

    /**
     * f0 -> PrimaryExpression()
     * f1 -> "+"
     * f2 -> PrimaryExpression()
     */
    public String visit(PlusExpression n, String[] argu) {
        String expResult1 = n.f0.accept(this, argu);
        n.f1.accept(this, argu);
        String expResult2 = n.f2.accept(this, argu);

        return buildSimpleExpression(expResult1, "+", expResult2);

    }

    /**
     * f0 -> PrimaryExpression()
     * f1 -> "-"
     * f2 -> PrimaryExpression()
     */
    public String visit(MinusExpression n, String[] argu) {
        String expResult1 = n.f0.accept(this, argu);
        n.f1.accept(this, argu);
        String expResult2 = n.f2.accept(this, argu);

        return buildSimpleExpression(expResult1, "-", expResult2);
    }

    /**
     * f0 -> PrimaryExpression()
     * f1 -> "*"
     * f2 -> PrimaryExpression()
     */
    public String visit(TimesExpression n, String[] argu) {
        String expResult1 = n.f0.accept(this, argu);
        n.f1.accept(this, argu);
        String expResult2 = n.f2.accept(this, argu);

        return buildSimpleExpression(expResult1, "*", expResult2);
    }

    /**
     * f0 -> PrimaryExpression()
     * f1 -> "["
     * f2 -> PrimaryExpression()
     * f3 -> "]"
     */
    public String visit(ArrayLookup n, String[] argu) {
        String expResult1 = n.f0.accept(this, argu);
        n.f1.accept(this, argu);

        String expResult2 = n.f2.accept(this, argu);
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
        String expResult = n.f0.accept(this,
                argu /*new String[] { MESSAGE_SEND, argu[0], argu[1], argu[2], argu[3] }*/);
        storeExpType = false;
        // String objectVar = null, objectType = null;
        // if (expResult.equals("%this")) {
        //     objectVar = expResult;
        //     objectType = argu[3];
        // } else {
        //     System.out.println(expResult);
        //     String[] objectVarAndType = expResult.split("~");
        //     objectVar = objectVarAndType[0];
        //     objectType = objectVarAndType[1];
        // }

        n.f1.accept(this, argu);

        String methodName = n.f2.accept(this, argu);

        // System.out.println("method name: " + methodName + "object: " + type);
        // // get method's return type
        // String methodType = symbols.getMethodType(methodName, type, null, true);

        n.f3.accept(this, argu);

        curMethodCallArgs = "i8* %this";
        curMethodCallArgIndex = 0;
        n.f4.accept(this, new String[] { METHOD, argu[1], CLASS, argu[3], METHOD, methodName, CLASS, lastObjectType });

        n.f5.accept(this, argu);

        // if (!n.f4.present())
        //     return methodType;

        // return methodType;
        return buildMessageSend(expResult, methodName, curMethodCallArgs);
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

        curMethodCallArgs += buildMethodCallArg(expResult,
                symbols.getMethodParamType(argu[7], argu[5], curMethodCallArgIndex));

        curMethodCallArgIndex++;
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

        curMethodCallArgs += buildMethodCallArg(expResult,
                symbols.getMethodParamType(argu[7], argu[5], curMethodCallArgIndex));

        curMethodCallArgIndex++;

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
                lastObjectType = symbols.getVarType(choice, argu[3], argu[1], null, true);
            }
            // if (argu[0].equals(METHOD))
            return createIRVarName(choice, argu[3], argu[1], false);

        //     return createIRVarName(choice, argu[4], argu[2], false) + "~"
        //             + symbols.getVarType(choice, argu[4], argu[2], null, true); // return the type of the variable as well

        case 4:
            if (storeExpType) {
                lastObjectType = argu[3];
            }
            return choice;

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
        return n.f0.toString();
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
        // if (argu[0].equals(MESSAGE_SEND))
        //     return createNewObject(id) + "~" + id;
        // else
        return createNewObject(id);
    }

    /**
     * f0 -> "!"
     * f1 -> Clause()
     */
    public String visit(NotExpression n, String[] argu) {
        n.f0.accept(this, argu);
        String clauseResult = n.f1.accept(this, argu);
        return buildNotExpression(clauseResult); // return clause's result
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
        return expResult; // return expression's result
    }
}
