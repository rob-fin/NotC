package notc.semantics;

import notc.antlrgen.NotCParser.SrcType;

import org.antlr.v4.runtime.Token;

import java.util.Collections;
import java.util.List;
import java.util.LinkedList;
import java.util.HashMap;

/* Class used to resolve types in the semantic analysis environment:
 * Function ids to signatures and variables to declared types. */
public class SymbolTable {

    // Functions
    private HashMap<String,FunType> signatures;
    // Variables (a stack for scoping)
    private LinkedList<HashMap<String,SrcType>> vars;

    SymbolTable() {
        signatures = new HashMap<String,FunType>();
        vars = new LinkedList<HashMap<String,SrcType>>();
        // Add signatures of built-in functions
        signatures.put("printInt",    new FunType(SrcType.VOID,    List.of(SrcType.INT)));
        signatures.put("printDouble", new FunType(SrcType.VOID,    List.of(SrcType.DOUBLE)));
        signatures.put("printString", new FunType(SrcType.VOID,    List.of(SrcType.STRING)));
        signatures.put("readInt",     new FunType(SrcType.INT,     Collections.emptyList()));
        signatures.put("readDouble",  new FunType(SrcType.DOUBLE,  Collections.emptyList()));
        signatures.put("readString",  new FunType(SrcType.STRING,  Collections.emptyList()));
    }

    // Called in a first pass through the program to add all function declarations
    void addFun(Token idTok, FunType ft) {
        String funId = idTok.getText();
        if (signatures.containsKey(funId))
            throw new SemanticException(idTok, "Redefinition of function");
        signatures.put(funId, ft);
    }

    // When a function body is to be checked, add its parameters as local variables.
    void setContext(List<SrcType> paramTypes, List<Token> paramIds) {
        vars.clear();
        pushScope();
        // Guaranteed by parser to be of same length
        int paramListLen = paramIds.size();
        for (int i = 0; i < paramListLen; i++)
            addVar(paramTypes.get(i), paramIds.get(i));
    }

    // Enter block
    void pushScope() {
        vars.push(new HashMap<String,SrcType>());
    }

    // Leave block
    void popScope() {
        vars.pollFirst();
    }

    // Start looking in outermost scope and return when a match is found
    SrcType lookupVar(Token idTok) {
        String varId = idTok.getText();
        SrcType t;
        for (HashMap<String,SrcType> scope : vars) {
            t = scope.get(varId);
            if (t != null)
                return t;
        }
        throw new SemanticException(idTok, "Undefined variable");
    }

    void addVar(SrcType t, Token idTok) {
        if (t.isVoid())
            throw new SemanticException(idTok, "Variables cannot have type void");
        HashMap<String,SrcType> outermostScope = vars.peekFirst();
        String varId = idTok.getText();
        if (outermostScope.containsKey(varId))
            throw new SemanticException(idTok, "Redefinition of variable");
        outermostScope.put(varId, t);
    }

    FunType lookupFun(Token idTok) {
        FunType sig = signatures.get(idTok.getText());
        if (sig != null)
            return sig;
        throw new SemanticException(idTok, "Undefined function");
    }

}
