package notc.codegen;

import notc.antlrgen.NotCParser.SrcType;
import notc.antlrgen.NotCParser.DefContext;
import notc.antlrgen.NotCParser.StmContext;

import org.antlr.v4.runtime.tree.TerminalNode;
import org.antlr.v4.runtime.Token;
import org.apache.commons.text.TextStringBuilder;
import com.google.common.collect.Lists;

import java.util.List;
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

    private String JvmSpec; // Part of method header
    private TextStringBuilder body;
    private ArrayDeque<HashMap<String,Integer>> vars;

    private JvmMethod(String JvmSpec) {
        this.JvmSpec = JvmSpec;
        body = new TextStringBuilder();
        vars = new ArrayDeque<HashMap<String,Integer>>();
    }

    // Instantiates a model of a JVM method from a parse tree rooted at a function
    // definition, and a specification of its name and signature in JVM format
    static JvmMethod of(DefContext def, String JvmSpec) {
        JvmMethod method = new JvmMethod(JvmSpec);
        // Add paramters as local variables
        List<SrcType> paramTypes = Lists.transform(def.params().type(),
                                                   tCtx -> tCtx.srcType);
        List<Token> paramIds = Lists.transform(def.params().ID(),
                                               TerminalNode::getSymbol);
        method.pushScope();
        int paramListLen = paramIds.size();
        // (Guaranteed by parser to be of same length)
        for (int i = 0; i < paramListLen; i++)
            method.addVar(paramIds.get(i), paramTypes.get(i));
        StatementGenerator stmGen = StatementGenerator.withTarget(method);
        // Generate the statements
        for (StmContext stm : def.stm())
            stm.accept(stmGen);
        return method;
    }

    // After instantiating the class, this can be called to get the generated code
    TextStringBuilder collectCode() {
        TextStringBuilder methodDef = new TextStringBuilder();
        methodDef.appendln(".method public static " + JvmSpec);
        methodDef.appendln("    .limit locals " + nextVarAddr);
        methodDef.appendln("    .limit stack " + maxStack);
        methodDef.appendln(body);
        methodDef.appendln(".end method");
        return methodDef;
    }

    // Adds new label for jump instructions
    String newLabel() {
        return "L" + nextLabel++ + ":";
    }

    // Add an instruction to the output and update stack accordingly
    void addInstruction(String instruction, int stackChange) {
        body.appendln(instruction);
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

    // Double is the only type that takes up two "slots" on the stack
    void addVar(Token idTok, SrcType t) { System.out.println("hejj");
        vars.peekFirst().put(idTok.getText(), nextVarAddr);
        nextVarAddr += t.isDouble() ? 2 : 1;
    }

    // Start looking in outermost scope and return when a match is found
    Integer lookupVar(Token idTok) {
        String varId = idTok.getText();
        Integer addr;
        for (HashMap<String,Integer> scope : vars) {
            addr = scope.get(varId);
            if (addr != null)
                return addr;
        }
        return null; // Avoids compilation error
    }
}
