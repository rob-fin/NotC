package notc.codegen;

import notc.antlrgen.NotCParser.VariableDeclarationContext;

import org.apache.commons.text.TextStringBuilder;

import java.util.List;
import java.util.Map;
import java.util.HashMap;

// Instantiated as code generator targets. Tracks state of method being generated.
class JvmMethod {
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

    int addressOf(VariableDeclarationContext varDecl) {
        return varAddresses.get(varDecl);
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
