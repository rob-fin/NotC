package notc.semantics;

import notc.antlrgen.NotCParser.FunctionHeaderContext;
import notc.antlrgen.NotCParser.VariableDeclarationContext;

import org.antlr.v4.runtime.Token;

import java.util.List;
import java.util.ArrayDeque;
import java.util.Map;
import java.util.HashMap;

// Resolves function and variable references to their original declarations
public class SymbolTable {

    private final Map<String,FunctionHeaderContext> headers;

    // Tracks variable scopes during semantic analysis...
    private final ArrayDeque<Map<String,VariableDeclarationContext>> varScopes;

    // ...then saves the resolved references for fast lookup on subsequent parse tree traversals.
    private final Map<Token,VariableDeclarationContext> varDeclarations;

    SymbolTable() {
        headers         = new HashMap<>();
        varScopes       = new ArrayDeque<>();
        varDeclarations = new HashMap<>();
    }

    public FunctionHeaderContext lookupFunction(Token idTok) {
        return headers.get(idTok.getText());
    }

    public VariableDeclarationContext lookupVariable(Token idTok) {
        return varDeclarations.get(idTok);
    }

    void declareFunction(FunctionHeaderContext header) {
        String funName = header.id.getText();
        if (headers.containsKey(funName))
            throw new SemanticException(header.id, "Redefinition of function");
        headers.put(funName, header);
    }

    void declareVariable(VariableDeclarationContext varDecl) {
        if (varDecl.type.isVoid())
            throw new SemanticException(varDecl.id, "Variables cannot have type void");
        Map<String,VariableDeclarationContext> outermostScope = varScopes.peekFirst();
        String varName = varDecl.id.getText();
        if (outermostScope.containsKey(varName))
            throw new SemanticException(varDecl.id, "Redefinition of variable");
        outermostScope.put(varName, varDecl);
    }

    void declareVariables(List<VariableDeclarationContext> varDecls) {
        for (VariableDeclarationContext varDecl : varDecls)
            declareVariable(varDecl);
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
