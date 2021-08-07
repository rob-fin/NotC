package notc.codegen;

import notc.antlrgen.NotCBaseVisitor;
import notc.antlrgen.NotCParser;
import notc.antlrgen.NotCParser.Type;
import notc.antlrgen.NotCParser.ExpressionContext;
import notc.antlrgen.NotCParser.FalseLiteralExpressionContext;
import notc.antlrgen.NotCParser.TrueLiteralExpressionContext;
import notc.antlrgen.NotCParser.DoubleLiteralExpressionContext;
import notc.antlrgen.NotCParser.IntLiteralExpressionContext;
import notc.antlrgen.NotCParser.StringLiteralExpressionContext;
import notc.antlrgen.NotCParser.VariableExpressionContext;
import notc.antlrgen.NotCParser.FunctionCallExpressionContext;
import notc.antlrgen.NotCParser.IncrementDecrementExpressionContext;
import notc.antlrgen.NotCParser.ArithmeticExpressionContext;
import notc.antlrgen.NotCParser.ComparisonExpressionContext;
import notc.antlrgen.NotCParser.AndOrExpressionContext;
import notc.antlrgen.NotCParser.AssignmentExpressionContext;

import org.antlr.v4.runtime.Token;
import org.apache.commons.lang3.ObjectUtils;

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

    // Common entry point.
    // Performs any necessary conversion of the type the expression evaluates to.
    // Returns the runtime type of the expression.
    Type generate(ExpressionContext expr) {
        if (expr == null)
            return Type.VOID;
        expr.accept(this);
        if (expr.runtimeConversion == null)
            return expr.type;

        Type from = expr.type;
        Type to = expr.runtimeConversion;
        if (to.isDouble())
            targetMethod.addInstruction("i2d", 1); // Widens
        else if (from.isDouble() && to.isInt())
            targetMethod.addInstruction("d2i", -1); // Truncates
        else if (!to.isBool())
            ; // Nothing else to do
        else if (from.isInt())
            intToBool();
        else
            doubleToBool();

        return to;
    }
    // Converts a double at the top of the stack to 0 or 1
    private void doubleToBool() {
        // 0.0 -> 0,  nonzero double -> nonzero int
        targetMethod.addInstruction("ldc2_w 0.0", 2);
        targetMethod.addInstruction("dcmpg", -1);

        intToBool();
    }

    // Converts a nonzero int at the top of the stack to 1
    private void intToBool() {
        // (value | -value) >> 31
        targetMethod.addInstruction("dup", 1);
        targetMethod.addInstruction("ineg", 0);
        targetMethod.addInstruction("ior", -1);
        targetMethod.addInstruction("ldc 31", 1);
        targetMethod.addInstruction("iushr", -1);
    }

    // Literals: instructions to put constant values on the stack
    @Override
    public Void visitFalseLiteralExpression(FalseLiteralExpressionContext falseLiteralExpr) {
        targetMethod.addInstruction("ldc 0", 1);
        return null;
    }

    @Override
    public Void visitTrueLiteralExpression(TrueLiteralExpressionContext trueLiteralExpr) {
        targetMethod.addInstruction("ldc 1", 1);
        return null;
    }

    @Override
    public Void visitDoubleLiteralExpression(DoubleLiteralExpressionContext doubleLiteralExpr) {
        String srcText = doubleLiteralExpr.value.getText();
        targetMethod.addInstruction("ldc2_w " + srcText, 2);
        return null;
    }

    @Override
    public Void visitIntLiteralExpression(IntLiteralExpressionContext intLitExpr) {
        String srcText = intLitExpr.value.getText();
        targetMethod.addInstruction("ldc " + srcText, 1);
        return null;
    }

    @Override
    public Void visitStringLiteralExpression(StringLiteralExpressionContext strLiteralExpr) {
        String srcText = strLiteralExpr.value.getText();
        targetMethod.addInstruction("ldc " + srcText, 1);
        return null;
    }

    // Variable expression: look up its address and load it
    @Override
    public Void visitVariableExpression(VariableExpressionContext varExpr) {
        Type varType = varExpr.type;
        int varAddr = targetMethod.lookupVar(varExpr.varId);
        if (varType.isDouble())
            targetMethod.addInstruction("dload " + varAddr, 2);
        else if (varType.isString())
            targetMethod.addInstruction("aload " + varAddr, 1);
        else
            targetMethod.addInstruction("iload " + varAddr, 1);
        return null;
    }

    // Function calls
    @Override
    public Void visitFunctionCallExpression(FunctionCallExpressionContext funCallExpr) {
        // Put all arguments on stack and calculate their size
        int argStackSize = 0;
        for (ExpressionContext arg : funCallExpr.args) {
            if (generate(arg).isDouble())
                argStackSize += 2;
            else
                argStackSize += 1; // int, bool, string
        }

        // Return value is left on stack, so calculate its size
        Type returnType = funCallExpr.type;
        int returnStackSize = 0;
        if (returnType.isDouble())
            returnStackSize = 2;
        else if (!returnType.isVoid())
            returnStackSize = 1;

        String invocation = "invokestatic " + methodSymTab.get(funCallExpr.id.getText());
                                                // Arguments are popped, return value is pushed
        targetMethod.addInstruction(invocation, returnStackSize - argStackSize);
        return null;
    }

    @Override
    public Void visitIncrementDecrementExpression(IncrementDecrementExpressionContext incrDecrExpr) {
        char typeSymbol;
        String dupInstr;
        int stackSpace;
        if (incrDecrExpr.type.isInt()) {
            typeSymbol = 'i';
            dupInstr = "dup";
            stackSpace = 1;
        } else {
            typeSymbol = 'd';
            dupInstr = "dup2";
            stackSpace = 2;
        }
        Token opTok = ObjectUtils.firstNonNull(incrDecrExpr.preOp, incrDecrExpr.postOp);
        String operation;
        if (opTok.getType() == NotCParser.INCR)
            operation = "add";
        else
            operation = "sub";
        int varAddr = targetMethod.lookupVar(incrDecrExpr.varId);
        targetMethod.addInstruction(typeSymbol + "load " + varAddr, stackSpace);
        if (incrDecrExpr.postOp != null)
            targetMethod.addInstruction(dupInstr, stackSpace); // Leave previous value on stack
        targetMethod.addInstruction(typeSymbol + "const_1", stackSpace);
        targetMethod.addInstruction(typeSymbol + operation, -stackSpace);
        if (incrDecrExpr.preOp != null)
            targetMethod.addInstruction(dupInstr, stackSpace); // Leave new value on stack
        targetMethod.addInstruction(typeSymbol + "store " + varAddr, -stackSpace);
        return null;
    }

    // +, -, *, /, %
    @Override
    public Void visitArithmeticExpression(ArithmeticExpressionContext arithmExpr) {
        // Generate operands
        generate(arithmExpr.opnd1);
        generate(arithmExpr.opnd2);
        String operation = operationByToken(arithmExpr.op);
        if (arithmExpr.type.isInt()) // stack: i i -> i
            targetMethod.addInstruction("i" + operation, -1);
        else // stack: d d -> d
            targetMethod.addInstruction("d" + operation, -2);
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
        generate(compExpr.opnd1);
        generate(compExpr.opnd2);
        if (compExpr.opnd1.type.isDouble() || compExpr.opnd2.type.isDouble())
            generateDoubleComparison(compExpr);
        else
            generateIntComparison(compExpr);
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
        targetMethod.addInstruction("if_icmp" + op + " " + trueLabel, -2);
        targetMethod.addInstruction("iconst_0", 1); // false
        targetMethod.addInstruction("goto " + endLabel, 0);
        targetMethod.addInstruction(trueLabel + ":", 0);
        targetMethod.addInstruction("iconst_1", 1); // true
        targetMethod.addInstruction(endLabel + ":", 0);
    }

    private void generateDoubleComparison(ComparisonExpressionContext compExpr) {
        String trueLabel = targetMethod.newLabel();
        String falseLabel = targetMethod.newLabel();
        String endLabel = targetMethod.newLabel();
        targetMethod.addInstruction("dcmpg", -3); // stack: d d -> i
        switch (compExpr.op.getType()) {
            case NotCParser.LT:  // a < b -> TOS = -1
                                 targetMethod.addInstruction("iconst_m1", 1);
                                 targetMethod.addInstruction("if_icmpeq " + trueLabel, -2);
                                 targetMethod.addInstruction("goto " + falseLabel, 0);
                                 break;
            case NotCParser.GT:  // a > b -> TOS = 1
                                 targetMethod.addInstruction("iconst_1", 1);
                                 targetMethod.addInstruction("if_icmpeq " + trueLabel, -2);
                                 targetMethod.addInstruction("goto " + falseLabel, 0);
                                 break;
            case NotCParser.GE:  // a >= b -> TOS != -1
                                 targetMethod.addInstruction("iconst_m1", 1);
                                 targetMethod.addInstruction("if_icmpeq " + falseLabel, -2);
                                 targetMethod.addInstruction("goto " + trueLabel, 0);
                                 break;
            case NotCParser.LE:  // a <= b -> TOS != 1
                                 targetMethod.addInstruction("iconst_1", 1);
                                 targetMethod.addInstruction("if_icmpeq " + falseLabel, -2);
                                 targetMethod.addInstruction("goto " + trueLabel, 0);
                                 break;
            case NotCParser.EQ:  // a = b -> TOS = 0
                                 targetMethod.addInstruction("iconst_0", 1);
                                 targetMethod.addInstruction("if_icmpeq " + trueLabel, -2);
                                 targetMethod.addInstruction("goto " + falseLabel, 0);
                                 break;
            case NotCParser.NE:  // a != b -> TOS != 0
                                 targetMethod.addInstruction("iconst_0", 1);
                                 targetMethod.addInstruction("if_icmpeq " + falseLabel, -2);
                                 targetMethod.addInstruction("goto " + trueLabel, 0);
                                 break;
        }
        targetMethod.addInstruction(trueLabel + ":", 0);
        targetMethod.addInstruction("iconst_1", 1);
        targetMethod.addInstruction("goto " + endLabel, 0);
        targetMethod.addInstruction(falseLabel + ":", 0);
        targetMethod.addInstruction("iconst_0", 1);
        targetMethod.addInstruction(endLabel + ":", 0);
    }

    // Use bitwise and/or on operands and check if result is 0
    @Override
    public Void visitAndOrExpression(AndOrExpressionContext andOrExpr) {
        String operation;
        if (andOrExpr.op.getType() == NotCParser.AND)
            operation = "iand";
        else
            operation = "ior";
        String falseLabel = targetMethod.newLabel();
        String endLabel = targetMethod.newLabel();
        // Put operands on stack
        generate(andOrExpr.opnd1);
        generate(andOrExpr.opnd2);
        targetMethod.addInstruction(operation, -1); // Stack: i i -> i
        targetMethod.addInstruction("ifeq " + falseLabel, -1);
        targetMethod.addInstruction("iconst_1", 1);
        targetMethod.addInstruction("goto " + endLabel, 0);
        targetMethod.addInstruction(falseLabel + ":", 0);
        targetMethod.addInstruction("iconst_0", 1);
        targetMethod.addInstruction(endLabel + ":", 0);
        return null;
    }

    // Assignments
    @Override
    public Void visitAssignmentExpression(AssignmentExpressionContext assExpr) {
        generate(assExpr.rhs); // Expression on the right of = goes on stack
        int varAddr = targetMethod.lookupVar(assExpr.varId);
        String storeInstr;
        String dupInstr;
        int stackSpace;
        if (assExpr.type.isDouble()) {
            storeInstr = "dstore ";
            dupInstr   = "dup2";
            stackSpace = 2;
        } else if (assExpr.type.isString()) {
            storeInstr = "astore ";
            dupInstr   = "dup";
            stackSpace = 1;
        } else { // ints, bools
            storeInstr = "istore ";
            dupInstr   = "dup";
            stackSpace = 1;
        }
        // Stored value is value of expression and is left on stack
        targetMethod.addInstruction(dupInstr, stackSpace);
        targetMethod.addInstruction(storeInstr + varAddr, -stackSpace);
        return null;
    }

}
