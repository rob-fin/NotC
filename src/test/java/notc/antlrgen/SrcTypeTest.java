package notc.antlrgen;

import notc.antlrgen.NotCParser.SrcType;
import notc.antlrgen.NotCParser.TypeContext;
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

// Class SrcType (defined in src/main/antlr/NotC.g4) represents the different
// types in the language and is injected alongside the ANTLR-generated abstract
// syntax classes to make dealing with types in the compiler components easier.
// Unit tests of its utilities are defined below.
class SrcTypeTest {

    TypeContext typeCtx;

    @BeforeEach
    void init() {
        typeCtx = new TypeContext();
    }

    @Test
    void ResolveBool_BoolResolved() {
        TypeContext boolCtx = new BoolTypeContext(typeCtx);
        SrcType resolvedType = SrcType.resolve(boolCtx);
        assertEquals(resolvedType, SrcType.BOOL);
    }

    @Test
    void ResolveDouble_DoubleResolved() {
        TypeContext doubleCtx = new DoubleTypeContext(typeCtx);
        SrcType resolvedType = SrcType.resolve(doubleCtx);
        assertEquals(resolvedType, SrcType.DOUBLE);
    }

    @Test
    void ResolveInt_IntResolved() {
        TypeContext intCtx = new IntTypeContext(typeCtx);
        SrcType resolvedType = SrcType.resolve(intCtx);
        assertEquals(resolvedType, SrcType.INT);
    }

    @Test
    void ResolveString_StringResolved() {
        TypeContext stringCtx = new StringTypeContext(typeCtx);
        SrcType resolvedType = SrcType.resolve(stringCtx);
        assertEquals(resolvedType, SrcType.STRING);
    }

    @Test
    void ResolveVoid_VoidResolved() {
        TypeContext voidCtx = new VoidTypeContext(typeCtx);
        SrcType resolvedType = SrcType.resolve(voidCtx);
        assertEquals(resolvedType, SrcType.VOID);
    }

    @Test
    void NumericalTypesAreNumerical() {
        assertTrue(SrcType.INT.isNumerical());
        assertTrue(SrcType.DOUBLE.isNumerical());
    }

    @Test
    void NonNumericalTypesAreNotNumerical() {
        assertFalse(SrcType.BOOL.isNumerical());
        assertFalse(SrcType.STRING.isNumerical());
        assertFalse(SrcType.VOID.isNumerical());
    }

}
