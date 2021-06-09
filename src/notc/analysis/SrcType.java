package notc.analysis;

import notc.analysis.NotCParser.TypeContext;
import notc.analysis.NotCParser.BoolTypeContext;
import notc.analysis.NotCParser.DoubleTypeContext;
import notc.analysis.NotCParser.IntTypeContext;
import notc.analysis.NotCParser.StringTypeContext;
import notc.analysis.NotCParser.VoidTypeContext;

import java.util.HashMap;

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

    public static SrcType resolve(TypeContext ctx) {
        if (ctxCache.containsKey(ctx))
            return ctxCache.get(ctx);
        SrcType t = resolver.visit(ctx);
        ctxCache.put(ctx, t);
        return t;
    }
    
    private static TypeVisitor resolver = new TypeVisitor();
    private static HashMap<TypeContext,SrcType> ctxCache = new HashMap<>();
    
    /*public static boolean isBool(TypeContext ctx) {
        return ctxCache.getOrDefault(ctx, resolve(ctx)) == BOOL;
    }
    
    public static boolean isDouble(TypeContext ctx) {
        return ctxCache.getOrDefault(ctx, resolve(ctx)) == DOUBLE;
    }
    
    public static boolean isInt(TypeContext ctx) {
        return ctxCache.getOrDefault(ctx, resolve(ctx)) == INT;
    }
    
    public static boolean isString(TypeContext ctx) {
        return ctxCache.getOrDefault(ctx, resolve(ctx)) == STRING;
    }
    
    public static boolean isVoid(TypeContext ctx) {
        return ctxCache.getOrDefault(ctx, resolve(ctx)) == VOID;
    }
    
    public static boolean isNumerical(TypeContext ctx) {
        return isInt(ctx) || isDouble(ctx);
    }
    
    public static boolean areSame(TypeContext ctx1, TypeContext ctx2) {
        return ctxCache.getOrDefault(ctx1, resolve(ctx1)) ==
               ctxCache.getOrDefault(ctx2, resolve(ctx2));
    }*/
    
        
    
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
    
