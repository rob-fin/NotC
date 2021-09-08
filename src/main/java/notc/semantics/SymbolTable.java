package notc.semantics;

import notc.antlrgen.NotCParser.Type;
import notc.antlrgen.NotCParser.Signature;
import notc.antlrgen.NotCParser.VariableDeclarationContext;

import org.antlr.v4.runtime.Token;

import java.util.Collections;
import java.util.List;
import java.util.ArrayDeque;
import java.util.Map;
import java.util.HashMap;

// Resolves function references to signatures
// and variable references to original declarations.
public class SymbolTable {

    private final Map<String,Signature> signatures;

    // Tracks variable scopes during semantic analysis...
    private final ArrayDeque<Map<String,VariableDeclarationContext>> varScopes;

    // ...then saves the resolved references for fast lookup on subsequent parse tree traversals.
    private final Map<Token,VariableDeclarationContext> varDeclarations;

    SymbolTable() {
        signatures      = new HashMap<>();
        varScopes       = new ArrayDeque<>();
        varDeclarations = new HashMap<>();
        // Built-in functions
        signatures.put("printInt",    new Signature(Type.VOID,   List.of(Type.INT)));
        signatures.put("printDouble", new Signature(Type.VOID,   List.of(Type.DOUBLE)));
        signatures.put("printString", new Signature(Type.VOID,   List.of(Type.STRING)));
        signatures.put("readInt",     new Signature(Type.INT,    Collections.emptyList()));
        signatures.put("readDouble",  new Signature(Type.DOUBLE, Collections.emptyList()));
        signatures.put("readString",  new Signature(Type.STRING, Collections.emptyList()));
    }

    public Signature lookupFun(Token idTok) {
        return signatures.get(idTok.getText());
    }

    public VariableDeclarationContext lookupVar(Token idTok) {
        return varDeclarations.get(idTok);
    }

    void addFun(Token idTok, Signature signature) {
        String funName = idTok.getText();
        if (signatures.containsKey(funName))
            throw new SemanticException(idTok, "Redefinition of function");
        signatures.put(funName, signature);
    }

    void addVar(VariableDeclarationContext var) {
        if (var.type.isVoid())
            throw new SemanticException(var.id, "Variables cannot have type void");
        Map<String,VariableDeclarationContext> outermostScope = varScopes.peekFirst();
        String varName = var.id.getText();
        if (outermostScope.containsKey(varName))
            throw new SemanticException(var.id, "Redefinition of variable");
        outermostScope.put(varName, var);
    }

    void addVars(List<VariableDeclarationContext> vars) {
        for (VariableDeclarationContext var : vars)
            addVar(var);
    }

    // Starts looking in outermost scope and returns when a match is found
    VariableDeclarationContext resolveVarReference(Token idTok) {
        String varName = idTok.getText();
        VariableDeclarationContext varDecl;
        for (Map<String,VariableDeclarationContext> scope : varScopes) {
            varDecl = scope.get(varName);
            if (varDecl != null) {
                varDeclarations.put(idTok, varDecl);
                return varDecl;
            }
        }
        throw new SemanticException(idTok, "Undefined variable");
    }

    // Called when a new function definition is to be analyzed
    void resetScope() {
        varScopes.clear();
        pushScope();
    }

    void pushScope() {
        varScopes.push(new HashMap<>());
    }

    void popScope() {
        varScopes.pollFirst();
    }

}
