package notc.antlrgen;

import notc.antlrgen.NotCParser.Type;
import notc.antlrgen.NotCParser.Signature;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

public class SignatureTest {

    @Test
    void EmptyParamList_Arity0AndReturnEmptyList() {
        Signature sig = new Signature(Type.INT, List.of());
        assertEquals(0, sig.arity());
        assertTrue(sig.paramTypes().isEmpty());
    }

    @Test
    void OneIntParam_Arity1AndReturnListWithOneInt() {
        Type paramType = Type.INT;
        Signature sig = new Signature(Type.INT, List.of(paramType));
        assertEquals(1, sig.arity());
        List<Type> returnedParamTypeList = sig.paramTypes();
        assertEquals(1, returnedParamTypeList.size());
        assertEquals(paramType, returnedParamTypeList.get(0));
    }

    @Test
    void EightParams_Arity8AndReturnListWithSameTypes() {
        List<Type> paramTypesInit = List.of(Type.INT,
                                            Type.DOUBLE,
                                            Type.BOOL,
                                            Type.STRING,
                                            Type.DOUBLE,
                                            Type.STRING,
                                            Type.INT,
                                            Type.INT);
        Signature sig = new Signature(Type.INT, paramTypesInit);
        List<Type> returnedParamTypes = sig.paramTypes();
        assertEquals(paramTypesInit, returnedParamTypes);
    }

    @Test
    void VoidReturnType_ReturnVoid() {
        Signature sig = new Signature(Type.VOID, null);
        assertEquals(Type.VOID, sig.returnType());
    }

    @Test
    void MethodDescriptor_NoParamsBoolReturn() {
        Signature sig = new Signature(Type.BOOL, List.of());
        String actualDescriptor = sig.methodDescriptor();
        assertEquals("\"()Z\"", actualDescriptor);
    }

    @Test
    void MethodDescriptor_ManyInts() {
        Signature sig = new Signature(Type.INT, List.of(Type.INT,
                                                        Type.INT,
                                                        Type.INT,
                                                        Type.INT));
        String actualDescriptor = sig.methodDescriptor();
        assertEquals("\"(IIII)I\"", actualDescriptor);
    }

    @Test
    void MethodDescriptor_MixedTypes() {
        Signature sig = new Signature(Type.VOID, List.of(Type.DOUBLE,
                                                         Type.STRING,
                                                         Type.BOOL,
                                                         Type.INT));
        String actualDescriptor = sig.methodDescriptor();
        assertEquals("\"(DLjava/lang/String;ZI)V\"", actualDescriptor);
    }

}
