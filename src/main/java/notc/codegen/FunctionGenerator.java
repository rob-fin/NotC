package notc.codegen;

import notc.antlrgen.NotCBaseVisitor;
import notc.antlrgen.NotCParser.Type;
import notc.antlrgen.NotCParser.FunctionDefinitionContext;
import notc.antlrgen.NotCParser.StatementContext;
import notc.antlrgen.NotCParser.DeclarationStatementContext;
import notc.antlrgen.NotCParser.InitializationStatementContext;
import notc.antlrgen.NotCParser.ExpressionStatementContext;
import notc.antlrgen.NotCParser.BlockStatementContext;
import notc.antlrgen.NotCParser.ForStatementContext;
import notc.antlrgen.NotCParser.WhileStatementContext;
import notc.antlrgen.NotCParser.IfStatementContext;
import notc.antlrgen.NotCParser.IfElseStatementContext;
import notc.antlrgen.NotCParser.ReturnStatementContext;

import java.util.Map;

class FunctionGenerator extends NotCBaseVisitor<Void> {
    private final ExpressionGenerator exprGen;
    private JvmMethod targetMethod;

    FunctionGenerator(ExpressionGenerator exprGen) {
        this.exprGen = exprGen;
    }

    // Entry point. Sets up target and generates the statements.
    JvmMethod generate(FunctionDefinitionContext funDef) {
        targetMethod = new JvmMethod(funDef.header);
        exprGen.setTarget(targetMethod);
        for (StatementContext stm : funDef.body)
            stm.accept(this);
        // Avoids falling off the end of the code
        if (funDef.header.returnType.isVoid())
            targetMethod.emit(Opcode.RETURN);
        return targetMethod;
    }

    // "type id1, id2..."
    @Override
    public Void visitDeclarationStatement(DeclarationStatementContext declStm) {
        targetMethod.reserveVarMemory(declStm.varDecls);
        return null;
    }

    // "type id = expr"
    @Override
    public Void visitInitializationStatement(InitializationStatementContext initStm) {
        targetMethod.reserveVarMemory(initStm.varDecl);
        exprGen.generate(initStm.expr);
        targetMethod.emitStore(initStm.varDecl);
        return null;
    }

    // Expression used as statement
    @Override
    public Void visitExpressionStatement(ExpressionStatementContext exprStm) {
        Type exprType = exprGen.generate(exprStm.expr);
        // Value is not used
        pop(exprType.size());
        return null;
    }

    @Override
    public Void visitForStatement(ForStatementContext forStm) {
        Type initType = exprGen.generate(forStm.initExpr);
        pop(initType.size());
        String testLabel = targetMethod.newLabel();
        String endLabel = targetMethod.newLabel();
        targetMethod.insertLabel(testLabel);
        Type condType = exprGen.generate(forStm.conditionExpr);
        if (condType.isVoid()) // for (;;)
            targetMethod.emit(Opcode.ICONST_1);
        targetMethod.emit(Opcode.IFEQ, endLabel); // "if TOS = 0"
        forStm.body.accept(this);
        Type advType = exprGen.generate(forStm.advanceExpr);
        pop(advType.size());
        targetMethod.emit(Opcode.GOTO, testLabel);
        targetMethod.insertLabel(endLabel);
        return null;
    }

    private void pop(int stackEntryCount) {
        if (stackEntryCount == 0)
            return;
        Opcode popOp = (stackEntryCount == 1) ? Opcode.POP : Opcode.POP2;
        targetMethod.emit(popOp);
    }

    @Override
    public Void visitWhileStatement(WhileStatementContext whileStm) {
        String testLabel = targetMethod.newLabel();
        String endLabel = targetMethod.newLabel();
        targetMethod.insertLabel(testLabel);
        exprGen.generate(whileStm.conditionExpr);
        targetMethod.emit(Opcode.IFEQ, endLabel);
        whileStm.loopedStm.accept(this);
        targetMethod.emit(Opcode.GOTO, testLabel);
        targetMethod.insertLabel(endLabel);
        return null;
    }

    @Override
    public Void visitIfStatement(IfStatementContext ifStm) {
        String trueLabel = targetMethod.newLabel();
        String endLabel = targetMethod.newLabel();
        exprGen.generate(ifStm.conditionExpr);
        targetMethod.emit(Opcode.IFNE, trueLabel); // "if TOS != 0"
        targetMethod.emit(Opcode.GOTO, endLabel);
        targetMethod.insertLabel(trueLabel);
        ifStm.consequentStm.accept(this);
        targetMethod.insertLabel(endLabel);
        return null;
    }

    @Override
    public Void visitIfElseStatement(IfElseStatementContext ifElseStm) {
        String falseLabel = targetMethod.newLabel();
        String trueLabel = targetMethod.newLabel();
        exprGen.generate(ifElseStm.conditionExpr);
        targetMethod.emit(Opcode.IFEQ, falseLabel);
        ifElseStm.consequentStm.accept(this);
        targetMethod.emit(Opcode.GOTO, trueLabel);
        targetMethod.insertLabel(falseLabel);
        ifElseStm.altStm.accept(this);
        targetMethod.insertLabel(trueLabel);
        return null;
    }

    @Override
    public Void visitBlockStatement(BlockStatementContext block) {
        for (StatementContext stm : block.statements)
            stm.accept(this);
        return null;
    }

    @Override
    public Void visitReturnStatement(ReturnStatementContext returnStm) {
        Type returnedType = exprGen.generate(returnStm.expr);
        Opcode returnOp = returnOpByType.get(returnedType);
        targetMethod.emit(returnOp);
        return null;
    }

    private final Map<Type,Opcode> returnOpByType = Map.of(
        Type.BOOL,   Opcode.IRETURN,
        Type.INT,    Opcode.IRETURN,
        Type.STRING, Opcode.ARETURN,
        Type.DOUBLE, Opcode.DRETURN,
        Type.VOID,   Opcode.RETURN
    );

}
