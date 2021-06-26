package notc.analysis;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

public class FunTypeTest {

    @Test
    void EmptyParamList_Arity0AndReturnEmptyList() {
        FunType funType = new FunType(null, List.of());
        assertEquals(funType.arity(), 0);
        assertTrue(funType.paramTypes().isEmpty());
    }

    @Test
    void OneIntParam_Arity1AndReturnListWithOneInt() {
        SrcType paramType = SrcType.INT;
        FunType funType = new FunType(null, List.of(paramType));
        assertEquals(funType.arity(), 1);
        List<SrcType> returnedParamTypeList = funType.paramTypes();
        assertEquals(returnedParamTypeList.size(), 1);
        assertEquals(returnedParamTypeList.get(0), paramType);
    }

    @Test
    void EightParams_Arity8AndReturnListWithSameTypes() {
        List<SrcType> paramTypesInit = List.of(SrcType.INT,
                                               SrcType.DOUBLE,
                                               SrcType.BOOL,
                                               SrcType.STRING,
                                               SrcType.DOUBLE,
                                               SrcType.STRING,
                                               SrcType.INT,
                                               SrcType.INT);
        FunType funType = new FunType(null, paramTypesInit);
        List<SrcType> returnedParamTypes = funType.paramTypes();
        assertTrue(paramTypesInit.equals(returnedParamTypes));
    }

    @Test
    void VoidReturnType_ReturnVoid() {
        FunType funType = new FunType(SrcType.VOID, null);
        assertEquals(funType.returnType(), SrcType.VOID);
    }

}
