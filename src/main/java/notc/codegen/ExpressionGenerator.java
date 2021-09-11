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
import notc.antlrgen.NotCParser.FunctionHeaderContext;
import notc.semantics.SymbolTable;

import org.antlr.v4.runtime.Token;
import org.apache.commons.lang3.ObjectUtils;

class ExpressionGenerator extends NotCBaseVisitor<Void> {
    private final SymbolTable symTab;
    private JvmMethod targetMethod;

    ExpressionGenerator(SymbolTable symTab) {
        this.symTab = symTab;
    }

    void setTarget(JvmMethod targetMethod) {
        this.targetMethod = targetMethod;
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
        int varAddr = targetMethod.addressOf(symTab.lookupVariable(varExpr.varId));
        String loadInstr = varExpr.type.prefix() + "load ";
        targetMethod.addInstruction(loadInstr + varAddr, varExpr.type.size());
        return null;
    }

    // Function calls
    @Override
    public Void visitFunctionCallExpression(FunctionCallExpressionContext funCallExpr) {
        // Put all arguments on stack and calculate their size
        int argsStackSize = 0;
        for (ExpressionContext arg : funCallExpr.args)
            argsStackSize += generate(arg).size();

        FunctionHeaderContext header = symTab.lookupFunction(funCallExpr.id);
        String descriptor = header.descriptor;
        String invocation = "invokestatic Method " + funCallExpr.id.getText() + ":" + descriptor;
        int returnStackSize = funCallExpr.type.size();
                                                // Arguments are popped, return value is pushed
        targetMethod.addInstruction(invocation, returnStackSize - argsStackSize);
        return null;
    }


    // +, -, *, /, %
    @Override
    public Void visitArithmeticExpression(ArithmeticExpressionContext arithmExpr) {
        // Generate operands
        generate(arithmExpr.opnd1);
        generate(arithmExpr.opnd2);
        int stackChange = -arithmExpr.type.size(); // Stack: |type| |type| -> |type|
        char typePrefix  = arithmExpr.type.prefix();
        String operation = operationByToken(arithmExpr.op);
        targetMethod.addInstruction(typePrefix + operation, stackChange);
        return null;
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
        String operation = operationByToken(compExpr.op);
        targetMethod.addInstruction("if_icmp" + operation + " " + trueLabel, -2);
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
            default: throw new IllegalArgumentException("Should be unreachable. Token: " + compExpr.op);
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
        String operation = 'i' + operationByToken(andOrExpr.op);
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
        int varAddr = targetMethod.addressOf(symTab.lookupVariable(assExpr.varId));
        String storeInstr = assExpr.type.prefix() + "store ";
        String dupInstr = formatDup(assExpr.type);
        int varSize = assExpr.type.size();
        // Stored value is value of expression and is left on stack
        targetMethod.addInstruction(dupInstr, varSize);
        targetMethod.addInstruction(storeInstr + varAddr, -varSize);
        return null;
    }

    @Override
    public Void visitIncrementDecrementExpression(IncrementDecrementExpressionContext incrDecrExpr) {
        Type varType = incrDecrExpr.type;
        char typePrefix = varType.prefix();
        String dupInstr = formatDup(varType);
        int varSize = varType.size();
        Token opTok = ObjectUtils.firstNonNull(incrDecrExpr.preOp, incrDecrExpr.postOp);
        String operation = operationByToken(opTok);
        int varAddr = targetMethod.addressOf(symTab.lookupVariable(incrDecrExpr.varId));
        targetMethod.addInstruction(varType.prefix() + "load " + varAddr, varSize);
        if (incrDecrExpr.postOp != null)
            targetMethod.addInstruction(dupInstr, varSize); // Leave previous value on stack
        targetMethod.addInstruction(typePrefix + "const_1", varSize);
        targetMethod.addInstruction(typePrefix + operation, -varSize);
        if (incrDecrExpr.preOp != null)
            targetMethod.addInstruction(dupInstr, varSize); // Leave new value on stack
        targetMethod.addInstruction(varType.prefix() + "store " + varAddr, -varSize);
        return null;
    }

    private String formatDup(Type t) {
        return (t.isDouble()) ? "dup2" : "dup";
    }

    private String operationByToken(Token opTok) {
        switch (opTok.getType()) {
            case NotCParser.MUL:  return "mul";
            case NotCParser.DIV:  return "div";
            case NotCParser.REM:  return "rem";
            case NotCParser.INCR:
            case NotCParser.ADD:  return "add";
            case NotCParser.DECR:
            case NotCParser.SUB:  return "sub";
            case NotCParser.LT:   return "lt";
            case NotCParser.GT:   return "gt";
            case NotCParser.GE:   return "ge";
            case NotCParser.LE:   return "le";
            case NotCParser.EQ:   return "eq";
            case NotCParser.NE:   return "ne";
            case NotCParser.AND:  return "and";
            case NotCParser.OR:   return "or";
            default: throw new IllegalArgumentException("Should be unreachable. Token: " + opTok);
        }
    }

}
