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
        Signature funType = new Signature(null, List.of());
        assertEquals(0, funType.arity());
        assertTrue(funType.paramTypes().isEmpty());
    }

    @Test
    void OneIntParam_Arity1AndReturnListWithOneInt() {
        Type paramType = Type.INT;
        Signature funType = new Signature(null, List.of(paramType));
        assertEquals(1, funType.arity());
        List<Type> returnedParamTypeList = funType.paramTypes();
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
        Signature funType = new Signature(null, paramTypesInit);
        List<Type> returnedParamTypes = funType.paramTypes();
        assertTrue(paramTypesInit.equals(returnedParamTypes));
    }

    @Test
    void VoidReturnType_ReturnVoid() {
        Signature funType = new Signature(Type.VOID, null);
        assertEquals(Type.VOID, funType.returnType());
    }

}
