package notc.semantics;

import notc.antlrgen.NotCBaseVisitor;
import notc.antlrgen.NotCParser.Type;
import notc.antlrgen.NotCParser.ExpressionContext;
import notc.antlrgen.NotCParser.FalseLiteralExpressionContext;
import notc.antlrgen.NotCParser.TrueLiteralExpressionContext;
import notc.antlrgen.NotCParser.DoubleLiteralExpressionContext;
import notc.antlrgen.NotCParser.IntLiteralExpressionContext;
import notc.antlrgen.NotCParser.StringLiteralExpressionContext;
import notc.antlrgen.NotCParser.VariableExpressionContext;
import notc.antlrgen.NotCParser.FunctionCallExpressionContext;
import notc.antlrgen.NotCParser.AssignmentExpressionContext;
import notc.antlrgen.NotCParser.ArithmeticExpressionContext;
import notc.antlrgen.NotCParser.PostIncrementExpressionContext;
import notc.antlrgen.NotCParser.PostDecrementExpressionContext;
import notc.antlrgen.NotCParser.PreIncrementExpressionContext;
import notc.antlrgen.NotCParser.PreDecrementExpressionContext;
import notc.antlrgen.NotCParser.ComparisonExpressionContext;
import notc.antlrgen.NotCParser.AndOrExpressionContext;
import notc.antlrgen.NotCParser.ParenthesizedExpressionContext;

import org.antlr.v4.runtime.Token;

import java.util.List;

// Visitor that type checks expressions. Each visit method tries to infer the type of the
// expression it was called on. If a type cannot be inferred, a SemanticException is thrown
// with the offending token and a message. Otherwise, each method annotates the corresponding
// parse tree node with the type (to be used by the code generator). The inferred type is returned
// to the caller because the expression may be part of a larger one whose type depends on it.
class ExpressionChecker extends NotCBaseVisitor<Type> {
    private SymbolTable symTab;

    ExpressionChecker(SymbolTable symTab) {
        this.symTab = symTab;
    }

    // Utility function to check if an expression has some expected type
    void expectType(ExpressionContext expr, Type expected) {
        Type actual = expr.accept(this);
        if (actual == expected) {
            return;
        } else if (actual.isInt() && expected.isDouble()) { // Also acceptable
            expr.i2d = true; // int to double conversion
            return;
        }
        throw new SemanticException(expr.getStart(),
                                    "Expression of type "
                                  + actual.name().toLowerCase()
                                  + " where expression of type "
                                  + expected.name().toLowerCase()
                                  + " was expected");
    }

    // Literal expressions simply have the type of the literal
    @Override
    public Type visitFalseLiteralExpression(FalseLiteralExpressionContext falseLitExpr) {
        falseLitExpr.type = Type.BOOL;
        return falseLitExpr.type;
    }

    @Override
    public Type visitTrueLiteralExpression(TrueLiteralExpressionContext trueLitExpr) {
        trueLitExpr.type = Type.BOOL;
        return trueLitExpr.type;
    }

    @Override
    public Type visitDoubleLiteralExpression(DoubleLiteralExpressionContext doubleLitExpr) {
        doubleLitExpr.type = Type.DOUBLE;
        return doubleLitExpr.type;
    }

    @Override
    public Type visitIntLiteralExpression(IntLiteralExpressionContext intLitExpr) {
        intLitExpr.type = Type.INT;
        return intLitExpr.type;
    }

    @Override
    public Type visitStringLiteralExpression(StringLiteralExpressionContext strLitExpr) {
        strLitExpr.type = Type.STRING;
        return strLitExpr.type;
    }

    // Variable: look its type up
    @Override
    public Type visitVariableExpression(VariableExpressionContext varExpr) {
        varExpr.type = symTab.lookupVar(varExpr.varId);
        return varExpr.type;
    }

    // Check arity against number of arguments and parameter types against argument types
    @Override
    public Type visitFunctionCallExpression(FunctionCallExpressionContext funCall) {
        FunctionType signature = symTab.lookupFun(funCall.id);
        if (signature.arity() != funCall.args.size()) {
            throw new SemanticException(funCall.getStart(),
                                        "Wrong number of arguments in function call");
        }
        int i = 0;
        for (Type t : signature.paramTypes())
            expectType(funCall.args.get(i++), t);
        funCall.type = signature.returnType();
        return signature.returnType();
    }

    // Expression to the right of = must be inferable to the variable's declared type
    @Override
    public Type visitAssignmentExpression(AssignmentExpressionContext assExpr) {
        Type declaredType = symTab.lookupVar(assExpr.varId);
        expectType(assExpr.rhs, declaredType);
        assExpr.type = declaredType;
        return assExpr.type;
    }

    // Arithmetic
    @Override
    public Type visitArithmeticExpression(ArithmeticExpressionContext arithmExpr) {
        Type t1 = arithmExpr.opnd1.accept(this);
        Type t2 = arithmExpr.opnd2.accept(this);
        if (!t1.isNumerical() || !t2.isNumerical()) {
            throw new SemanticException(arithmExpr.getStart(),
                                        "Attempted arithmetic on non-numerical expression");
        }
        if (t1.isDouble() || t2.isDouble()) {
            // Check if an int to double conversion should occur for any operand
            if (t1.isInt())
                arithmExpr.opnd1.i2d = true;
            if (t2.isInt())
                arithmExpr.opnd2.i2d = true;
            arithmExpr.type = Type.DOUBLE;
        } else {
            arithmExpr.type = Type.INT;
        }
        return arithmExpr.type;
    }

    // Utility function to infer and check increments and decrements
    private Type checkIncrDecr(Token varId) {
        Type t = symTab.lookupVar(varId);
        if (t.isNumerical())
            return t;
        throw new SemanticException(varId, "Attempted increment or decrement "
                                         + "of variable that was not int or double");
    }

    // id "++" -> PostIncrExp
    @Override
    public Type visitPostIncrementExpression(PostIncrementExpressionContext postIncrExp) {
        postIncrExp.type = checkIncrDecr(postIncrExp.varId);;
        return postIncrExp.type;
    }

    // id "--" -> PostDecrExp
    @Override
    public Type visitPostDecrementExpression(PostDecrementExpressionContext postDecrExp) {
        postDecrExp.type = checkIncrDecr(postDecrExp.varId);;
        return postDecrExp.type;
    }

    // "++" id -> PreIncrExp
    @Override
    public Type visitPreIncrementExpression(PreIncrementExpressionContext preIncrExp) {
        preIncrExp.type = checkIncrDecr(preIncrExp.varId);
        return preIncrExp.type;
    }

    // "--" id -> PreDecrExp
    @Override
    public Type visitPreDecrementExpression(PreDecrementExpressionContext preDecrExp) {
        preDecrExp.type = checkIncrDecr(preDecrExp.varId);
        return preDecrExp.type;
    }

    // Numerical comparisons: <, > <=, >=, ==, !=
    public Type visitComparisonExpression(ComparisonExpressionContext compExpr) {
        Type t1 = compExpr.opnd1.accept(this);
        Type t2 = compExpr.opnd2.accept(this);
        if (!t1.isNumerical() || !t2.isNumerical())
            throw new SemanticException(compExpr.getStart(), "Ill-typed boolean expression");
        if (t1.isDouble() || t2.isDouble()) {
            if (t1.isInt())
                compExpr.opnd1.i2d = true;
            if (t2.isInt())
                compExpr.opnd2.i2d = true;
        }
        compExpr.type = Type.BOOL;
        return compExpr.type;
    }

    @Override
    public Type visitAndOrExpression(AndOrExpressionContext andOrExpr) {
        Type t1 = andOrExpr.opnd1.accept(this);
        Type t2 = andOrExpr.opnd2.accept(this);
        if (!t1.isBool() || !t2.isBool())
            throw new SemanticException(andOrExpr.getStart(), "Ill-typed boolean expression");
        andOrExpr.type = Type.BOOL;
        return andOrExpr.type;
    }

    @Override
    public Type visitParenthesizedExpression(ParenthesizedExpressionContext paren) {
        Type t = paren.expr.accept(this);
        paren.type = t;
        return t;
    }

}
