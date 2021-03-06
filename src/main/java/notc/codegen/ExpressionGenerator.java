package notc.codegen;

import notc.antlrgen.NotCBaseVisitor;
import notc.antlrgen.NotCParser;
import notc.antlrgen.NotCParser.Type;
import notc.antlrgen.NotCParser.ExpressionContext;
import notc.antlrgen.NotCParser.NegationExpressionContext;
import notc.antlrgen.NotCParser.FalseLiteralExpressionContext;
import notc.antlrgen.NotCParser.TrueLiteralExpressionContext;
import notc.antlrgen.NotCParser.DoubleLiteralExpressionContext;
import notc.antlrgen.NotCParser.IntLiteralExpressionContext;
import notc.antlrgen.NotCParser.StringLiteralExpressionContext;
import notc.antlrgen.NotCParser.VariableExpressionContext;
import notc.antlrgen.NotCParser.FunctionCallExpressionContext;
import notc.antlrgen.NotCParser.IncrementExpressionContext;
import notc.antlrgen.NotCParser.DecrementExpressionContext;
import notc.antlrgen.NotCParser.ArithmeticExpressionContext;
import notc.antlrgen.NotCParser.ComparisonExpressionContext;
import notc.antlrgen.NotCParser.BinaryBooleanExpressionContext;
import notc.antlrgen.NotCParser.AssignmentExpressionContext;
import notc.antlrgen.NotCParser.FunctionHeaderContext;
import notc.antlrgen.NotCParser.VariableDeclarationContext;
import notc.semantics.SymbolTable;

import org.antlr.v4.runtime.Token;

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

    // Common entry point. Generates expr and performs
    // any necessary conversion of the type it is generated as.
    // Returns its runtime type.
    Type generate(ExpressionContext expr) {
        if (expr == null)
            return Type.VOID;

        expr.accept(this);

        if (expr.runtimeConversion == null)
            return expr.type;

        convertTopOfStack(expr.type, expr.runtimeConversion);
        return expr.runtimeConversion;
    }

    private void convertTopOfStack(Type from, Type to) {
        if (to.isDouble())
            targetMethod.emit(Opcode.I2D); // Widens
        else if (from.isDouble() && to.isInt())
            targetMethod.emit(Opcode.D2I); // Truncates
        else if (!to.isBool())
            return; // bools used as ints
        else if (from.isInt())
            intToBool();
        else
            doubleToBool();
    }

    // nonzero double -> 1,  0.0 -> 0
    private void doubleToBool() {
        targetMethod.emit(Opcode.LDC2_W, "0.0");
        targetMethod.emit(Opcode.DCMPG);
        intToBool();
    }

    // nonzero int -> 1,  0 -> 0
    private void intToBool() {
        // (value | -value) >> 31
        targetMethod.emit(Opcode.DUP);
        targetMethod.emit(Opcode.INEG);
        targetMethod.emit(Opcode.IOR);
        targetMethod.emit(Opcode.LDC, "31");
        targetMethod.emit(Opcode.IUSHR);
    }

    @Override
    public Void visitNegationExpression(NegationExpressionContext negation) {
        Type t = generate(negation.opnd);
        Opcode negationOp = t.isDouble() ? Opcode.DNEG : Opcode.INEG;
        targetMethod.emit(negationOp);
        return null;
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
        funCallExpr.args.forEach(this::generate); // Puts arguments on stack
        FunctionHeaderContext callee = symTab.lookupFunction(funCallExpr.id);
        targetMethod.emitCall(callee);
        return null;
    }

    @Override
    public Void visitAssignmentExpression(AssignmentExpressionContext assExpr) {
        Type exprType = generate(assExpr.rhs);
        // Stored value is value of expression and is left on stack
        Opcode dup = exprType.isDouble() ? Opcode.DUP2 : Opcode.DUP;
        targetMethod.emit(dup);
        VariableDeclarationContext varDecl = symTab.lookupVariable(assExpr.varId);
        targetMethod.emitStore(varDecl);
        return null;
    }

    // <, > <=, >=, ==, !=
    @Override
    public Void visitComparisonExpression(ComparisonExpressionContext compExpr) {
        Type t1 = generate(compExpr.opnd1);
        Type t2 = generate(compExpr.opnd2);
        // The type is unknown at parse time
        if (t1.isDouble() || t2.isDouble())
            generateDoubleComparison(compExpr);
        else
            generateIntComparison(compExpr);
        return null;
    }

    // Simply uses "if_icmp{lt|gt|ge|le|eq|ne}"
    private void generateIntComparison(ComparisonExpressionContext compExpr) {
        String trueLabel = targetMethod.newLabel();
        String endLabel = targetMethod.newLabel();
        Opcode icmp = icmpByToken.get(compExpr.op.getType());
        targetMethod.emit(icmp, trueLabel);
        targetMethod.emit(Opcode.ICONST_0);
        targetMethod.emit(Opcode.GOTO, endLabel);
        targetMethod.insertLabel(trueLabel);
        targetMethod.emit(Opcode.ICONST_1);
        targetMethod.insertLabel(endLabel);
    }

    private final Map<Integer,Opcode> icmpByToken = Map.of(
        NotCParser.LT, Opcode.IF_ICMPLT,
        NotCParser.GT, Opcode.IF_ICMPGT,
        NotCParser.GE, Opcode.IF_ICMPGE,
        NotCParser.LE, Opcode.IF_ICMPLE,
        NotCParser.EQ, Opcode.IF_ICMPEQ,
        NotCParser.NE, Opcode.IF_ICMPNE
    );

    // "dcmpg" is trickier
    private void generateDoubleComparison(ComparisonExpressionContext compExpr) {
        String trueLabel = targetMethod.newLabel();
        String endLabel = targetMethod.newLabel();
        targetMethod.emit(Opcode.DCMPG);
        switch (compExpr.op.getType()) {
            case NotCParser.LT: // a < b -> TOS = -1
                                targetMethod.emit(Opcode.ICONST_M1);
                                targetMethod.emit(Opcode.IF_ICMPEQ, trueLabel);
                                break;
            case NotCParser.GT: // a > b -> TOS = 1
                                targetMethod.emit(Opcode.ICONST_1);
                                targetMethod.emit(Opcode.IF_ICMPEQ, trueLabel);
                                break;
            case NotCParser.GE: // a >= b -> TOS != -1
                                targetMethod.emit(Opcode.ICONST_M1);
                                targetMethod.emit(Opcode.IF_ICMPNE, trueLabel);
                                break;
            case NotCParser.LE: // a <= b -> TOS != 1
                                targetMethod.emit(Opcode.ICONST_1);
                                targetMethod.emit(Opcode.IF_ICMPNE, trueLabel);
                                break;
            case NotCParser.EQ: // a = b -> TOS = 0
                                targetMethod.emit(Opcode.ICONST_0);
                                targetMethod.emit(Opcode.IF_ICMPEQ, trueLabel);
                                break;
            case NotCParser.NE: // a != b -> TOS != 0
                                targetMethod.emit(Opcode.ICONST_0);
                                targetMethod.emit(Opcode.IF_ICMPNE, trueLabel);
                                break;
            default: throw new IllegalArgumentException("Should be unreachable " + compExpr.op);
        }
        targetMethod.emit(Opcode.ICONST_0);
        targetMethod.emit(Opcode.GOTO, endLabel);
        targetMethod.insertLabel(trueLabel);
        targetMethod.emit(Opcode.ICONST_1);
        targetMethod.insertLabel(endLabel);
    }

    // &&, ||
    @Override
    public Void visitBinaryBooleanExpression(BinaryBooleanExpressionContext binBoolExpr) {
        String trueLabel = targetMethod.newLabel();
        String falseLabel = targetMethod.newLabel();
        String endLabel = targetMethod.newLabel();
        // Makes generated code short-circuit the evaluation when possible
        generate(binBoolExpr.opnd1);
        if (binBoolExpr.op.getType() == NotCParser.AND)
            targetMethod.emit(Opcode.IFEQ, falseLabel);
        else
            targetMethod.emit(Opcode.IFNE, trueLabel);
        generate(binBoolExpr.opnd2);
        targetMethod.emit(Opcode.IFNE, trueLabel);

        targetMethod.insertLabel(falseLabel);
        targetMethod.emit(Opcode.ICONST_0);
        targetMethod.emit(Opcode.GOTO, endLabel);
        targetMethod.insertLabel(trueLabel);
        targetMethod.emit(Opcode.ICONST_1);
        targetMethod.insertLabel(endLabel);
        return null;
    }

    // ++
    @Override
    public Void visitIncrementExpression(IncrementExpressionContext incrExpr) {
        Opcode arithmOp = incrExpr.type.isDouble() ? Opcode.DADD : Opcode.IADD;
        generateIncrementDecrement(incrExpr.varId, arithmOp, incrExpr.preOp != null);
        return null;
    }

    // --
    @Override
    public Void visitDecrementExpression(DecrementExpressionContext decrExpr) {
        Opcode arithmOp = decrExpr.type.isDouble() ? Opcode.DSUB : Opcode.ISUB;
        generateIncrementDecrement(decrExpr.varId, arithmOp, decrExpr.preOp != null);
        return null;
    }

    private void generateIncrementDecrement(Token varId, Opcode arithmOp, boolean pre) {
        VariableDeclarationContext varDecl = symTab.lookupVariable(varId);
        Opcode dup;
        Opcode const1;
        if (varDecl.type.isDouble()) {
            dup = Opcode.DUP2;
            const1 = Opcode.DCONST_1;
        } else {
            dup = Opcode.DUP;
            const1 = Opcode.ICONST_1;
        }

        targetMethod.emitLoad(varDecl);
        if (!pre)
            targetMethod.emit(dup); // Leaves old value on stack
        targetMethod.emit(const1);
        targetMethod.emit(arithmOp);
        if (pre)
            targetMethod.emit(dup); // Leaves new value on stack
        targetMethod.emitStore(varDecl);
    }

    // +, -, *, /, %
    @Override
    public Void visitArithmeticExpression(ArithmeticExpressionContext arithmExpr) {
        generate(arithmExpr.opnd1);
        generate(arithmExpr.opnd2);
        Map<Integer,Opcode> lookupTable = arithmExpr.type.isDouble() ? doubleArithmetic
                                                                     : intArithmetic;
        Opcode arithmOp = lookupTable.get(arithmExpr.op.getType());
        targetMethod.emit(arithmOp);
        return null;
    }

    // Ugly but resolving operators with separate parser rules messes up their associativity

    private final Map<Integer,Opcode> intArithmetic = Map.of(
        NotCParser.ADD, Opcode.IADD,
        NotCParser.SUB, Opcode.ISUB,
        NotCParser.MUL, Opcode.IMUL,
        NotCParser.DIV, Opcode.IDIV,
        NotCParser.REM, Opcode.IREM
    );

    private final Map<Integer,Opcode> doubleArithmetic = Map.of(
        NotCParser.ADD, Opcode.DADD,
        NotCParser.SUB, Opcode.DSUB,
        NotCParser.MUL, Opcode.DMUL,
        NotCParser.DIV, Opcode.DDIV,
        NotCParser.REM, Opcode.DREM
    );

}
