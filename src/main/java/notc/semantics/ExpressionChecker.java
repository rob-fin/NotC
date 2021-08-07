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
import notc.antlrgen.NotCParser.IncrementDecrementExpressionContext;
import notc.antlrgen.NotCParser.ArithmeticExpressionContext;
import notc.antlrgen.NotCParser.ComparisonExpressionContext;
import notc.antlrgen.NotCParser.AndOrExpressionContext;
import notc.antlrgen.NotCParser.AssignmentExpressionContext;
import notc.antlrgen.NotCParser.ParenthesizedExpressionContext;

import org.antlr.v4.runtime.Token;

import java.util.List;

// Visitor that type checks expressions. Each visit method tries to infer the type of the
// expression it was called on. If a type cannot be inferred, a SemanticException is thrown with
// the offending token and a message. Otherwise, each method annotates the corresponding parse
// tree node with the type (to be used by the code generator). The inferred type is returned to the
// caller because the expression may be a subexpression of another one whose type depends on it.
class ExpressionChecker extends NotCBaseVisitor<Type> {
    private SymbolTable symTab;

    ExpressionChecker(SymbolTable symTab) {
        this.symTab = symTab;
    }

    Type typeAnnotate(ExpressionContext expr, Type t) {
        expr.type = t;
        return t;
    }

    // Utility to check if an expression has some expected type
    void expectType(ExpressionContext expr, Type expected) {
        Type actual = expr.accept(this);
        if (actual == expected) return;
        if (actual.isConvertibleTo(expected)) {
            expr.runtimeConversion = expected;
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
        return typeAnnotate(falseLitExpr, Type.BOOL);
    }

    @Override
    public Type visitTrueLiteralExpression(TrueLiteralExpressionContext trueLitExpr) {
        return typeAnnotate(trueLitExpr, Type.BOOL);
    }

    @Override
    public Type visitDoubleLiteralExpression(DoubleLiteralExpressionContext doubleLitExpr) {
        return typeAnnotate(doubleLitExpr, Type.DOUBLE);
    }

    @Override
    public Type visitIntLiteralExpression(IntLiteralExpressionContext intLitExpr) {
        return typeAnnotate(intLitExpr, Type.INT);
    }

    @Override
    public Type visitStringLiteralExpression(StringLiteralExpressionContext strLitExpr) {
        return typeAnnotate(strLitExpr, Type.STRING);
    }

    // Look up declared type
    @Override
    public Type visitVariableExpression(VariableExpressionContext varExpr) {
        return typeAnnotate(varExpr, symTab.lookupVar(varExpr.varId));
    }

    // Check arity against number of arguments and parameter types against argument types
    @Override
    public Type visitFunctionCallExpression(FunctionCallExpressionContext funCallExpr) {
        FunctionType signature = symTab.lookupFun(funCallExpr.id);
        if (signature.arity() != funCallExpr.args.size()) {
            throw new SemanticException(funCallExpr.getStart(),
                                        "Wrong number of arguments in function call");
        }
        int i = 0;
        for (Type t : signature.paramTypes())
            expectType(funCallExpr.args.get(i++), t);
        return typeAnnotate(funCallExpr, signature.returnType());
    }

    @Override
    public Type visitIncrementDecrementExpression(IncrementDecrementExpressionContext incrDecrExpr) {
        Type t = symTab.lookupVar(incrDecrExpr.varId);
        if (t.isNumerical())
            return typeAnnotate(incrDecrExpr, t);
        throw new SemanticException(incrDecrExpr.varId,
                                    "Attempted increment or decrement "
                                  + "of variable that was not int or double");
    }

    // Utility for type checking arithmetic and comparison expressions
    private Type checkBinaryNumerical(ExpressionContext opnd1, ExpressionContext opnd2) {
        Type opnd1Type = opnd1.accept(this);
        Type opnd2Type = opnd2.accept(this);
        if (!opnd1Type.isNumerical() || !opnd2Type.isNumerical()) {
            throw new SemanticException(opnd1.getParent().getStart(),
                                        "Binary operation expects numerical operands");
        }
        // Both operands should be generated as the largest type
        Type mostGeneralType = opnd1Type.compareTo(opnd2Type) < 0 ? opnd1Type : opnd2Type;
        if (opnd1Type != mostGeneralType)
            opnd1.runtimeConversion = mostGeneralType;
        if (opnd2Type != mostGeneralType)
            opnd2.runtimeConversion = mostGeneralType;
        return mostGeneralType;
    }

    // +, -, *, /, %
    @Override
    public Type visitArithmeticExpression(ArithmeticExpressionContext arithmExpr) {
        Type overallType = checkBinaryNumerical(arithmExpr.opnd1, arithmExpr.opnd2);
        return typeAnnotate(arithmExpr, overallType);
    }

    // Numerical comparisons: <, > <=, >=, ==, !=
    @Override
    public Type visitComparisonExpression(ComparisonExpressionContext compExpr) {
        checkBinaryNumerical(compExpr.opnd1, compExpr.opnd2);
        return typeAnnotate(compExpr, Type.BOOL);
    }

    @Override
    public Type visitAndOrExpression(AndOrExpressionContext andOrExpr) {
        Type t1 = andOrExpr.opnd1.accept(this);
        Type t2 = andOrExpr.opnd2.accept(this);
        if (!t1.isBool() || !t2.isBool())
            throw new SemanticException(andOrExpr.getStart(), "Ill-typed boolean expression");
        return typeAnnotate(andOrExpr, Type.BOOL);
    }

    // Expression to the right of = must be inferable to the variable's declared type
    @Override
    public Type visitAssignmentExpression(AssignmentExpressionContext assExpr) {
        Type declaredType = symTab.lookupVar(assExpr.varId);
        expectType(assExpr.rhs, declaredType);
        return typeAnnotate(assExpr, declaredType);
    }

    @Override
    public Type visitParenthesizedExpression(ParenthesizedExpressionContext paren) {
        Type t = paren.expr.accept(this);
        return typeAnnotate(paren, t);
    }

}
