package notc.analysis;

import notc.analysis.NotCParser.ExpContext;
import notc.analysis.NotCParser.FalseLitExpContext;
import notc.analysis.NotCParser.TrueLitExpContext;
import notc.analysis.NotCParser.DoubleLitExpContext;
import notc.analysis.NotCParser.IntLitExpContext;
import notc.analysis.NotCParser.StringLitExpContext;
import notc.analysis.NotCParser.VarExpContext;
import notc.analysis.NotCParser.FunCallExpContext;
import notc.analysis.NotCParser.AssExpContext;
import notc.analysis.NotCParser.MulExpContext;
import notc.analysis.NotCParser.DivExpContext;
import notc.analysis.NotCParser.AddExpContext;
import notc.analysis.NotCParser.SubExpContext;
import notc.analysis.NotCParser.PostIncrExpContext;
import notc.analysis.NotCParser.PostDecrExpContext;
import notc.analysis.NotCParser.PreIncrExpContext;
import notc.analysis.NotCParser.PreDecrExpContext;
import notc.analysis.NotCParser.LtExpContext;
import notc.analysis.NotCParser.GtExpContext;
import notc.analysis.NotCParser.GEqExpContext;
import notc.analysis.NotCParser.LEqExpContext;
import notc.analysis.NotCParser.EqExpContext;
import notc.analysis.NotCParser.NEqExpContext;
import notc.analysis.NotCParser.AndExpContext;
import notc.analysis.NotCParser.OrExpContext;

import java.util.List;

/* Visitor class that type checks expressions.
/* Each visit method infers the type of the expression it was called on
 * and annotates the corresponding tree node with it.
 * The type is also returned to the caller because the expression
 * may be part of a larger one whose type depends on it.
 * If a type cannot be inferred, an exception is thrown. */
class ExpressionChecker extends NotCBaseVisitor<SrcType> {
    private SymbolTable symTab;

    ExpressionChecker(SymbolTable symTab) {
        this.symTab = symTab;
    }

    // Check if an expression has some expected type 
    void expectType(ExpContext exp, SrcType expected) {
        SrcType actual = visit(exp);
        if (actual == expected ||
            actual.isInt() && expected.isDouble()) // Also acceptable
            return;
        throw new TypeException("Expression of type " +
                                actual.name().toLowerCase() +
                                " where expression of type " +
                                expected.name().toLowerCase() +
                                " was expected");
    }

    // Literal expressions simply have the type of the literal
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

    // Variable
    @Override
    public SrcType visitVarExp(VarExpContext varExp) {
        String id = varExp.varId.getText();
        SrcType t = symTab.lookupVar(id);
        return varExp.annot = t;
    }

    // Function call: id "(" [exp] ")" -> FunCallExp
    @Override
    public SrcType visitFunCallExp(FunCallExpContext callExp) {
        String id = callExp.funId.getText();
        FunType sig = symTab.lookupFun(id);
        List<ExpContext> args = callExp.exp();
        if (sig.arity() != args.size())
            throw new TypeException("Wrong number of " +
                                    "arguments to function " + id);
        // Check types of argument expressions against parameters
        int i = 0;
        for (SrcType t : sig.paramTypes())
            expectType(args.get(i++), t);
        return callExp.annot = sig.returnType();
    }

    // Assignment: id "=" exp -> AssExp
    @Override
    public SrcType visitAssExp(AssExpContext ass) {
        SrcType t = symTab.lookupVar(ass.varId.getText());
        expectType(ass.exp(), t);
        return ass.annot = t;
    }

    // Utility function to infer and check arithmetic expressions
    private SrcType checkArithmetic(ExpContext opnd1, ExpContext opnd2) {
        SrcType t1 = visit(opnd1);
        SrcType t2 = visit(opnd2);
        if (!t1.isNumerical() || !t2.isNumerical())
            throw new TypeException("Attempted arithmetic " +
                                    "on non-numerical expression");
        if (t1.isDouble() || t2.isDouble())
            return SrcType.DOUBLE;
        return SrcType.INT;
    }

    // exp "*" exp -> MulExp
    @Override
    public SrcType visitMulExp(MulExpContext mulExp) {
        SrcType t = checkArithmetic(mulExp.opnd1, mulExp.opnd2);
        return mulExp.annot = t;
    }

    // exp "/" exp -> DivExp
    @Override
    public SrcType visitDivExp(DivExpContext divExp) {
        SrcType t = checkArithmetic(divExp.opnd1, divExp.opnd2);
        return divExp.annot = t;
    }

    // exp "+" exp -> AddExp
    @Override
    public SrcType visitAddExp(AddExpContext addExp) {
        SrcType t = checkArithmetic(addExp.opnd1, addExp.opnd2);
        return addExp.annot = t;
    }

    // exp "-" exp -> SubExp
    @Override
    public SrcType visitSubExp(SubExpContext subExp) {
        SrcType t = checkArithmetic(subExp.opnd1, subExp.opnd2);
        return subExp.annot = t;
    }

    // Utility function to infer and check increments and decrements
    private SrcType checkIncrDecr(String id) {
        SrcType t = symTab.lookupVar(id);
        if (t.isNumerical())
            return t;
        throw new TypeException("Attempted increment or decrement of " +
                                "variable that was not int or double");
    }

    // id "++" -> PostIncrExp
    @Override
    public SrcType visitPostIncrExp(PostIncrExpContext postIncrExp) {
        SrcType t = checkIncrDecr(postIncrExp.varId.getText());
        return postIncrExp.annot = t;
    }

    // id "--" -> PostDecrExp
    @Override
    public SrcType visitPostDecrExp(PostDecrExpContext postDecrExp) {
        SrcType t = checkIncrDecr(postDecrExp.varId.getText());
        return postDecrExp.annot = t;
    }

    // "++" id -> PreIncrExp
    @Override
    public SrcType visitPreIncrExp(PreIncrExpContext preIncrExp) {
        SrcType t = checkIncrDecr(preIncrExp.varId.getText());
        return preIncrExp.annot = t;
    }

    // "--" id -> PreDecrExp
    @Override
    public SrcType visitPreDecrExp(PreDecrExpContext preDecrExp) {
        SrcType t = checkIncrDecr(preDecrExp.varId.getText());
        return preDecrExp.annot = t;
    }

    /* Utility function to check comparison expressions.
     * Type bool is not numerical as in C,
     * but its values have the relation true > false. */
    private void checkComparison(ExpContext opnd1, ExpContext opnd2) {
        SrcType t1 = visit(opnd1);
        SrcType t2 = visit(opnd2);
        if (t1.isNumerical() && t2.isNumerical() ||
            t1.isBool() && t2.isBool())
            return;
        throw new TypeException("Ill-typed boolean operation");
    }

    // exp "<" exp-> LtExp
    @Override
    public SrcType visitLtExp(LtExpContext ltExp) {
        checkComparison(ltExp.opnd1, ltExp.opnd2);
        return ltExp.annot = SrcType.BOOL;
    }

    // exp ">" exp-> GtExp 
    @Override
    public SrcType visitGtExp(GtExpContext gtExp) {
        checkComparison(gtExp.opnd1, gtExp.opnd2);
        return gtExp.annot = SrcType.BOOL;
    }

    // exp ">=" exp-> GEqExp
    @Override
    public SrcType visitGEqExp(GEqExpContext GEqExp) {
        checkComparison(GEqExp.opnd1, GEqExp.opnd2);
        return GEqExp.annot = SrcType.BOOL;
    }

    // exp "<=" exp-> LEqExp
    @Override
    public SrcType visitLEqExp(LEqExpContext LEqExp) {
        checkComparison(LEqExp.opnd1, LEqExp.opnd2);
        return LEqExp.annot = SrcType.BOOL;
    }

    // exp "==" exp-> EqExp
    @Override
    public SrcType visitEqExp(EqExpContext EqExp) {
        checkComparison(EqExp.opnd1, EqExp.opnd2);
        return EqExp.annot = SrcType.BOOL;
    }

    // exp "!=" exp-> NEqExp
    @Override
    public SrcType visitNEqExp(NEqExpContext NEqExp) {
        checkComparison(NEqExp.opnd1, NEqExp.opnd2);
        return NEqExp.annot = SrcType.BOOL;
    }

    // Utility function to check boolean expressions
    private void checkBoolean(ExpContext opnd1, ExpContext opnd2) {
        SrcType t1 = visit(opnd1);
        SrcType t2 = visit(opnd2);
        if (t1.isBool() && t2.isBool())
            return;
        throw new TypeException("Ill-typed boolean operation");
    }

    // exp "&&" exp -> AndExp
    @Override
    public SrcType visitAndExp(AndExpContext andExp) {
        checkBoolean(andExp.opnd1, andExp.opnd2);
        return andExp.annot = SrcType.BOOL;
    }

    // exp "||" exp -> OrExp
    @Override
    public SrcType visitOrExp(OrExpContext orExp) {
        checkBoolean(orExp.opnd1, orExp.opnd2);
        return orExp.annot = SrcType.BOOL;
    }

}
