package notc.semantics;

import notc.antlrgen.NotCParser.Type;

import org.antlr.v4.runtime.Token;

import java.util.Collections;
import java.util.List;
import java.util.ArrayDeque;
import java.util.Map;
import java.util.HashMap;

// Resolves types in the semantic analysis environment:
// Function ids to signatures and variables to declared types.
class SymbolTable {

    // Functions
    private HashMap<String,FunctionType> signatures;
    // Variables (a stack for scoping)
    private ArrayDeque<Map<String,Type>> variables;

    SymbolTable() {
        signatures = new HashMap<String,FunctionType>();
        variables = new ArrayDeque<Map<String,Type>>();
        // Add signatures of built-in functions
        signatures.put("printInt",    new FunctionType(Type.VOID,   List.of(Type.INT)));
        signatures.put("printDouble", new FunctionType(Type.VOID,   List.of(Type.DOUBLE)));
        signatures.put("printString", new FunctionType(Type.VOID,   List.of(Type.STRING)));
        signatures.put("readInt",     new FunctionType(Type.INT,    Collections.emptyList()));
        signatures.put("readDouble",  new FunctionType(Type.DOUBLE, Collections.emptyList()));
        signatures.put("readString",  new FunctionType(Type.STRING, Collections.emptyList()));
    }

    // Called in a first pass through the program to add all function declarations
    void addFun(Token idTok, FunctionType funType) {
        String funId = idTok.getText();
        if (signatures.containsKey(funId))
            throw new SemanticException(idTok, "Redefinition of function");
        signatures.put(funId, funType);
    }

    // When a function body is to be checked, add its parameters as local variables.
    void setContext(List<Type> paramTypes, List<Token> paramIds) {
        variables.clear();
        pushScope();
        // Guaranteed by parser to be of same length
        int paramListLen = paramIds.size();
        for (int i = 0; i < paramListLen; i++)
            addVar(paramTypes.get(i), paramIds.get(i));
    }

    // Enter block
    void pushScope() {
        variables.push(new HashMap<String,Type>());
    }

    // Leave block
    void popScope() {
        variables.pollFirst();
    }

    void addVar(Type t, Token idTok) {
        if (t.isVoid())
            throw new SemanticException(idTok, "Variables cannot have type void");
        Map<String,Type> outermostScope = variables.peekFirst();
        String varId = idTok.getText();
        if (outermostScope.containsKey(varId))
            throw new SemanticException(idTok, "Redefinition of variable");
        outermostScope.put(varId, t);
    }

    // Start looking in outermost scope and return when a match is found
    Type lookupVar(Token idTok) {
        String varId = idTok.getText();
        Type t;
        for (Map<String,Type> scope : variables) {
            t = scope.get(varId);
            if (t != null)
                return t;
        }
        throw new SemanticException(idTok, "Undefined variable");
    }

    // Used when type checking function calls
    FunctionType lookupFun(Token idTok) {
        FunctionType signature = signatures.get(idTok.getText());
        if (signature != null)
            return signature;
        throw new SemanticException(idTok, "Undefined function");
    }

}
