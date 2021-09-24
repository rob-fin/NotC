package notc.codegen;

import notc.antlrgen.NotCParser.FunctionHeaderContext;
import notc.antlrgen.NotCParser.VariableDeclarationContext;
import notc.antlrgen.NotCParser.Type;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

class JvmMethodTest {

    private JvmMethod method;

    @BeforeEach
    void init() {
        FunctionHeaderContext header = new FunctionHeaderContext(null, 0);
        method = new JvmMethod(header);
    }

    @Test
    void EmitOpWithoutOperand_OpAppearsInDefinition() {
        Opcode op = Opcode.I2D;
        method.emit(op);
        String methodDef = method.collectCode();
        assertTrue(methodDef.contains(op.mnemonic));
    }

    @Test
    void EmitOpWithOperand_OpAppearsInDefinition() {
        Opcode op = Opcode.GOTO;
        String opnd = "Label";
        method.emit(op, opnd);
        String methodDef = method.collectCode();
        assertTrue(methodDef.contains(op.mnemonic + " " + opnd));
    }

    @Test
    void EmitOp_StackDepthUpdated() {
        Opcode op = Opcode.DCONST_1;
        method.emit(op);
        int expectedMaxStack = op.defaultStackChange;
        String methodDef = method.collectCode();
        assertTrue(methodDef.contains(".limit stack " + expectedMaxStack));
    }

    @Test
    void DeepStackGetsPopped_MaxStackValueRetained() {
        int stackItemCount = 10;
        Opcode op = Opcode.ICONST_1;
        for (int i = 0; i < stackItemCount; ++i)
            method.emit(op);
        int expectedMaxStack = stackItemCount * op.defaultStackChange;
        for (int i = 0; i < stackItemCount; ++i)
            method.emit(Opcode.POP);
        String methodDef = method.collectCode();
        assertTrue(methodDef.contains(".limit stack " + expectedMaxStack));
    }

    @Test
    void ReserveMemoryForVars_VarStorageIsSumOfTheirSizes() {
        VariableDeclarationContext stringVar = new VariableDeclarationContext(null, 0);
        VariableDeclarationContext doubleVar = new VariableDeclarationContext(null, 0);
        stringVar.type = Type.STRING;
        doubleVar.type = Type.DOUBLE;
        int expectedVarMemSize = stringVar.type.size() + doubleVar.type.size();
        method.reserveVarMemory(stringVar);
        method.reserveVarMemory(doubleVar);
        String methodDef = method.collectCode();
        assertTrue(methodDef.contains(".limit locals " + expectedVarMemSize));
    }

    @Test
    void StoreThenLoad_InstructionsReferenceSameAddress() {
        // Mixes in some other variables
        for (int i = 0; i < 7; ++i) {
            VariableDeclarationContext varDecl = new VariableDeclarationContext(null, 0);
            varDecl.type = Type.STRING;
            method.reserveVarMemory(varDecl);
        }
        VariableDeclarationContext intVar = new VariableDeclarationContext(null, 0);
        intVar.type = Type.INT;
        method.reserveVarMemory(intVar);
        // Stores integer 1 in variable, then loads it
        method.emit(Opcode.ICONST_1);
        method.emitStore(intVar);
        method.emitLoad(intVar);
        // Tests that emitted instructions refer to same address
        String storeLine = getLineContaining("istore");
        String loadLine = getLineContaining("iload");
        String storeAddress = storeLine.substring(storeLine.lastIndexOf(" ") + 1);
        String loadAddress = loadLine.substring(loadLine.lastIndexOf(" ") + 1);
        assertEquals(storeAddress, loadAddress);
    }

    private String getLineContaining(String searchStr) {
        String methodDef = method.collectCode();
        return methodDef.lines()
            .filter(s -> s.contains(searchStr))
            .findFirst()
            .get();
    }

}