package notc.antlrgen;

import notc.antlrgen.NotCParser.Type;
import notc.antlrgen.NotCParser.TypeTokenContext;
import notc.antlrgen.NotCParser.BoolTypeContext;
import notc.antlrgen.NotCParser.DoubleTypeContext;
import notc.antlrgen.NotCParser.IntTypeContext;
import notc.antlrgen.NotCParser.StringTypeContext;
import notc.antlrgen.NotCParser.VoidTypeContext;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

// Enum Type (defined in src/main/antlr/NotC.g4) represents the different
// types in the language and is injected alongside the ANTLR-generated abstract
// syntax classes to make dealing with types in the compiler components easier.
class TypeTest {

    TypeTokenContext typeCtx;

    @BeforeEach
    void init() {
        typeCtx = new TypeTokenContext();
    }

    @Test
    void ResolveBool_BoolResolved() {
        TypeTokenContext boolCtx = new BoolTypeContext(typeCtx);
        Type resolvedType = Type.resolve(boolCtx);
        assertEquals(Type.BOOL, resolvedType);
    }

    @Test
    void ResolveDouble_DoubleResolved() {
        TypeTokenContext doubleCtx = new DoubleTypeContext(typeCtx);
        Type resolvedType = Type.resolve(doubleCtx);
        assertEquals(Type.DOUBLE, resolvedType);
    }

    @Test
    void ResolveInt_IntResolved() {
        TypeTokenContext intCtx = new IntTypeContext(typeCtx);
        Type resolvedType = Type.resolve(intCtx);
        assertEquals(Type.INT, resolvedType);
    }

    @Test
    void ResolveString_StringResolved() {
        TypeTokenContext stringCtx = new StringTypeContext(typeCtx);
        Type resolvedType = Type.resolve(stringCtx);
        assertEquals(Type.STRING, resolvedType);
    }

    @Test
    void ResolveVoid_VoidResolved() {
        TypeTokenContext voidCtx = new VoidTypeContext(typeCtx);
        Type resolvedType = Type.resolve(voidCtx);
        assertEquals(Type.VOID, resolvedType);
    }

    @Test
    void NumericalTypesAreNumerical() {
        assertTrue(Type.INT.isNumerical());
        assertTrue(Type.DOUBLE.isNumerical());
        assertTrue(Type.BOOL.isNumerical());
    }

    @Test
    void NonNumericalTypesAreNotNumerical() {
        assertFalse(Type.STRING.isNumerical());
        assertFalse(Type.VOID.isNumerical());
    }

}
