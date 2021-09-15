package notc.codegen;

import notc.antlrgen.NotCParser.Type;
import notc.antlrgen.NotCParser.VariableDeclarationContext;

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

    private int nextVarAddress    = 0;
    private int currentStackDepth = 0;
    private int maxStackDepth     = 0;
    private int nextLabel         = 0;

    private String specification;
    private TextStringBuilder body;
    private Map<VariableDeclarationContext,Integer> varAddresses;

    private JvmMethod() {}

    static JvmMethod from(String specification) {
        JvmMethod method = new JvmMethod();
        method.specification = specification;
        method.body = new TextStringBuilder();
        method.varAddresses = new HashMap<>();
        return method;
    }

    void reserveVarMemory(VariableDeclarationContext varDecl) {
        varAddresses.put(varDecl, nextVarAddress);
        nextVarAddress += varDecl.type.size();
    }

    void reserveVarMemory(List<VariableDeclarationContext> varDecls) {
        for (VariableDeclarationContext decl : varDecls)
            reserveVarMemory(decl);
    }

    void emit(Opcode op, String... operands) {
        addInstruction(op.mnemonic, op.defaultStackChange, operands);
    }

    void emit(Opcode op, int contextualStackChange, String...operands) {
        addInstruction(op.mnemonic, contextualStackChange, operands);
    }

    // Add an instruction to the body and update stack accordingly
    private void addInstruction(String mnemonic, int stackChange, String... operands) {
        body.append(mnemonic);
        for (String o : operands)
            body.append(" " + o);
        body.appendln(";");
        currentStackDepth += stackChange;
        maxStackDepth = Math.max(maxStackDepth, currentStackDepth);
    }

    // Emitters that deal with internal variable addresses
    void emitStore(VariableDeclarationContext varDecl) {
        Opcode storeOp = STORE_OP_BY_TYPE.get(varDecl.type);
        int varAddr = varAddresses.get(varDecl);
        emit(storeOp, Integer.toString(varAddr));
    }

    void emitLoad(VariableDeclarationContext varDecl) {
        Opcode loadOp = LOAD_OP_BY_TYPE.get(varDecl.type);
        int varAddr = varAddresses.get(varDecl);
        emit(loadOp, Integer.toString(varAddr));
    }

    // Get a new label for jump instructions
    String newLabel() {
        return "L" + nextLabel++;
    }

    void insertLabel(String l) {
        body.appendln(l + ":");
    }

    String collectCode() {
        return "public static Method " + specification +
               "stack " + maxStackDepth + " locals " + nextVarAddress +
               "{" + body + "}";
    }

}
