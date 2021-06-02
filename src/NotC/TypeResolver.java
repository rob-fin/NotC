package NotC;

import NotC.Absyn.Type;
import NotC.Absyn.Tbool;
import NotC.Absyn.Tdouble;
import NotC.Absyn.Tint;
import NotC.Absyn.Tstring;
import NotC.Absyn.Tvoid;

/* Utility class for resolving abstract syntax types at run-time */
public class TypeResolver implements Type.Visitor<Boolean,TypeResolver.TypeCode> {
    
    private static final TypeResolver instance = new TypeResolver();
    private TypeResolver() {}
    
    /* Comparable without instanceofs */
    enum TypeCode {
        BOOL,
        DOUBLE,
        INT,
        STRING,
        VOID;
    }
    
    public static boolean isBool(Type t) {
        return t.accept(instance, TypeCode.BOOL);
    }
    
    public static boolean isDouble(Type t) {
        return t.accept(instance, TypeCode.DOUBLE);
    }
    
    public static boolean isInt(Type t) {
        return t.accept(instance, TypeCode.INT);
    }
    
    public static boolean isString(Type t) {
        return t.accept(instance, TypeCode.STRING);
    }
    
    public static boolean isVoid(Type t) {
        return t.accept(instance, TypeCode.VOID);
    }
    
    public static boolean isNumerical(Type t) {
        return t.accept(instance, TypeCode.INT) ||
               t.accept(instance, TypeCode.DOUBLE);
    }
    
    
    public Boolean visit(Tbool t, TypeCode tc) {
        return tc == TypeCode.BOOL;
    }
    
    public Boolean visit(Tdouble t, TypeCode tc) {
        return tc == TypeCode.DOUBLE;
    }
    
    public Boolean visit(Tint t, TypeCode tc) {
        return tc == TypeCode.INT;
    }
    
    public Boolean visit(Tstring t, TypeCode tc) {
        return tc == TypeCode.STRING;
    }
    
    public Boolean visit(Tvoid t, TypeCode tc) {
        return tc == TypeCode.VOID;
    }
    
}
