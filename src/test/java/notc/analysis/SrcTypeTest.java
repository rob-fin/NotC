package notc.analysis;

import notc.analysis.NotCParser.TypeContext;
import notc.analysis.NotCParser.BoolTypeContext;
import notc.analysis.NotCParser.DoubleTypeContext;
import notc.analysis.NotCParser.IntTypeContext;
import notc.analysis.NotCParser.StringTypeContext;
import notc.analysis.NotCParser.VoidTypeContext;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
