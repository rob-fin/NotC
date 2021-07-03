package notc.analysis;

import notc.analysis.NotCParser.SrcType;
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

import org.antlr.v4.runtime.Token;

import java.util.List;

/* Visitor that type checks expressions.
 * Each visit method infers the type of the expression it was called on
 * and annotates the corresponding parse tree node with it.
 * The inferred type is returned to the caller because the expression
 * may be part of a larger one whose type depends on it.
 * If a type cannot be inferred,
 * a SemanticException is thrown with the offending token and a message. */
class ExpressionChecker extends NotCBaseVisitor<SrcType> {
    private SymbolTable symTab;

    ExpressionChecker(SymbolTable symTab) {
        this.symTab = symTab;
    }

    // Utility function to check if an expression has some expected type
    void expectType(ExpContext exp, SrcType expected) {
        SrcType actual = visit(exp);
        if (actual == expected ||
            actual.isInt() && expected.isDouble()) // Also acceptable
            return;
        throw new SemanticException(exp.getStart(),
                                    "Expression of type " +
                                    actual.name().toLowerCase() +
                                    " where expression of type " +
                                    expected.name().toLowerCase() +
                                    " was expected");
    }

    // Literal expressions simply have the type of the literal
    @Override
    public SrcType visitFalseLitExp(FalseLitExpContext falseLitExp) {
        falseLitExp.typeAnnot = SrcType.BOOL;
        return SrcType.BOOL;
    }

    @Override
    public SrcType visitTrueLitExp(TrueLitExpContext trueLitExp) {
        trueLitExp.typeAnnot = SrcType.BOOL;
        return SrcType.BOOL;
    }

    @Override
    public SrcType visitDoubleLitExp(DoubleLitExpContext doubleLitExp) {
        doubleLitExp.typeAnnot = SrcType.DOUBLE;
        return SrcType.DOUBLE;
    }

    @Override
    public SrcType visitIntLitExp(IntLitExpContext intLitExp) {
        intLitExp.typeAnnot = SrcType.INT;
        return SrcType.INT;
    }

    @Override
    public SrcType visitStringLitExp(StringLitExpContext strLitExp) {
        strLitExp.typeAnnot = SrcType.STRING;
        return SrcType.STRING;
    }

    // Variable: look its type up
    @Override
    public SrcType visitVarExp(VarExpContext varExp) {
        SrcType t = symTab.lookupVar(varExp.varId);
        varExp.typeAnnot = t;
        return t;
    }

    // Function call: id "(" [exp] ")" -> FunCallExp
    @Override
    public SrcType visitFunCallExp(FunCallExpContext callExp) {
        FunType sig = symTab.lookupFun(callExp.funId);
        List<ExpContext> args = callExp.exp();
        if (sig.arity() != args.size())
            throw new SemanticException(callExp.getStart(),
                                        "Wrong number of arguments in function call");
        // Check types of argument expressions against parameters
        int i = 0;
        for (SrcType t : sig.paramTypes())
            expectType(args.get(i++), t);
        callExp.typeAnnot = sig.returnType();
        return sig.returnType();
    }

    // Assignment: id "=" exp -> AssExp, where exp must be inferable to id's declared type
    @Override
    public SrcType visitAssExp(AssExpContext ass) {
        SrcType t = symTab.lookupVar(ass.varId);
        expectType(ass.exp(), t);
        ass.typeAnnot = t;
        return t;
    }

    // Utility function to infer and check arithmetic expressions
    private SrcType checkArithmetic(ExpContext opnd1, ExpContext opnd2) {
        SrcType t1 = visit(opnd1);
        SrcType t2 = visit(opnd2);
        if (!t1.isNumerical() || !t2.isNumerical())
            throw new SemanticException(opnd1.getStart(),
                                        "Attempted arithmetic on non-numerical expression");
        if (t1.isDouble() || t2.isDouble())
            return SrcType.DOUBLE;
        return SrcType.INT;
    }

    // exp "*" exp -> MulExp
    @Override
    public SrcType visitMulExp(MulExpContext mulExp) {
        SrcType t = checkArithmetic(mulExp.opnd1, mulExp.opnd2);
        mulExp.typeAnnot = t;
        return t;
    }

    // exp "/" exp -> DivExp
    @Override
    public SrcType visitDivExp(DivExpContext divExp) {
        SrcType t = checkArithmetic(divExp.opnd1, divExp.opnd2);
        divExp.typeAnnot = t;
        return t;
    }

    // exp "+" exp -> AddExp
    @Override
    public SrcType visitAddExp(AddExpContext addExp) {
        SrcType t = checkArithmetic(addExp.opnd1, addExp.opnd2);
        addExp.typeAnnot = t;
        return t;
    }

    // exp "-" exp -> SubExp
    @Override
    public SrcType visitSubExp(SubExpContext subExp) {
        SrcType t = checkArithmetic(subExp.opnd1, subExp.opnd2);
        subExp.typeAnnot = t;
        return t;
    }

    // Utility function to infer and check increments and decrements
    private SrcType checkIncrDecr(Token varId) {
        SrcType t = symTab.lookupVar(varId);
        if (t.isNumerical())
            return t;
        throw new SemanticException(varId,
                                    "Attempted increment or decrement " +
                                    "of variable that was not int or double");
    }

    // id "++" -> PostIncrExp
    @Override
    public SrcType visitPostIncrExp(PostIncrExpContext postIncrExp) {
        SrcType t = checkIncrDecr(postIncrExp.varId);
        postIncrExp.typeAnnot = t;
        return t;
    }

    // id "--" -> PostDecrExp
    @Override
    public SrcType visitPostDecrExp(PostDecrExpContext postDecrExp) {
        SrcType t = checkIncrDecr(postDecrExp.varId);
        postDecrExp.typeAnnot = t;
        return t;
    }

    // "++" id -> PreIncrExp
    @Override
    public SrcType visitPreIncrExp(PreIncrExpContext preIncrExp) {
        SrcType t = checkIncrDecr(preIncrExp.varId);
        preIncrExp.typeAnnot = t;
        return t;
    }

    // "--" id -> PreDecrExp
    @Override
    public SrcType visitPreDecrExp(PreDecrExpContext preDecrExp) {
        SrcType t = checkIncrDecr(preDecrExp.varId);
        preDecrExp.typeAnnot = t;
        return t;
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
        throw new SemanticException(opnd1.getStart(),
                                    "Ill-typed comparison expression");
    }

    // exp "<" exp-> LtExp
    @Override
    public SrcType visitLtExp(LtExpContext ltExp) {
        checkComparison(ltExp.opnd1, ltExp.opnd2);
        return ltExp.typeAnnot = SrcType.BOOL;
    }

    // exp ">" exp-> GtExp
    @Override
    public SrcType visitGtExp(GtExpContext gtExp) {
        checkComparison(gtExp.opnd1, gtExp.opnd2);
        gtExp.typeAnnot = SrcType.BOOL;
        return SrcType.BOOL;
    }

    // exp ">=" exp-> GEqExp
    @Override
    public SrcType visitGEqExp(GEqExpContext GEqExp) {
        checkComparison(GEqExp.opnd1, GEqExp.opnd2);
        GEqExp.typeAnnot = SrcType.BOOL;
        return SrcType.BOOL;
    }

    // exp "<=" exp-> LEqExp
    @Override
    public SrcType visitLEqExp(LEqExpContext LEqExp) {
        checkComparison(LEqExp.opnd1, LEqExp.opnd2);
        LEqExp.typeAnnot = SrcType.BOOL;
        return SrcType.BOOL;
    }

    // exp "==" exp-> EqExp
    @Override
    public SrcType visitEqExp(EqExpContext EqExp) {
        checkComparison(EqExp.opnd1, EqExp.opnd2);
        EqExp.typeAnnot = SrcType.BOOL;
        return SrcType.BOOL;
    }

    // exp "!=" exp-> NEqExp
    @Override
    public SrcType visitNEqExp(NEqExpContext NEqExp) {
        checkComparison(NEqExp.opnd1, NEqExp.opnd2);
        NEqExp.typeAnnot = SrcType.BOOL;
        return SrcType.BOOL;
    }

    // Utility function to check boolean expressions
    private void checkBoolean(ExpContext opnd1, ExpContext opnd2) {
        SrcType t1 = visit(opnd1);
        SrcType t2 = visit(opnd2);
        if (t1.isBool() && t2.isBool())
            return;
        throw new SemanticException(opnd1.getStart(),
                                    "Ill-typed boolean expression");
    }

    // exp "&&" exp -> AndExp
    @Override
    public SrcType visitAndExp(AndExpContext andExp) {
        checkBoolean(andExp.opnd1, andExp.opnd2);
        andExp.typeAnnot = SrcType.BOOL;
        return SrcType.BOOL;
    }

    // exp "||" exp -> OrExp
    @Override
    public SrcType visitOrExp(OrExpContext orExp) {
        checkBoolean(orExp.opnd1, orExp.opnd2);
        orExp.typeAnnot = SrcType.BOOL;
        return SrcType.BOOL;
    }

}
