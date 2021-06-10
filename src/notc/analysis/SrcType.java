package notc.analysis;

import notc.analysis.NotCParser.TypeContext;
import notc.analysis.NotCParser.BoolTypeContext;
import notc.analysis.NotCParser.DoubleTypeContext;
import notc.analysis.NotCParser.IntTypeContext;
import notc.analysis.NotCParser.StringTypeContext;
import notc.analysis.NotCParser.VoidTypeContext;

import java.util.HashMap;

/* Enum representing the different types in the language.
 * ANTLR-generated abstract syntax objects are a bit awkward to compare to each other,
 * so instances of this class are used in the symbol tables and type annotations. */
public enum SrcType {
        BOOL,
        STRING,
        VOID,
        INT,
        DOUBLE;

    public boolean isBool() {
        return compareTo(BOOL) == 0;
    }

    public boolean isString() {
        return compareTo(STRING) == 0;
    }

    public boolean isVoid() {
        return compareTo(VOID) == 0;
    }

    public boolean isInt() {
        return compareTo(INT) == 0;
    }

    public boolean isDouble() {
        return compareTo(DOUBLE) == 0;
    }

    public boolean isNumerical() {
        return compareTo(INT) >= 0;
    }

    /* Resolve abstract syntax types to instances
     * of this enum at run-time using an utility visitor */
    public static SrcType resolve(TypeContext ctx) {
        if (ctxCache.containsKey(ctx))
            return ctxCache.get(ctx); // TODO: is a type really ever resolved twice?
        SrcType t = resolver.visit(ctx);
        ctxCache.put(ctx, t);
        return t;
    }

    private static TypeVisitor resolver = new TypeVisitor();
    private static HashMap<TypeContext,SrcType> ctxCache = new HashMap<>();

    static class TypeVisitor extends NotCBaseVisitor<SrcType> {

        @Override
        public SrcType visitBoolType(BoolTypeContext ctx) {
            return BOOL;
        }

        @Override
        public SrcType visitDoubleType(DoubleTypeContext ctx) {
            return DOUBLE;
        }

        @Override
        public SrcType visitIntType(IntTypeContext ctx) {
            return INT;
        }

        @Override
        public SrcType visitStringType(StringTypeContext ctx) {
            return STRING;
        }

        @Override
        public SrcType visitVoidType(VoidTypeContext ctx) {
            return VOID;
        }

    }
}

