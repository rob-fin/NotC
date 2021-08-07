package notc.codegen;

import notc.antlrgen.NotCBaseVisitor;
import notc.antlrgen.NotCParser.Type;
import notc.antlrgen.NotCParser.StatementContext;
import notc.antlrgen.NotCParser.DeclarationStatementContext;
import notc.antlrgen.NotCParser.InitializationStatementContext;
import notc.antlrgen.NotCParser.ExpressionStatementContext;
import notc.antlrgen.NotCParser.BlockStatementContext;
import notc.antlrgen.NotCParser.WhileStatementContext;
import notc.antlrgen.NotCParser.IfStatementContext;
import notc.antlrgen.NotCParser.IfElseStatementContext;
import notc.antlrgen.NotCParser.ReturnStatementContext;

import org.antlr.v4.runtime.Token;

// Visitor that generates Jasmin instructions from statements.
// Most instructions generated here do something with what's
// put on the stack by the statements' constituent expressions.
class StatementGenerator extends NotCBaseVisitor<Void> {
    private ExpressionGenerator exprGen;
    private JvmMethod targetMethod; // Generated code goes here
    private static StatementGenerator instance = new StatementGenerator();
    private StatementGenerator() {}

    // Reuse same instance
    static StatementGenerator withTarget(JvmMethod method) {
        instance.targetMethod = method;
        instance.exprGen = ExpressionGenerator.withTarget(method);
        return instance;
    }

    @Override
    public Void visitDeclarationStatement(DeclarationStatementContext declStm) {
        Type declaredType = declStm.typeDeclaration.type;
        for (Token idTok : declStm.varIds)
            targetMethod.addVar(idTok, declaredType);
        return null;
    }

    @Override
    public Void visitInitializationStatement(InitializationStatementContext initStm) {
        // Get an address for the variable
        Type varType = initStm.typeDeclaration.type;
        targetMethod.addVar(initStm.varId, varType);
        // Generate its initializing expression
        exprGen.generate(initStm.expr);
        String storeInstr;
        int stackChange = 1;
        if (varType.isDouble()) {
            stackChange = 2;
            storeInstr = "dstore ";
        } else if (varType.isString()) {
            storeInstr = "astore ";
        } else { // ints, bools
            storeInstr = "istore ";
        }
        int varAddr = targetMethod.lookupVar(initStm.varId);
        targetMethod.addInstruction(storeInstr + varAddr, stackChange);
        return null;
    }

    // Expression used as statement
    @Override
    public Void visitExpressionStatement(ExpressionStatementContext exprStm) {
        Type exprType = exprGen.generate(exprStm.expr);
        // Value is not used and should be popped
        if (exprType.isVoid())
            return null; // Leaves nothing on the stack anyway
        else if (exprType.isDouble())
            targetMethod.addInstruction("pop2", -2);
        else // ints, bools, strings
            targetMethod.addInstruction("pop", -1);
        return null;
    }

    @Override
    public Void visitBlockStatement(BlockStatementContext block) {
        targetMethod.pushScope();
        for (StatementContext stm : block.statements)
            stm.accept(this);
        targetMethod.popScope();
        return null;
    }

    @Override
    public Void visitWhileStatement(WhileStatementContext _while) {
        String testLabel = targetMethod.newLabel();
        String endLabel = targetMethod.newLabel();
        targetMethod.addInstruction(testLabel + ":", 0);
        exprGen.generate(_while.expr);
        targetMethod.addInstruction("ifeq " + endLabel, -1); // if tos is 0
        targetMethod.pushScope();
        _while.stm.accept(this);
        targetMethod.popScope();
        targetMethod.addInstruction("goto " + testLabel, 0);
        targetMethod.addInstruction(endLabel + ":", 0);
        return null;
    }

    @Override
    public Void visitIfStatement(IfStatementContext _if) {
        String trueLabel = targetMethod.newLabel();
        String endLabel = targetMethod.newLabel();
        exprGen.generate(_if.expr);
        targetMethod.addInstruction("ifne " + trueLabel, -1);
        targetMethod.addInstruction("goto " + endLabel, 0);
        targetMethod.addInstruction(trueLabel + ":", 0);
        targetMethod.pushScope();
        _if.stm.accept(this);
        targetMethod.popScope();
        targetMethod.addInstruction(endLabel + ":", 0);
        return null;
    }

    @Override
    public Void visitIfElseStatement(IfElseStatementContext ifElse) {
        String falseLabel = targetMethod.newLabel();
        String trueLabel = targetMethod.newLabel();
        exprGen.generate(ifElse.expr);
        targetMethod.addInstruction("ifeq " + falseLabel, -1);
        targetMethod.pushScope();
        ifElse.stm1.accept(this);
        targetMethod.popScope();
        targetMethod.addInstruction("goto " + trueLabel, 0);
        targetMethod.addInstruction(falseLabel + ":", 0);
        targetMethod.pushScope();
        ifElse.stm2.accept(this);
        targetMethod.popScope();
        targetMethod.addInstruction(trueLabel + ":", 0);
        return null;
    }

    @Override
    public Void visitReturnStatement(ReturnStatementContext _return) {
        if (_return.expr == null) { // void return
            targetMethod.addInstruction("return", 0);
            return null;
        }
        Type returnedType = exprGen.generate(_return.expr);
        if (returnedType.isDouble())
            targetMethod.addInstruction("dreturn", -2);
        else if (returnedType.isString())
            targetMethod.addInstruction("areturn", -1);
        else // Non-converted ints, bools
            targetMethod.addInstruction("ireturn", -1);
        return null;
    }
}
