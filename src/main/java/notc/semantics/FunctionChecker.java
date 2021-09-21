package notc.semantics;

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

// Visitor that performs semantic analysis on function definitions.
// This involves visiting each statement
// and type checking their constituent expressions.
class FunctionChecker extends NotCBaseVisitor<Void> {
    private final SymbolTable symTab;
    private final ExpressionChecker exprChecker;
    private Type expectedReturn;

    FunctionChecker(SymbolTable symTab) {
        this.symTab = symTab;
        exprChecker = new ExpressionChecker(symTab);
    }

    // Entry point: adds parameters as local variables, then visits each statement
    void checkDefinition(FunctionDefinitionContext funDef) {
        symTab.resetScope();
        symTab.declareVariables(funDef.header.params);
        expectedReturn = funDef.header.returnType;
        for (StatementContext stm : funDef.body)
            stm.accept(this);
    }

    // "type id1, id2...": adds variables to symbol table
    @Override
    public Void visitDeclarationStatement(DeclarationStatementContext declStm) {
        symTab.declareVariables(declStm.varDecls);
        return null;
    }

    // "type id = expr": adds variable after checking its initializing expression
    @Override
    public Void visitInitializationStatement(InitializationStatementContext initStm) {
        Type declaredType = initStm.varDecl.type;
        exprChecker.expectType(initStm.expr, declaredType);
        symTab.declareVariable(initStm.varDecl);
        return null;
    }

    // Expression used as statement
    @Override
    public Void visitExpressionStatement(ExpressionStatementContext exprStm) {
        exprStm.expr.accept(exprChecker);
        return null;
    }

    // New scope within block statement
    @Override
    public Void visitBlockStatement(BlockStatementContext block) {
        symTab.pushScope();
        for (StatementContext stm : block.statements)
            stm.accept(this);
        symTab.popScope();
        return null;
    }

    // Same in for, while and if, and their condition expressions need to be checked

    @Override
    public Void visitForStatement(ForStatementContext forStm) {
        symTab.pushScope();
        if (forStm.initExpr != null)
            forStm.initExpr.accept(exprChecker);
        if (forStm.conditionExpr != null)
            exprChecker.expectType(forStm.conditionExpr, Type.BOOL);
        if (forStm.advanceExpr != null)
            forStm.advanceExpr.accept(exprChecker);
        forStm.body.accept(this);
        symTab.popScope();
        return null;
    }

    @Override
    public Void visitWhileStatement(WhileStatementContext whileStm) {
        exprChecker.expectType(whileStm.conditionExpr, Type.BOOL);
        symTab.pushScope();
        whileStm.loopedStm.accept(this);
        symTab.popScope();
        return null;
    }

    @Override
    public Void visitIfStatement(IfStatementContext ifStm) {
        exprChecker.expectType(ifStm.conditionExpr, Type.BOOL);
        symTab.pushScope();
        ifStm.consequentStm.accept(this);
        symTab.popScope();
        return null;
    }

    // New scope in each branch
    @Override
    public Void visitIfElseStatement(IfElseStatementContext ifElseStm) {
        exprChecker.expectType(ifElseStm.conditionExpr, Type.BOOL);
        symTab.pushScope();
        ifElseStm.consequentStm.accept(this);
        symTab.popScope();
        symTab.pushScope();
        ifElseStm.altStm.accept(this);
        symTab.popScope();
        return null;
    }

    // Checks expression of return statement against function's declared return type
    // (unless it's void).
    @Override
    public Void visitReturnStatement(ReturnStatementContext returnStm) {
        if (returnStm.expr != null) {
            exprChecker.expectType(returnStm.expr, expectedReturn);
            return null;
        }
        if (!expectedReturn.isVoid()) {
            throw new SemanticException(returnStm.getStart(),
                "Return without value in non-void function"
            );
        }
        return null;
    }

}
