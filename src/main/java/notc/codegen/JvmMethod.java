package notc.codegen;

import notc.antlrgen.NotCParser.Type;
import notc.antlrgen.NotCParser.VariableDeclarationContext;
import notc.antlrgen.NotCParser.FunctionHeaderContext;

import org.apache.commons.text.TextStringBuilder;

import java.util.List;
import java.util.Map;
import java.util.HashMap;

// Instantiated as code generator targets. Tracks state of method being generated.
class JvmMethod {

    private static final Map<Type,Opcode> STORE_OP_BY_TYPE = Map.of(
        Type.BOOL,   Opcode.ISTORE,
        Type.INT,    Opcode.ISTORE,
        Type.STRING, Opcode.ASTORE,
        Type.DOUBLE, Opcode.DSTORE
    );

    private static final Map<Type,Opcode> LOAD_OP_BY_TYPE = Map.of(
        Type.BOOL,   Opcode.ILOAD,
        Type.INT,    Opcode.ILOAD,
        Type.STRING, Opcode.ALOAD,
        Type.DOUBLE, Opcode.DLOAD
    );

    private final String specification;
    private final TextStringBuilder body;
    private final Map<VariableDeclarationContext,Integer> varAddresses;

    private int nextVarAddress;
    private int currentStackDepth;
    private int maxStackDepth;
    private int nextLabel;

    JvmMethod(FunctionHeaderContext header) {
        specification = header.specification;
        body = new TextStringBuilder();
        varAddresses = new HashMap<>();
        reserveVarMemory(header.params);
    }

    void reserveVarMemory(VariableDeclarationContext varDecl) {
        varAddresses.put(varDecl, nextVarAddress);
        nextVarAddress += varDecl.type.size();
    }

    void reserveVarMemory(List<VariableDeclarationContext> varDecls) {
        for (VariableDeclarationContext decl : varDecls)
            reserveVarMemory(decl);
    }

    void emit(Opcode op) {
        emit(op, null);
    }

    void emit(Opcode op, String operand) {
        addInstruction(op.mnemonic, operand);
        updateStack(op.defaultStackChange);
    }

    void emitLoad(VariableDeclarationContext varDecl) {
        Opcode loadOp = LOAD_OP_BY_TYPE.get(varDecl.type);
        Integer varAddr = varAddresses.get(varDecl);
        emit(loadOp, varAddr.toString());
    }

    void emitStore(VariableDeclarationContext varDecl) {
        Opcode storeOp = STORE_OP_BY_TYPE.get(varDecl.type);
        Integer varAddr = varAddresses.get(varDecl);
        emit(storeOp, varAddr.toString());
    }

    // Arguments should be generated before call
    void emitCall(FunctionHeaderContext callee) {
        Opcode op = Opcode.INVOKESTATIC;
        addInstruction(op.mnemonic, callee.fqn);
        int returnStackSize = callee.returnType.size();
        int paramsStackSize = callee.params.stream()
            .map(p -> p.type)
            .mapToInt(Type::size)
            .sum();
        // Arguments are popped, return value is pushed
        int stackChange = returnStackSize - paramsStackSize;
        updateStack(stackChange);
    }

    private void addInstruction(String mnemonic, String operand) {
        body.append(mnemonic);
        if (operand != null)
            body.append(" ").append(operand);
        body.appendNewLine();
    }

    private void updateStack(int stackChange) {
        currentStackDepth += stackChange;
        if (currentStackDepth < 0)
            throw new IllegalStateException("Negative stack depth");
        maxStackDepth = Math.max(maxStackDepth, currentStackDepth);
    }

    // Returns a new label for jump instructions
    String newLabel() {
        return "L" + nextLabel++;
    }

    void insertLabel(String label) {
        body.append(label).appendln(":");
    }

    String collectCode() {
        return String.join(System.lineSeparator(),
            ".method public static " + specification,
            ".limit locals " + nextVarAddress,
            ".limit stack " + maxStackDepth,
            body,
            ".end method"
        );
    }

}