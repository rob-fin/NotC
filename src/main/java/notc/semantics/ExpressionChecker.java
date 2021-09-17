package notc.semantics;

import notc.antlrgen.NotCBaseVisitor;
import notc.antlrgen.NotCParser.Type;
import notc.antlrgen.NotCParser.FunctionHeaderContext;
import notc.antlrgen.NotCParser.VariableDeclarationContext;
import notc.antlrgen.NotCParser.ExpressionContext;
import notc.antlrgen.NotCParser.FalseLiteralExpressionContext;
import notc.antlrgen.NotCParser.TrueLiteralExpressionContext;
import notc.antlrgen.NotCParser.DoubleLiteralExpressionContext;
import notc.antlrgen.NotCParser.IntLiteralExpressionContext;
import notc.antlrgen.NotCParser.StringLiteralExpressionContext;
import notc.antlrgen.NotCParser.VariableExpressionContext;
import notc.antlrgen.NotCParser.FunctionCallExpressionContext;
import notc.antlrgen.NotCParser.IncrementExpressionContext;
import notc.antlrgen.NotCParser.DecrementExpressionContext;
import notc.antlrgen.NotCParser.ArithmeticExpressionContext;
import notc.antlrgen.NotCParser.ComparisonExpressionContext;
import notc.antlrgen.NotCParser.BinaryBooleanExpressionContext;
import notc.antlrgen.NotCParser.AssignmentExpressionContext;
import notc.antlrgen.NotCParser.ParenthesizedExpressionContext;

import org.antlr.v4.runtime.Token;
import com.google.common.collect.Lists;

import java.util.List;

// Infers types of expressions and checks if they make sense.
// The parse tree node of each expression is annotated with its type.
class ExpressionChecker extends NotCBaseVisitor<Type> {
    private final SymbolTable symTab;

    ExpressionChecker(SymbolTable symTab) {
        this.symTab = symTab;
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
            " was expected"
        );
    }

    // The type is returned to the caller because the expression may be a
    // subexpression of another one whose type depends on it.
    Type typeAnnotate(ExpressionContext expr, Type t) {
        expr.type = t;
        return t;
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
        FunctionHeaderContext header = symTab.lookupFunction(funCallExpr.id);
        if (header == null)
            throw new SemanticException(funCallExpr.id, "Undefined function");
        if (header.params.size() != funCallExpr.args.size()) {
            throw new SemanticException(funCallExpr.id,
                "Wrong number of arguments in function call"
            );
        }

        List<Type> paramTypes = Lists.transform(header.params, p -> p.type);
        int paramCount = paramTypes.size();
        for (int i = 0; i < paramCount; ++i)
            expectType(funCallExpr.args.get(i), paramTypes.get(i));

        return typeAnnotate(funCallExpr, header.returnType);
    }

    // Expression to the right of = must be inferable to the variable's declared type
    @Override
    public Type visitAssignmentExpression(AssignmentExpressionContext assExpr) {
        VariableDeclarationContext originalDecl = symTab.resolveVarReference(assExpr.varId);
        Type declaredType = originalDecl.type;
        expectType(assExpr.rhs, declaredType);
        return typeAnnotate(assExpr, declaredType);
    }

    // ++
    @Override
    public Type visitIncrementExpression(IncrementExpressionContext incrExpr) {
        Type t = checkIncrementDecrement(incrExpr.varId);
        return typeAnnotate(incrExpr, t);
    }

    // --
    @Override
    public Type visitDecrementExpression(DecrementExpressionContext decrExpr) {
        Type t = checkIncrementDecrement(decrExpr.varId);
        return typeAnnotate(decrExpr, t);
    }

    private Type checkIncrementDecrement(Token varId) {
        VariableDeclarationContext varDecl = symTab.resolveVarReference(varId);
        if (varDecl.type.isNumerical())
            return varDecl.type;
        throw new SemanticException(varId,
            "Attempted increment or decrement of non-numerical variable"
        );
    }

    // &&, ||
    @Override
    public Type visitBinaryBooleanExpression(BinaryBooleanExpressionContext binBoolExpr) {
        expectType(binBoolExpr.opnd1, Type.BOOL);
        expectType(binBoolExpr.opnd2, Type.BOOL);
        return typeAnnotate(binBoolExpr, Type.BOOL);
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

    // Utility for type checking arithmetic and comparison expressions.
    // Returns the most general of the two types inferred.
    private Type checkBinaryNumerical(ExpressionContext opnd1, ExpressionContext opnd2) {
        Type opnd1Type = opnd1.accept(this);
        Type opnd2Type = opnd2.accept(this);
        if (!opnd1Type.isNumerical() || !opnd2Type.isNumerical()) {
            throw new SemanticException(opnd1.getParent().getStart(),
                "Binary operation expects numerical operands"
            );
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
    public Type visitParenthesizedExpression(ParenthesizedExpressionContext paren) {
        Type t = paren.expr.accept(this);
        return typeAnnotate(paren, t);
    }

}
