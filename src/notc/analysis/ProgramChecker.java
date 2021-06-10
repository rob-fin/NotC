package notc.analysis;

import notc.analysis.NotCParser.ProgramContext;
import notc.analysis.NotCParser.DefContext;
import notc.analysis.NotCParser.TypeContext;
import notc.analysis.NotCParser.ParamsContext;
import notc.analysis.NotCParser.ExpContext;
import notc.analysis.NotCParser.StmContext;
import notc.analysis.NotCParser.DeclStmContext;
import notc.analysis.NotCParser.ExpStmContext;
import notc.analysis.NotCParser.IfElseStmContext;
import notc.analysis.NotCParser.WhileStmContext;
import notc.analysis.NotCParser.BlockStmContext;
import notc.analysis.NotCParser.ReturnStmContext;
import notc.analysis.NotCParser.InitStmContext;

import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeVisitor;
import org.antlr.v4.runtime.tree.TerminalNode;

import java.util.List;
import java.util.LinkedList;
import java.util.Collections;
import java.util.stream.Collectors;
import java.util.function.Function;



// Visitor for the highest-level construct in the grammar. Entry point for the type checker.
public class ProgramChecker extends NotCBaseVisitor<Void> {

    SymbolTable symTab;

    /* Check the list of function definitions in two passes:
     * First to collect the id and type signature of each function.
     * Then to check their definitions in a context where these functions are declared. */
    @Override
    public Void visitProgram(ProgramContext prog) {
        symTab = new SymbolTable();

        // Populate symbol table with functions
        for (DefContext def : prog.def()) {
            SrcType returnType = SrcType.resolve(def.returnType);
            List<SrcType> paramTypes = transformTreeList(def.params().type(),
                                                         t -> SrcType.resolve(t));
            FunType signature = new FunType(returnType, paramTypes);
            String id = def.funId.getText();
            symTab.addFun(id, signature);
        }

        // Check that main is present and ok
        FunType mainType = symTab.lookupFun("main");
        if (mainType.arity() != 0)
            throw new TypeException("Non-empty parameter list in function main");
        SrcType mainReturn = mainType.getReturnType();
        if (mainReturn != SrcType.VOID)
            throw new TypeException("Non-void return type declared for function main");

        FunctionChecker funChecker = new FunctionChecker();
        // Type check each function
        for (DefContext def : prog.def())
            def.accept(funChecker);

        // Program is well-typed and now has type annotations to be used by the code generator
        return null;

    }

    // Utility function to convert lists with elements of ANTLR generated types
    // to lists with elements of some other type (e.g. TerminalNode list to String list)
    private <A,R> List<R> convertAstList(List<A> astList, Function<A,R> op) {
        if (astList == null)
            return Collections.<R>emptyList();
        return treeList.stream().map(op).collect(Collectors.toList());
    }

}

    /* Visitor that type checks a function definition's statements.
     * This involves type checking any constituent expressions. */
    private class FunctionChecker extends NotCBaseVisitor<Void> {
        private SrcType expectedReturn;
        private ExpressionChecker expChecker;

        FunctionChecker() {
            expChecker = new ExpressionChecker(symTab);
        }

        // Entry point: Add parameters as local variables, then visit each statement
        @Override
        public Void visitDef(DefContext def) {
            FunType sig = symTab.lookupFun(def.funId.getText());
            List<String> varIds = transformTreeList(def.params().ID(), TerminalNode::getText);
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
            List<String> varIds = transformTreeList(decl.ID(), TerminalNode::getText);
            for (String id : varIds)
                symTab.addVar(t, id);
            return null;
        }

        // Initialization: type id "=" exp ";" -> InitStm
        @Override
        public Void visitInitStm(InitStmContext init) {
            SrcType t = SrcType.resolve(init.type());
            String id = init.ID().getText();
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

}
