package notc.codegen;

import notc.antlrgen.NotCBaseVisitor;
import notc.antlrgen.NotCParser.Type;
import notc.antlrgen.NotCParser.FunctionDefinitionContext;
import notc.antlrgen.NotCParser.StatementContext;
import notc.antlrgen.NotCParser.DeclarationStatementContext;
import notc.antlrgen.NotCParser.InitializationStatementContext;
import notc.antlrgen.NotCParser.ExpressionStatementContext;
import notc.antlrgen.NotCParser.BlockStatementContext;
import notc.antlrgen.NotCParser.WhileStatementContext;
import notc.antlrgen.NotCParser.IfStatementContext;
import notc.antlrgen.NotCParser.IfElseStatementContext;
import notc.antlrgen.NotCParser.ReturnStatementContext;

class FunctionGenerator extends NotCBaseVisitor<Void> {
    private final ExpressionGenerator exprGen;
    private JvmMethod targetMethod;

    FunctionGenerator(ExpressionGenerator exprGen) {
        this.exprGen = exprGen;
    }

    // Entry point. Sets up target and generates the statements.
    JvmMethod generate(FunctionDefinitionContext funDef) {
        String name = funDef.header.id.getText();
        String descriptor = funDef.header.descriptor;
        String specification = name + ":" + descriptor;
        targetMethod = JvmMethod.from(specification);
        targetMethod.reserveVarMemory(funDef.header.params);
        exprGen.setTarget(targetMethod);
        for (StatementContext stm : funDef.body)
            stm.accept(this);
        // Don't fall off the end of the code
        if (funDef.header.returnType.isVoid())
            targetMethod.addInstruction("return", 0);
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
        // Get an address
        targetMethod.reserveVarMemory(initStm.varDecl);
        int varAddr = targetMethod.addressOf(initStm.varDecl);
        // Generate initializing expression and store result
        exprGen.generate(initStm.expr);
        Type varType = initStm.varDecl.type;
        targetMethod.addInstruction(varType.prefix() + "store " + varAddr, -varType.size());
        return null;
    }

    // Expression used as statement
    @Override
    public Void visitExpressionStatement(ExpressionStatementContext exprStm) {
        Type exprType = exprGen.generate(exprStm.expr);
        // Value is not used and should be popped
        String popInstr;
        switch (exprType) {
            case DOUBLE: popInstr = "pop2"; break;
            case INT:
            case BOOL:
            case STRING: popInstr = "pop";  break;
            case VOID:   return null; // Leaves nothing on stack anyway
            default: throw new IllegalArgumentException("Should be unreachable. Type: " + exprType);
        }
        targetMethod.addInstruction(popInstr, -exprType.size());
        return null;
    }

    @Override
    public Void visitBlockStatement(BlockStatementContext block) {
        for (StatementContext stm : block.statements)
            stm.accept(this);
        return null;
    }

    @Override
    public Void visitWhileStatement(WhileStatementContext whileStm) {
        String testLabel = targetMethod.newLabel();
        String endLabel = targetMethod.newLabel();
        targetMethod.addInstruction(testLabel + ":", 0);
        exprGen.generate(whileStm.conditionExpr);
        targetMethod.addInstruction("ifeq " + endLabel, -1); // "if TOS = 0"
        whileStm.loopedStm.accept(this);
        targetMethod.addInstruction("goto " + testLabel, 0);
        targetMethod.addInstruction(endLabel + ":", 0);
        return null;
    }

    @Override
    public Void visitIfStatement(IfStatementContext ifStm) {
        String trueLabel = targetMethod.newLabel();
        String endLabel = targetMethod.newLabel();
        exprGen.generate(ifStm.conditionExpr);
        targetMethod.addInstruction("ifne " + trueLabel, -1); // "if TOS != 0"
        targetMethod.addInstruction("goto " + endLabel, 0);
        targetMethod.addInstruction(trueLabel + ":", 0);
        ifStm.consequentStm.accept(this);
        targetMethod.addInstruction(endLabel + ":", 0);
        return null;
    }

    @Override
    public Void visitIfElseStatement(IfElseStatementContext ifElseStm) {
        String falseLabel = targetMethod.newLabel();
        String trueLabel = targetMethod.newLabel();
        exprGen.generate(ifElseStm.conditionExpr);
        targetMethod.addInstruction("ifeq " + falseLabel, -1);
        ifElseStm.consequentStm.accept(this);
        targetMethod.addInstruction("goto " + trueLabel, 0);
        targetMethod.addInstruction(falseLabel + ":", 0);
        ifElseStm.altStm.accept(this);
        targetMethod.addInstruction(trueLabel + ":", 0);
        return null;
    }

    @Override
    public Void visitReturnStatement(ReturnStatementContext returnStm) {
        if (returnStm.expr == null) { // void return
            targetMethod.addInstruction("return", 0);
            return null;
        }
        Type returnedType = exprGen.generate(returnStm.expr);
        targetMethod.addInstruction(returnedType.prefix() + "return", -returnedType.size());
        return null;
    }
}
