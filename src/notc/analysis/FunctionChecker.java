package notc.analysis;

import notc.analysis.NotCParser.*;

import org.antlr.v4.runtime.tree.TerminalNode;

import java.util.stream.Collectors;
import java.util.List;
import java.util.LinkedList;
import java.util.Collections;

class FunctionChecker extends NotCBaseVisitor<Void> {
    
    private SymbolTable symTab;
    private SrcType expectedReturn;
    private ExpressionChecker expChecker;
    
    FunctionChecker(SymbolTable symTab) {
        this.symTab = symTab;
        expChecker = new ExpressionChecker(symTab);
    }
    
    @Override
    public Void visitDef(DefContext def) {
        FunType sig = symTab.lookupFun(def.funId.getText());
        List<String> varIds = def.params().ID().stream()
                              .map(TerminalNode::getText)
                              .collect(Collectors.toList());
        symTab.setContext(sig.getParamTypes(), varIds);
        expectedReturn = sig.getReturnType();
        for (StmContext stm : def.stm())
            visit(stm);
        return null;
    }

    
    @Override
    public Void visitDeclStm(DeclStmContext decl) {
        SrcType t = SrcType.resolve(decl.type());
        List<String> varIds = decl.ID().stream()
                                .map(TerminalNode::getText)
                                .collect(Collectors.toList());
        for (String id : varIds)
            symTab.addVar(t, id);
        return null;
    }

    @Override
    public Void visitInitStm(InitStmContext init) {
        SrcType t = SrcType.resolve(init.type());
        String id = init.ID().getText();
        ExpContext exp = init.exp();
        expChecker.expectType(exp, t);
        symTab.addVar(t, id);
        return null;
    }

    @Override
    public Void visitExpStm(ExpStmContext es) {
        expChecker.visit(es.exp());
        return null;
    }

    @Override
    public Void visitReturnStm(ReturnStmContext ret) {
        expChecker.expectType(ret.exp(), expectedReturn);
        return null;
    }

    @Override
    public Void visitBlockStm(BlockStmContext bl) {
        symTab.pushScope();
        for (StmContext stm : bl.stm())
            visit(stm);
        symTab.popScope();
        return null;
    }

    @Override
    public Void visitWhileStm(WhileStmContext wh) {
        expChecker.expectType(wh.exp(), SrcType.BOOL);
        symTab.pushScope();
        visit(wh.stm());
        symTab.popScope();
        return null;
    }

    @Override
    public Void visitIfElseStm(IfElseStmContext ie) {
        expChecker.expectType(ie.exp(), SrcType.BOOL);
        for (StmContext stm : ie.stm()) { // Always two stms for now
            symTab.pushScope();
            visit(stm);
            symTab.popScope();
        }
        return null;
    }

    
}
