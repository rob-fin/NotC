package notc.semantics;

import notc.antlrgen.NotCBaseVisitor;
import notc.antlrgen.NotCParser.Type;
import notc.antlrgen.NotCParser.Signature;
import notc.antlrgen.NotCParser.ExpressionContext;
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

import org.antlr.v4.runtime.Token;

// Visitor that performs semantic analysis on function definitions.
// This involves visiting each statement
// and type checking their constituent expressions.
class FunctionChecker extends NotCBaseVisitor<Void> {
    private SymbolTable symTab;
    private Type expectedReturn;
    private ExpressionChecker exprChecker;

    FunctionChecker(SymbolTable symTab) {
        this.symTab = symTab;
        exprChecker = new ExpressionChecker(symTab);
    }

    // Entry point: Add parameters as local variables, then visit each statement
    void checkDefinition(FunctionDefinitionContext funDef) {
        Signature signature = symTab.lookupFun(funDef.id);
        symTab.setContext(signature.paramTypes(), funDef.paramIds);
        expectedReturn = signature.returnType();
        for (StatementContext stm : funDef.body)
            stm.accept(this);
    }

    // "type id1, id2...": Add declared variables to symbol table
    @Override
    public Void visitDeclarationStatement(DeclarationStatementContext declStm) {
        Type declaredType = declStm.typeDeclaration.type;
        for (Token id : declStm.varIds)
            symTab.addVar(declaredType, id);
        return null;
    }

    // "type id = expr": Add variable after checking its initializing expression
    @Override
    public Void visitInitializationStatement(InitializationStatementContext initStm) {
        Type declaredType = initStm.typeDeclaration.type;
        exprChecker.expectType(initStm.expr, declaredType);
        symTab.addVar(declaredType, initStm.varId);
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

    // Same in while and if, but also check expression

    @Override
    public Void visitWhileStatement(WhileStatementContext _while) {
        exprChecker.expectType(_while.expr, Type.BOOL);
        symTab.pushScope();
        _while.stm.accept(this);
        symTab.popScope();
        return null;
    }

    @Override
    public Void visitIfStatement(IfStatementContext _if) {
        exprChecker.expectType(_if.expr, Type.BOOL);
        symTab.pushScope();
        _if.stm.accept(this);
        symTab.popScope();
        return null;
    }

    // If-else statements define two new scopes
    @Override
    public Void visitIfElseStatement(IfElseStatementContext ifElse) {
        exprChecker.expectType(ifElse.expr, Type.BOOL);
        symTab.pushScope();
        ifElse.stm1.accept(this);
        symTab.popScope();
        symTab.pushScope();
        ifElse.stm2.accept(this);
        symTab.popScope();
        return null;
    }

    // Check expression of return statement against function's declared return type
    // (unless it's void).
    @Override
    public Void visitReturnStatement(ReturnStatementContext _return) {
        if (_return.expr != null) {
            exprChecker.expectType(_return.expr, expectedReturn);
            return null;
        }
        if (!expectedReturn.isVoid()) {
            throw new SemanticException(_return.getStart(),
                                        "Return statement with value in void-returning function");
        }
        return null;
    }

}
