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
import notc.antlrgen.NotCParser.VariableDeclarationContext;
import notc.semantics.SymbolTable;

import org.antlr.v4.runtime.Token;
import org.apache.commons.lang3.ObjectUtils;

import java.util.Map;

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
            targetMethod.emit(Opcode.I2D); // Widens
        else if (from.isDouble() && to.isInt())
            targetMethod.emit(Opcode.D2I); // Truncates
        else if (!to.isBool())
            ; // Nothing to do
        else if (from.isInt())
            intToBool();
        else
            doubleToBool();

        return to;
    }

    // Converts a double at the top of the stack to 0 or 1
    private void doubleToBool() {
        // 0.0 -> 0,  nonzero double -> nonzero int
        targetMethod.emit(Opcode.LDC2_W, "0.0");
        targetMethod.emit(Opcode.DCMPG);

        intToBool();
    }

    // Converts a nonzero int at the top of the stack to 1
    private void intToBool() {
        // (value | -value) >> 31
        targetMethod.emit(Opcode.DUP);
        targetMethod.emit(Opcode.INEG);
        targetMethod.emit(Opcode.IOR);
        targetMethod.emit(Opcode.LDC, "31");
        targetMethod.emit(Opcode.IUSHR);
    }

    // Literals: operations to put constant values on the stack
    @Override
    public Void visitFalseLiteralExpression(FalseLiteralExpressionContext falseLiteralExpr) {
        targetMethod.emit(Opcode.ICONST_0);
        return null;
    }

    @Override
    public Void visitTrueLiteralExpression(TrueLiteralExpressionContext trueLiteralExpr) {
        targetMethod.emit(Opcode.ICONST_1);
        return null;
    }

    @Override
    public Void visitDoubleLiteralExpression(DoubleLiteralExpressionContext doubleLiteralExpr) {
        String srcText = doubleLiteralExpr.value.getText();
        targetMethod.emit(Opcode.LDC2_W, srcText);
        return null;
    }

    @Override
    public Void visitIntLiteralExpression(IntLiteralExpressionContext intLitExpr) {
        String srcText = intLitExpr.value.getText();
        targetMethod.emit(Opcode.LDC, srcText);
        return null;
    }

    @Override
    public Void visitStringLiteralExpression(StringLiteralExpressionContext strLiteralExpr) {
        String srcText = strLiteralExpr.value.getText();
        targetMethod.emit(Opcode.LDC, srcText);
        return null;
    }

    // Loads the variable referenced by the expression
    @Override
    public Void visitVariableExpression(VariableExpressionContext varExpr) {
        VariableDeclarationContext varDecl = symTab.lookupVariable(varExpr.varId);
        targetMethod.emitLoad(varDecl);
        return null;
    }

    @Override
    public Void visitFunctionCallExpression(FunctionCallExpressionContext funCallExpr) {
        // Put all arguments on stack and calculate their size
        int argsStackSize = 0;
        for (ExpressionContext arg : funCallExpr.args)
            argsStackSize += generate(arg).size();
        FunctionHeaderContext header = symTab.lookupFunction(funCallExpr.id);
        String descriptor = header.descriptor;
        String invocationOpnd = "Method " + funCallExpr.id.getText() + ":" + descriptor;
        int returnStackSize = funCallExpr.type.size();
        // Arguments are popped, return value is pushed
        int stackChange = returnStackSize - argsStackSize;
        targetMethod.emit(Opcode.INVOKESTATIC, stackChange, invocationOpnd);
        return null;
    }


    // +, -, *, /, %
    @Override
    public Void visitArithmeticExpression(ArithmeticExpressionContext arithmExpr) {
        // Generate operands
        generate(arithmExpr.opnd1);
        generate(arithmExpr.opnd2);
        Opcode op = getArithmeticOp(arithmExpr.op, arithmExpr.type);
        targetMethod.emit(op);
        return null;
    }
// TODO: maybe break up arithmetic parser rules to avoid this
    private Opcode getArithmeticOp(Token tok, Type t) {
        switch (tok.getType()) {
            case NotCParser.ADD: return t.isInt() ? Opcode.IADD : Opcode.DADD;
            case NotCParser.SUB: return t.isInt() ? Opcode.ISUB : Opcode.DSUB;
            case NotCParser.MUL: return t.isInt() ? Opcode.IMUL : Opcode.DMUL;
            case NotCParser.DIV: return t.isInt() ? Opcode.IDIV : Opcode.DDIV;
            case NotCParser.REM: return t.isInt() ? Opcode.IREM : Opcode.DREM;
            default: throw new IllegalArgumentException("Should be unreachable. Token: " + tok);
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

    // Simply uses "if_icmp{lt|gt|ge|le|eq|ne}"
    private void generateIntComparison(ComparisonExpressionContext compExpr) {
        String trueLabel = targetMethod.newLabel();
        String endLabel = targetMethod.newLabel();
        Opcode compJmp = comparisonByToken(compExpr.op);
        targetMethod.emit(compJmp, trueLabel);
        targetMethod.emit(Opcode.ICONST_0);
        targetMethod.emit(Opcode.GOTO, endLabel);
        targetMethod.insertLabel(trueLabel);
        targetMethod.emit(Opcode.ICONST_1);
        targetMethod.insertLabel(endLabel);
    }

    private Opcode comparisonByToken(Token tok) {
        switch (tok.getType()) {
            case NotCParser.LT: return Opcode.IF_ICMPLT;
            case NotCParser.GT: return Opcode.IF_ICMPGT;
            case NotCParser.GE: return Opcode.IF_ICMPGE;
            case NotCParser.LE: return Opcode.IF_ICMPLE;
            case NotCParser.EQ: return Opcode.IF_ICMPEQ;
            case NotCParser.NE: return Opcode.IF_ICMPNE;
            default: throw new IllegalArgumentException("Should be unreachable. Token: " + tok);
        }
    }

    // "dcmpg" is trickier
    private void generateDoubleComparison(ComparisonExpressionContext compExpr) {
        String trueLabel = targetMethod.newLabel();
        String falseLabel = targetMethod.newLabel();
        String endLabel = targetMethod.newLabel();
        targetMethod.emit(Opcode.DCMPG);
        switch (compExpr.op.getType()) {
            case NotCParser.LT: // a < b -> TOS = -1
                                targetMethod.emit(Opcode.ICONST_M1);
                                targetMethod.emit(Opcode.IF_ICMPEQ, trueLabel);
                                targetMethod.emit(Opcode.GOTO, falseLabel);
                                break;
            case NotCParser.GT: // a > b -> TOS = 1
                                targetMethod.emit(Opcode.ICONST_1);
                                targetMethod.emit(Opcode.IF_ICMPEQ, trueLabel);
                                targetMethod.emit(Opcode.GOTO, falseLabel);
                                break;
            case NotCParser.GE: // a >= b -> TOS != -1
                                targetMethod.emit(Opcode.ICONST_M1);
                                targetMethod.emit(Opcode.IF_ICMPEQ, falseLabel);
                                targetMethod.emit(Opcode.GOTO, trueLabel);
                                break;
            case NotCParser.LE: // a <= b -> TOS != 1
                                targetMethod.emit(Opcode.ICONST_1);
                                targetMethod.emit(Opcode.IF_ICMPEQ, falseLabel);
                                targetMethod.emit(Opcode.GOTO, trueLabel);
                                break;
            case NotCParser.EQ: // a = b -> TOS = 0
                                targetMethod.emit(Opcode.ICONST_0);
                                targetMethod.emit(Opcode.IF_ICMPEQ, trueLabel);
                                targetMethod.emit(Opcode.GOTO, falseLabel);
                                break;
            case NotCParser.NE: // a != b -> TOS != 0
                                targetMethod.emit(Opcode.ICONST_0);
                                targetMethod.emit(Opcode.IF_ICMPEQ, falseLabel);
                                targetMethod.emit(Opcode.GOTO, trueLabel);
                                break;
            default: throw new IllegalArgumentException("Should be unreachable. Token: " +
                                                        compExpr.op);
        }
        targetMethod.insertLabel(trueLabel);
        targetMethod.emit(Opcode.ICONST_1);
        targetMethod.emit(Opcode.GOTO, endLabel);
        targetMethod.insertLabel(falseLabel);
        targetMethod.emit(Opcode.ICONST_0);
        targetMethod.insertLabel(endLabel);
    }

    // Use bitwise and/or on operands and check if result is 0
    @Override
    public Void visitAndOrExpression(AndOrExpressionContext andOrExpr) {
        Opcode op;
        if (andOrExpr.op.getType() == NotCParser.AND)
            op = Opcode.IAND;
        else
            op = Opcode.IOR;
        String trueLabel = targetMethod.newLabel();
        String endLabel = targetMethod.newLabel();
        // Put operands on stack
        generate(andOrExpr.opnd1);
        generate(andOrExpr.opnd2);
        targetMethod.emit(op);
        targetMethod.emit(Opcode.IFNE, trueLabel);
        targetMethod.emit(Opcode.ICONST_0);
        targetMethod.emit(Opcode.GOTO, endLabel);
        targetMethod.insertLabel(trueLabel);
        targetMethod.emit(Opcode.ICONST_1);
        targetMethod.insertLabel(endLabel);
        return null;
    }

    // Assignments
    @Override
    public Void visitAssignmentExpression(AssignmentExpressionContext assExpr) {
        Type t = generate(assExpr.rhs);
        // Stored value is value of expression and is left on stack
        Opcode dup = t.isDouble() ? Opcode.DUP2 : Opcode.DUP;
        targetMethod.emit(dup);
        VariableDeclarationContext varDecl = symTab.lookupVariable(assExpr.varId);
        targetMethod.emitStore(varDecl);
        return null;
    }

    @Override
    public Void visitIncrementDecrementExpression(IncrementDecrementExpressionContext incrDecrExpr) {
        VariableDeclarationContext varDecl = symTab.lookupVariable(incrDecrExpr.varId);
        Opcode dupOp = varDecl.type.isDouble() ? Opcode.DUP2 : Opcode.DUP;
        Token opTok = ObjectUtils.firstNonNull(incrDecrExpr.preOp, incrDecrExpr.postOp);
        Opcode arithmOp;
        if (opTok.getType() == NotCParser.INCR)
            arithmOp = varDecl.type.isInt() ? Opcode.IADD : Opcode.DADD;
        else
            arithmOp = varDecl.type.isInt() ? Opcode.ISUB : Opcode.DSUB;
        targetMethod.emitLoad(varDecl);
        if (incrDecrExpr.postOp != null)
            targetMethod.emit(dupOp); // Leave previous value on stack
        Opcode const1 = varDecl.type.isInt() ? Opcode.ICONST_1 : Opcode.DCONST_1;
        targetMethod.emit(const1);
        targetMethod.emit(arithmOp);
        if (incrDecrExpr.preOp != null)
            targetMethod.emit(dupOp); // Leave new value on stack
        targetMethod.emitStore(varDecl);
        return null;
    }

}
