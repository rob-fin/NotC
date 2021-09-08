package notc.semantics;

import notc.antlrgen.NotCBaseVisitor;
import notc.antlrgen.NotCParser.Type;
import notc.antlrgen.NotCParser.Signature;
import notc.antlrgen.NotCParser.VariableDeclarationContext;
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

// Each visit method tries to infer the type of the expression it was called on. If a type cannot
// be inferred, compilation stops. Otherwise, the corresponding parse tree node is annotated with
// the type for later use by the code generator. The inferred type is returned to the caller
// because the expression may be a subexpression of another one whose type depends on it.
class ExpressionChecker extends NotCBaseVisitor<Type> {
    private final SymbolTable symTab;

    ExpressionChecker(SymbolTable symTab) {
        this.symTab = symTab;
    }

    Type typeAnnotate(ExpressionContext expr, Type t) {
        expr.type = t;
        return t;
    }

    // Checks if an expression has some expected type
    void expectType(ExpressionContext expr, Type expected) {
        Type actual = expr.accept(this);
        if (actual == expected) return;
        if (actual.isConvertibleTo(expected)) {
            expr.runtimeConversion = expected;
            return;
        }
        throw new SemanticException(expr.getStart(),
                                    "Expression of type " + actual +
                                    " where expression of type " + expected +
                                    " was expected");
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

    // Looks up declared type
    @Override
    public Type visitVariableExpression(VariableExpressionContext varExpr) {
        VariableDeclarationContext originalDecl = symTab.resolveVarReference(varExpr.varId);
        return typeAnnotate(varExpr, originalDecl.type);
    }

    // Checks arity against number of arguments and parameter types against argument types
    @Override
    public Type visitFunctionCallExpression(FunctionCallExpressionContext funCallExpr) {
        Signature signature = symTab.lookupFun(funCallExpr.id);
        if (signature == null)
            throw new SemanticException(funCallExpr.id, "Undefined function");
        if (signature.arity() != funCallExpr.args.size()) {
            throw new SemanticException(funCallExpr.id,
                                        "Wrong number of arguments in function call");
        }
        int i = 0;
        for (Type t : signature.paramTypes())
            expectType(funCallExpr.args.get(i++), t); // Guaranteed by parser to be of same length
        return typeAnnotate(funCallExpr, signature.returnType());
    }

    @Override
    public Type visitIncrementDecrementExpression(IncrementDecrementExpressionContext incrDecrExpr) {
        VariableDeclarationContext originalDecl = symTab.resolveVarReference(incrDecrExpr.varId);
        Type declaredType = originalDecl.type;
        if (declaredType.isNumerical())
            return typeAnnotate(incrDecrExpr, declaredType);
        throw new SemanticException(incrDecrExpr.varId,
                                    "Attempted increment or decrement of non-numerical variable");
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
        VariableDeclarationContext originalDecl = symTab.resolveVarReference(assExpr.varId);
        Type declaredType = originalDecl.type;
        expectType(assExpr.rhs, declaredType);
        return typeAnnotate(assExpr, declaredType);
    }

    @Override
    public Type visitParenthesizedExpression(ParenthesizedExpressionContext paren) {
        Type t = paren.expr.accept(this);
        return typeAnnotate(paren, t);
    }

}
