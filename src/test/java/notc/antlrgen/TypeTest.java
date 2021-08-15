package notc.antlrgen;

import notc.antlrgen.NotCParser.Type;
import notc.antlrgen.NotCParser.TypeTokenContext;
import notc.antlrgen.NotCParser.BoolTypeContext;
import notc.antlrgen.NotCParser.DoubleTypeContext;
import notc.antlrgen.NotCParser.IntTypeContext;
import notc.antlrgen.NotCParser.StringTypeContext;
import notc.antlrgen.NotCParser.VoidTypeContext;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

// Enum Type (defined in src/main/antlr/NotC.g4) represents the different
// types in the language and is injected alongside the ANTLR-generated abstract
// syntax classes to make dealing with types in the compiler components easier.
class TypeTest {

    private final TypeTokenContext typeCtx = new TypeTokenContext();

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

    @Test
    void Conversion_IntIsConvertibleToDouble() {
        assertTrue(Type.INT.isConvertibleTo(Type.DOUBLE));
    }

    @Test
    void Conversion_IntIsConvertibleToBool() {
        assertTrue(Type.INT.isConvertibleTo(Type.BOOL));
    }

    @Test
    void Conversion_DoubleIsConvertibleToBool() {
        assertTrue(Type.DOUBLE.isConvertibleTo(Type.BOOL));
    }

    @Test
    void Conversion_DoubleIsConvertibleToInt() {
        assertTrue(Type.DOUBLE.isConvertibleTo(Type.INT));
    }

    @Test
    void Conversion_BoolIsConvertibleToInt() {
        assertTrue(Type.BOOL.isConvertibleTo(Type.INT));
    }

    @Test
    void Conversion_BoolIsConvertibleToDouble() {
        assertTrue(Type.BOOL.isConvertibleTo(Type.DOUBLE));
    }

    @Test
    void Conversion_BoolIsNotConvertibleToString() {
        assertFalse(Type.BOOL.isConvertibleTo(Type.STRING));
    }

    @Test
    void Conversion_DoubleIsNotConvertibleToVoid() {
        assertFalse(Type.DOUBLE.isConvertibleTo(Type.VOID));
    }

    @Test
    void Conversion_VoidNotConvertible() {
        assertFalse(Type.VOID.isConvertibleTo(Type.STRING));
        assertFalse(Type.VOID.isConvertibleTo(Type.BOOL));
        assertFalse(Type.VOID.isConvertibleTo(Type.DOUBLE));
        assertFalse(Type.VOID.isConvertibleTo(Type.INT));
    }

    @Test
    void Conversion_StringNotConvertible() {
        assertFalse(Type.STRING.isConvertibleTo(Type.VOID));
        assertFalse(Type.STRING.isConvertibleTo(Type.BOOL));
        assertFalse(Type.STRING.isConvertibleTo(Type.DOUBLE));
        assertFalse(Type.STRING.isConvertibleTo(Type.INT));
    }

    @Test
    void TypeDescriptor_Bool() {
        assertEquals("Z", Type.BOOL.descriptor());
    }

    @Test
    void TypeDescriptor_Void() {
        assertEquals("V", Type.VOID.descriptor());
    }

    @Test
    void TypeDescriptor_String() {
        assertEquals("Ljava/lang/String;", Type.STRING.descriptor());
    }

    @Test
    void TypeDescriptor_Int() {
        assertEquals("I", Type.INT.descriptor());
    }

    @Test
    void TypeDescriptor_Double() {
        assertEquals("D", Type.DOUBLE.descriptor());
    }

    @Test
    void Size_Double() {
        assertEquals(2, Type.DOUBLE.size());
    }

    @Test
    void Size_Void() {
        assertEquals(0, Type.VOID.size());
    }

    @Test
    void Size_Bool() {
        assertEquals(1, Type.BOOL.size());
    }

    @Test
    void Size_String() {
        assertEquals(1, Type.STRING.size());
    }

    @Test
    void Size_Int() {
        assertEquals(1, Type.INT.size());
    }

}

