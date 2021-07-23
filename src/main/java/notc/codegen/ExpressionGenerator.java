package notc.codegen;

import notc.antlrgen.NotCParser;
import notc.antlrgen.NotCParser.Type;
import notc.antlrgen.NotCBaseVisitor;
import notc.antlrgen.NotCParser.ExpressionContext;
import notc.antlrgen.NotCParser.FalseLiteralExpressionContext;
import notc.antlrgen.NotCParser.TrueLiteralExpressionContext;
import notc.antlrgen.NotCParser.DoubleLiteralExpressionContext;
import notc.antlrgen.NotCParser.IntLiteralExpressionContext;
import notc.antlrgen.NotCParser.StringLiteralExpressionContext;
import notc.antlrgen.NotCParser.VariableExpressionContext;
import notc.antlrgen.NotCParser.FunctionCallExpressionContext;
import notc.antlrgen.NotCParser.AssignmentExpressionContext;
import notc.antlrgen.NotCParser.ArithmeticExpressionContext;
import notc.antlrgen.NotCParser.ComparisonExpressionContext;

import org.antlr.v4.runtime.Token;

import java.util.Map;

class ExpressionGenerator extends NotCBaseVisitor<Void> {
    private static Map<String,String> methodSymTab;
    private JvmMethod targetMethod;
    private static ExpressionGenerator instance = new ExpressionGenerator();
    private ExpressionGenerator() {}
    static void setMethodSymTab(Map<String,String> methods) {
        methodSymTab = methods;
    }

    static ExpressionGenerator withTarget(JvmMethod method) {
        instance.targetMethod = method;
        return instance;
    }

    // Literals: instructions to put constant values on the stack
    @Override
    public Void visitFalseLiteralExpression(FalseLiteralExpressionContext falseLiteralExpr) {
        targetMethod.addInstruction("    ldc 0", 1);
        return null;
    }

    @Override
    public Void visitTrueLiteralExpression(TrueLiteralExpressionContext trueLiteralExpr) {
        targetMethod.addInstruction("    ldc 1", 1);
        return null;
    }

    @Override
    public Void visitDoubleLiteralExpression(DoubleLiteralExpressionContext doubleLiteralExpr) {
        String srcText = doubleLiteralExpr.value.getText();
        targetMethod.addInstruction("    ldc2_w " + srcText, 2);
        return null;
    }

    @Override
    public Void visitIntLiteralExpression(IntLiteralExpressionContext intLitExpr) {
        String srcText = intLitExpr.value.getText();
        targetMethod.addInstruction("    ldc " + srcText, 1);
        return null;
    }

    @Override
    public Void visitStringLiteralExpression(StringLiteralExpressionContext strLitExpr) {
        String srcText = strLitExpr.value.getText();
        targetMethod.addInstruction("    ldc " + srcText, 1);
        return null;
    }

    // Variable expression: look up its address and load it
    @Override
    public Void visitVariableExpression(VariableExpressionContext varExpr) {
        Type varType = varExpr.type;
        int varAddr = targetMethod.lookupVar(varExpr.varId);
        if (varType.isDouble())
            targetMethod.addInstruction("    dload " + varAddr, 2);
        else if (varType.isString())
            targetMethod.addInstruction("    aload " + varAddr, 1);
        else
            targetMethod.addInstruction("    iload " + varAddr, 1);
        return null;
    }

    // Function calls
    @Override
    public Void visitFunctionCallExpression(FunctionCallExpressionContext funCall) {
        // Put all arguments on stack and calculate their size
        int argStackSize = 0;
        for (ExpressionContext arg : funCall.args) {
            arg.accept(this);
            if (arg.type.isDouble())
                argStackSize += 2;
            else
                argStackSize += 1; // int, bool, string
        }

        // Return value is left on stack, so calculate its size
        Type returnType = funCall.type;
        int returnStackSize = 0;
        if (returnType.isDouble())
            returnStackSize = 2;
        else if (!returnType.isVoid())
            returnStackSize = 1;

        String invocation = "    invokestatic " + methodSymTab.get(funCall.id.getText());
                                          // Arguments are popped, return value is pushed
        targetMethod.addInstruction(invocation, returnStackSize - argStackSize);
        return null;
    }

    // Assignments
    @Override
    public Void visitAssignmentExpression(AssignmentExpressionContext assExpr) {
        assExpr.rhs.accept(this); // Expression on the right of = goes on stack
        int varAddr = targetMethod.lookupVar(assExpr.varId);
        String storeInstr;
        String dupInstr;
        int stackSpace;
        if (assExpr.type.isDouble()) {
            storeInstr = "    dstore ";
            dupInstr   = "    dup2";
            stackSpace = 2;
        } else if (assExpr.type.isString()) {
            storeInstr = "    astore ";
            dupInstr   = "    dup";
            stackSpace = 1;
        } else { // ints, bools
            storeInstr = "    istore ";
            dupInstr   = "    dup";
            stackSpace = 1;
        }
        // Stored value is value of expression and is left on stack
        targetMethod.addInstruction(dupInstr, stackSpace);
        targetMethod.addInstruction(storeInstr + varAddr, -stackSpace);
        return null;
    }

    // +, -, *, /, %
    @Override
    public Void visitArithmeticExpression(ArithmeticExpressionContext arithmExpr) {
        // Generate operands
        arithmExpr.opnd1.accept(this);
        arithmExpr.opnd2.accept(this);
        String operation = operationByToken(arithmExpr.op);
        if (arithmExpr.type.isInt()) // stack: i i -> i
            targetMethod.addInstruction("    i" + operation, -1);
        else // stack: d d -> d
            targetMethod.addInstruction("    d" + operation, -2);

        return null;
    }

    private String operationByToken(Token opTok) {
        switch (opTok.getType()) {
            case NotCParser.MUL:  return "mul";
            case NotCParser.DIV:  return "div";
            case NotCParser.REM:  return "rem";
            case NotCParser.ADD:  return "add";
            case NotCParser.SUB:  return "sub";
            default:              return "";
        }
    }

    @Override
    public Void visitComparisonExpression(ComparisonExpressionContext compExpr) {
        // Put operands on stack
        compExpr.opnd1.accept(this);
        compExpr.opnd2.accept(this);
        Type t1 = compExpr.opnd1.type;
        Type t2 = compExpr.opnd2.type;
        if (t1.isInt() && t2.isInt() || t1.isBool())
            generateIntComparison(compExpr);
        else
            generateDoubleComparison(compExpr);
        return null;
    }

    private void generateIntComparison(ComparisonExpressionContext compExpr) {
        String trueLabel = targetMethod.newLabel();
        String endLabel = targetMethod.newLabel();
        String op = null;
        switch (compExpr.op.getType()) {
            case NotCParser.LT:  op = "lt";
                                 break;
            case NotCParser.GT:  op = "gt";
                                 break;
            case NotCParser.GE:  op = "ge";
                                 break;
            case NotCParser.LE:  op = "le";
                                 break;
            case NotCParser.EQ:  op = "eq";
                                 break;
            case NotCParser.NE:  op = "ne";
                                 break;
        }
        targetMethod.addInstruction("    if_icmp" + op + " " + trueLabel, -2);
        targetMethod.addInstruction("    iconst_0", 1); // false
        targetMethod.addInstruction("    goto " + endLabel, 0);
        targetMethod.addInstruction(trueLabel + ":", 0);
        targetMethod.addInstruction("    iconst_1", 1); // true
        targetMethod.addInstruction(endLabel + ":", 0);
    }

    private void generateDoubleComparison(ComparisonExpressionContext compExpr) {
        String trueLabel = targetMethod.newLabel();
        String falseLabel = targetMethod.newLabel();
        String endLabel = targetMethod.newLabel();
        targetMethod.addInstruction("    dcmpg", -3); // stack: d d -> i
        switch (compExpr.op.getType()) {
            case NotCParser.LT:  // a < b -> TOS = -1
                                 targetMethod.addInstruction("    iconst_m1", 1);
                                 targetMethod.addInstruction("    if_icmpeq " + trueLabel, -2);
                                 targetMethod.addInstruction("    goto " + falseLabel, 0);
                                 break;
            case NotCParser.GT:  // a > b -> TOS = 1
                                 targetMethod.addInstruction("    iconst_1", 1);
                                 targetMethod.addInstruction("    if_icmpeq " + trueLabel, -2);
                                 targetMethod.addInstruction("    goto " + falseLabel, 0);
                                 break;
            case NotCParser.GE:  // a >= b -> TOS != -1
                                 targetMethod.addInstruction("    iconst_m1", 1);
                                 targetMethod.addInstruction("    if_icmpeq " + falseLabel, -2);
                                 targetMethod.addInstruction("    goto " + trueLabel, 0);
                                 break;
            case NotCParser.LE:  // a <= b -> TOS != 1
                                 targetMethod.addInstruction("    iconst_1", 1);
                                 targetMethod.addInstruction("    if_icmpeq " + falseLabel, -2);
                                 targetMethod.addInstruction("    goto " + trueLabel, 0);
                                 break;
            case NotCParser.EQ:  // a = b -> TOS = 0
                                 targetMethod.addInstruction("    iconst_0", 1);
                                 targetMethod.addInstruction("    if_icmpeq " + trueLabel, -2);
                                 targetMethod.addInstruction("    goto " + falseLabel, 0);
                                 break;
            case NotCParser.NE:  // a != b -> TOS != 0
                                 targetMethod.addInstruction("    iconst_0", 1);
                                 targetMethod.addInstruction("    if_icmpeq " + falseLabel, -2);
                                 targetMethod.addInstruction("    goto " + trueLabel, 0);
                                 break;
        }
        targetMethod.addInstruction(trueLabel + ":", 0);
        targetMethod.addInstruction("    iconst_1", 1);
        targetMethod.addInstruction("    goto " + endLabel, 0);
        targetMethod.addInstruction(falseLabel + ":", 0);
        targetMethod.addInstruction("    iconst_0", 1);
        targetMethod.addInstruction(endLabel + ":", 0);
    }

}
