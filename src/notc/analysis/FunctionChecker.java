package notc.analysis;

import notc.analysis.NotCParser.DefContext;
import notc.analysis.NotCParser.ExpContext;
import notc.analysis.NotCParser.StmContext;
import notc.analysis.NotCParser.DeclStmContext;
import notc.analysis.NotCParser.ExpStmContext;
import notc.analysis.NotCParser.IfElseStmContext;
import notc.analysis.NotCParser.WhileStmContext;
import notc.analysis.NotCParser.BlockStmContext;
import notc.analysis.NotCParser.ReturnStmContext;
import notc.analysis.NotCParser.InitStmContext;

import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.TerminalNode;

import java.util.List;

/* Visitor that performs semantic analysis on function definitions.
 * This involves visiting each statement and type checking any constituent expressions. */
class FunctionChecker extends NotCBaseVisitor<Void> {
    private SymbolTable symTab;
    private SrcType expectedReturn;
    private ExpressionChecker expChecker;

    FunctionChecker(SymbolTable symTab) {
        this.symTab = symTab;
        expChecker = new ExpressionChecker(symTab);
    }

    // Entry point: Add parameters as local variables, then visit each statement
    @Override
    public Void visitDef(DefContext def) {
        FunType sig = symTab.lookupFun(def.funId);
        List<Token> varIds = NotCParser.transformList(def.params().ID(),
                                                      TerminalNode::getSymbol);
        symTab.setContext(sig.paramTypes(), varIds);
        expectedReturn = sig.returnType();
        for (StmContext stm : def.stm())
            visit(stm);
        return null;
    }

    // Declaration: type [id] ";" -> DeclStm
    @Override
    public Void visitDeclStm(DeclStmContext decl) {
        SrcType t = SrcType.resolve(decl.type());
        List<Token> varIds = NotCParser.transformList(decl.ID(),
                                                      TerminalNode::getSymbol);
        for (Token id : varIds)
            symTab.addVar(t, id);
        return null;
    }

    // Initialization: type id "=" exp ";" -> InitStm
    @Override
    public Void visitInitStm(InitStmContext init) {
        SrcType t = SrcType.resolve(init.type());
        Token id = init.ID().getSymbol();
        ExpContext exp = init.exp();
        expChecker.expectType(exp, t);
        symTab.addVar(t, id);
        return null;
    }

    // Expression used as statement: exp ";" -> ExpStm
    @Override
    public Void visitExpStm(ExpStmContext es) {
        expChecker.visit(es.exp());
        return null;
    }

    // Return: "return" exp ";" -> ReturnStm
    @Override
    public Void visitReturnStm(ReturnStmContext ret) {
        expChecker.expectType(ret.exp(), expectedReturn);
        return null;
    }

    // Block: "{" [stm] "}" -> BlockStm
    @Override
    public Void visitBlockStm(BlockStmContext bl) {
        symTab.pushScope();
        for (StmContext stm : bl.stm())
            visit(stm);
        symTab.popScope();
        return null;
    }

    // while: "while" "(" exp ")" stm -> WhileStm
    @Override
    public Void visitWhileStm(WhileStmContext wh) {
        expChecker.expectType(wh.exp(), SrcType.BOOL);
        symTab.pushScope();
        visit(wh.stm());
        symTab.popScope();
        return null;
    }

    // if-else: "if" "(" exp ")" stm1 "else" stm2 -> IfElseStm
    @Override
    public Void visitIfElseStm(IfElseStmContext ie) {
        expChecker.expectType(ie.exp(), SrcType.BOOL);
        symTab.pushScope();
        visit(ie.stm1);
        symTab.popScope();
        symTab.pushScope();
        visit(ie.stm2);
        symTab.popScope();
        return null;
    }

}
