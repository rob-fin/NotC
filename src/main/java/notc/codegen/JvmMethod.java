package notc.codegen;

import notc.antlrgen.NotCParser.Type;

import org.antlr.v4.runtime.Token;
import org.apache.commons.text.TextStringBuilder;

import java.util.ArrayDeque;
import java.util.Map;
import java.util.HashMap;

// Models a JVM method
class JvmMethod {
    // Updated as instructions are added
    private int nextVarAddr = 0;
    private int currentStack = 0;
    private int maxStack = 0;
    private int nextLabel = 0;

    private String spec; // Part of method header
    private TextStringBuilder body;
    private ArrayDeque<Map<String,Integer>> vars;

    JvmMethod(String spec) {
        this.spec = spec;
        body = new TextStringBuilder();
        vars = new ArrayDeque<Map<String,Integer>>();

    }

    TextStringBuilder collectCode() {
        TextStringBuilder methodDef = new TextStringBuilder();
        methodDef.appendln(indent(1) + "public static Method " + spec);
        methodDef.appendln(indent(2) + "stack " + maxStack + " locals " + nextVarAddr);
        methodDef.appendln(indent(1) + "{");
        methodDef.appendln(body);
        methodDef.appendln(indent(1) + "}");
        return methodDef;
    }

    // Get a new label for jump instructions
    String newLabel() {
        return "L" + nextLabel++;
    }

    private String indent(int level) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < level; i++)
            sb.append("    ");
        return sb.toString();
    }

    // Add an instruction to the body and update stack accordingly
    void addInstruction(String instruction, int stackChange) {
        body.appendln(indent(2) + instruction + ";");
        currentStack += stackChange;
        maxStack = Math.max(maxStack, currentStack);
    }

    // Enter block
    void pushScope() {
        vars.push(new HashMap<String,Integer>());
    }

    // Leave block
    void popScope() {
        vars.pollFirst();
    }

    // Local variables. This is effectively symbol table functionality.

    void addVar(Token idTok, Type t) {
        vars.peekFirst().put(idTok.getText(), nextVarAddr);
        nextVarAddr += t.size();
    }

    // Start looking in outermost scope and return when a match is found
    Integer lookupVar(Token idTok) {
        String varId = idTok.getText();
        Integer addr;
        for (Map<String,Integer> scope : vars) {
            addr = scope.get(varId);
            if (addr != null)
                return addr;
        }
        return null;
    }
}
