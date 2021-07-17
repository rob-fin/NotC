package notc.semantics;

import notc.antlrgen.NotCBaseVisitor;
import notc.antlrgen.NotCParser.SrcType;
import notc.antlrgen.NotCParser.ExpContext;
import notc.antlrgen.NotCParser.FalseLitExpContext;
import notc.antlrgen.NotCParser.TrueLitExpContext;
import notc.antlrgen.NotCParser.DoubleLitExpContext;
import notc.antlrgen.NotCParser.IntLitExpContext;
import notc.antlrgen.NotCParser.StringLitExpContext;
import notc.antlrgen.NotCParser.VarExpContext;
import notc.antlrgen.NotCParser.FunCallExpContext;
import notc.antlrgen.NotCParser.AssExpContext;
import notc.antlrgen.NotCParser.MultiplicativeExpContext;
import notc.antlrgen.NotCParser.AdditiveExpContext;
import notc.antlrgen.NotCParser.PostIncrExpContext;
import notc.antlrgen.NotCParser.PostDecrExpContext;
import notc.antlrgen.NotCParser.PreIncrExpContext;
import notc.antlrgen.NotCParser.PreDecrExpContext;
import notc.antlrgen.NotCParser.LtExpContext;
import notc.antlrgen.NotCParser.GtExpContext;
import notc.antlrgen.NotCParser.GEqExpContext;
import notc.antlrgen.NotCParser.LEqExpContext;
import notc.antlrgen.NotCParser.EqExpContext;
import notc.antlrgen.NotCParser.NEqExpContext;
import notc.antlrgen.NotCParser.AndExpContext;
import notc.antlrgen.NotCParser.OrExpContext;
import notc.antlrgen.NotCParser.ParenthesizedExpContext;

import org.antlr.v4.runtime.Token;

import java.util.List;

// Visitor that type checks expressions. Each visit method infers the type
// of the expression it was called on and annotates the corresponding
// parse tree node with it. The annotations are used by the code generator.
// The inferred type is returned to the caller because the expression
// may be part of a larger one whose type depends on it.
// If a type cannot be inferred,
// a SemanticException is thrown with the offending token and a message.
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
                                    "Expression of type "
                                  + actual.name().toLowerCase()
                                  + " where expression of type "
                                  + expected.name().toLowerCase()
                                  + " was expected");
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

    // Check multiplications and divisions
    @Override
    public SrcType visitMultiplicativeExp(MultiplicativeExpContext multipExp) {
        SrcType t = checkArithmetic(multipExp.opnd1, multipExp.opnd2);
        multipExp.typeAnnot = t;
        return t;
    }

    // Check additions and subtractions
    @Override
    public SrcType visitAdditiveExp(AdditiveExpContext additExp) {
        SrcType t = checkArithmetic(additExp.opnd1, additExp.opnd2);
        additExp.typeAnnot = t;
        return t;
    }

    // Utility function to infer and check increments and decrements
    private SrcType checkIncrDecr(Token varId) {
        SrcType t = symTab.lookupVar(varId);
        if (t.isNumerical())
            return t;
        throw new SemanticException(varId,
                                    "Attempted increment or decrement "
                                  + "of variable that was not int or double");
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

    // Utility function to check comparison expressions.
    // Type bool is not numerical as in C,
    // but its values have the relation true > false.
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

    @Override
    public SrcType visitParenthesizedExp(ParenthesizedExpContext paren) {
        SrcType t = paren.expr.accept(this);
        paren.typeAnnot = t;
        return t;
    }

}
