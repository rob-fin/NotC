package notc.codegen;

import notc.antlrgen.NotCParser.VariableDeclarationContext;

import org.apache.commons.text.TextStringBuilder;

import java.util.List;
import java.util.Map;
import java.util.HashMap;

// Models JVM methods. Instantiated as code generator targets.
class JvmMethod {
    // Updated as instructions are added
    private int nextVarAddress = 0;
    private int currentStack   = 0;
    private int maxStack       = 0;
    private int nextLabel      = 0;

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

    // Add an instruction to the body and update stack accordingly
    void addInstruction(String instruction, int stackChange) {
        body.appendln(instruction + ";");
        currentStack += stackChange;
        maxStack = Math.max(maxStack, currentStack);
    }

    void reserveVarMemory(VariableDeclarationContext varDecl) {
        varAddresses.put(varDecl, nextVarAddress);
        nextVarAddress += varDecl.type.size();
    }

    void reserveVarMemory(List<VariableDeclarationContext> varDecls) {
        for (VariableDeclarationContext decl : varDecls)
            reserveVarMemory(decl);
    }

    int addressOf(VariableDeclarationContext varDecl) {
        return varAddresses.get(varDecl);
    }

    // Get a new label for jump instructions
    String newLabel() {
        return "L" + nextLabel++;
    }

    String collectCode() {
        return "public static Method " + specification +
               "stack " + maxStack + " locals " + nextVarAddress +
               "{" + body + "}";
    }

}
