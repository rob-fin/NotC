package notc.analysis;

import notc.analysis.NotCParser.*;

import org.antlr.v4.runtime.tree.TerminalNode;

import java.util.stream.Collectors;
import java.util.List;
import java.util.LinkedList;
import java.util.Collections;

class CheckFunction extends NotCBaseVisitor<Void> {
    
    private SymbolTable symTab;
    private SrcType expectedReturn;
    private CheckStatement checkStm;
    private InferExpression inferExp;
    
    CheckFunction(SymbolTable symTab) {
        this.symTab = symTab;
        checkStm = new CheckStatement();
        inferExp = new InferExpression();
    }
    
    public void checkFunctionBody(List<StmContext> stms, SrcType expectedReturn) {
        this.expectedReturn = expectedReturn;
        for (StmContext stm : stms)
            checkStm.visit(stm);
    }
    
    // Check if an expression has some expected type 
    private void checkExp(ExpContext exp, SrcType expected) {
        SrcType actual = inferExp.visit(exp);
        if (actual == expected ||
            actual.isInt() && expected.isDouble()) // Also acceptable
            return;
        
        throw new TypeException("Expression of type " +
                                actual.name().toLowerCase() +
                                " where expression of type " +
                                expected.name().toLowerCase() +
                                " was expected");
    }
    
    private class CheckStatement extends NotCBaseVisitor<Void> {
        
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
            checkExp(exp, t);
            symTab.addVar(t, id);
            return null;
        }

        @Override
        public Void visitExpStm(ExpStmContext es) {
            inferExp.visit(es.exp());
            return null;
        }

        @Override
        public Void visitReturnStm(ReturnStmContext ret) {
            checkExp(ret.exp(), expectedReturn);
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
            checkExp(wh.exp(), SrcType.BOOL);
            symTab.pushScope();
            visit(wh.stm());
            symTab.popScope();
            return null;
        }

        @Override
        public Void visitIfElseStm(IfElseStmContext ie) {
            checkExp(ie.exp(), SrcType.BOOL);
            for (StmContext stm : ie.stm()) { // Always two stms for now
                symTab.pushScope();
                visit(stm);
                symTab.popScope();
            }
            return null;
        }
    }
    
    private class InferExpression extends NotCBaseVisitor<SrcType> {
        @Override
        public SrcType visitFalseLitExp(FalseLitExpContext falseLitExp) {
            return falseLitExp.annot = SrcType.BOOL;
        }
        @Override
        public SrcType visitTrueLitExp(TrueLitExpContext trueLitExp) {
            return trueLitExp.annot = SrcType.BOOL;
        }
        @Override
        public SrcType visitDoubleLitExp(DoubleLitExpContext doubleLitExp) {
            return doubleLitExp.annot = SrcType.DOUBLE;
        }
        @Override
        public SrcType visitIntLitExp(IntLitExpContext intLitExp) {
            return intLitExp.annot = SrcType.INT;
        }
        @Override
        public SrcType visitStringLitExp(StringLitExpContext strLitExp) {
            return strLitExp.annot = SrcType.STRING;
        }
        @Override
        public SrcType visitVarExp(VarExpContext varExp) {
            String id = varExp.varId.getText();
            SrcType t = symTab.lookupVar(id);
            return varExp.annot = t;
        }
        @Override
        public SrcType visitFunCallExp(FunCallExpContext callExp) {
            String id = callExp.funId.getText();
            FunType sig = symTab.lookupFun(id);
            List<ExpContext> args = callExp.exp();
            if (sig.getArity() != args.size())
                throw new TypeException("Wrong number of arguments to function " + id);
            // Check types of argument expressions against parameters
            int i = 0;
            for (SrcType t : sig.getParamTypes())
                checkExp(args.get(i++), t);
            return callExp.annot = sig.getReturnType();
        }
        @Override
        public SrcType visitAssExp(AssExpContext ass) {
            return null;
        }
        // Utility function to infer and check arithmetic expressions 
        private SrcType checkArithmetic(ExpContext opnd1, ExpContext opnd2) {
            SrcType t1 = visit(opnd1);
            SrcType t2 = visit(opnd2);
            if (!t1.isNumerical() || !t2.isNumerical())
                throw new TypeException("Attempted arithmetic on non-numerical expression");
            if (t1 == SrcType.DOUBLE || t2 == SrcType.DOUBLE)
                return SrcType.DOUBLE;
            return SrcType.INT;
        }
        @Override
        public SrcType visitMulExp(MulExpContext mulExp) {
            return mulExp.annot = checkArithmetic(mulExp.opnd1, mulExp.opnd2);
        }
        @Override
        public SrcType visitDivExp(DivExpContext divExp) {
            return divExp.annot = checkArithmetic(divExp.opnd1, divExp.opnd2);
        }
        @Override
        public SrcType visitAddExp(AddExpContext addExp) {
            return addExp.annot = checkArithmetic(addExp.opnd1, addExp.opnd2);
        }
        @Override
        public SrcType visitSubExp(SubExpContext subExp) {
            return subExp.annot = checkArithmetic(subExp.opnd1, subExp.opnd2);
        }
        // Utility function to infer and check increments and decrements 
        private SrcType checkIncrDecr(String id) {
            SrcType t = symTab.lookupVar(id);
            if (t.isNumerical())
                return t;
            throw new TypeException("Attempted increment or decrement of " +
                                    "variable that was not int or double");
        }
        @Override
        public SrcType visitPostIncrExp(PostIncrExpContext postIncrExp) {
            return postIncrExp.annot = checkIncrDecr(postIncrExp.varId.getText());
        }
        @Override
        public SrcType visitPostDecrExp(PostDecrExpContext postDecrExp) {
            return postDecrExp.annot = checkIncrDecr(postDecrExp.varId.getText());
        }
        @Override
        public SrcType visitPreIncrExp(PreIncrExpContext preIncrExp) {
            return preIncrExp.annot = checkIncrDecr(preIncrExp.varId.getText());
        }
        @Override
        public SrcType visitPreDecrExp(PreDecrExpContext preDecrExp) {
            return preDecrExp.annot = checkIncrDecr(preDecrExp.varId.getText());
        }
        /*  Utility function to check comparison expressions.
            Type bool is not numerical as in C
            but its values have the relation true > false */
        private void checkComparison(ExpContext opnd1, ExpContext opnd2) {
            SrcType t1 = visit(opnd1);
            SrcType t2 = visit(opnd2);
            if (t1.isNumerical() && t2.isNumerical() ||
                t1.isBool() && t2.isBool())
                return;
            throw new TypeException("Ill-typed boolean operation");
        }
        @Override
        public SrcType visitLtExp(LtExpContext ltExp) {
            checkComparison(ltExp.opnd1, ltExp.opnd2);
            return ltExp.annot = SrcType.BOOL;
        }
        @Override
        public SrcType visitGtExp(GtExpContext gtExp) {
            checkComparison(gtExp.opnd1, gtExp.opnd2);
            return gtExp.annot = SrcType.BOOL;
        }
        @Override
        public SrcType visitGEqExp(GEqExpContext GEqExp) {
            checkComparison(GEqExp.opnd1, GEqExp.opnd2);
            return GEqExp.annot = SrcType.BOOL;
        }
        @Override
        public SrcType visitLEqExp(LEqExpContext LEqExp) {
            checkComparison(LEqExp.opnd1, LEqExp.opnd2);
            return LEqExp.annot = SrcType.BOOL;
        }
        @Override
        public SrcType visitEqExp(EqExpContext EqExp) {
            checkComparison(EqExp.opnd1, EqExp.opnd2);
            return EqExp.annot = SrcType.BOOL;
        }
        @Override
        public SrcType visitNEqExp(NEqExpContext NEqExp) {
            checkComparison(NEqExp.opnd1, NEqExp.opnd2);
            return NEqExp.annot = SrcType.BOOL;
        }
        /* Utility function to check boolean expressions */
        private void checkBoolean(ExpContext opnd1, ExpContext opnd2) {
            SrcType t1 = visit(opnd1);
            SrcType t2 = visit(opnd2);
            if (t1.isBool() && t2.isBool())
                return;
            throw new TypeException("Ill-typed boolean operation");
        }
        @Override
        public SrcType visitAndExp(AndExpContext andExp) {
            checkBoolean(andExp.opnd1, andExp.opnd2);
            return andExp.annot = SrcType.BOOL;
        }
        @Override
        public SrcType visitOrExp(OrExpContext orExp) {
            checkBoolean(orExp.opnd1, orExp.opnd2);
            return orExp.annot = SrcType.BOOL;
        }
        
    }
    
}
