package notc.analysis;

import java.util.List;
import java.util.LinkedList;
import java.util.HashMap;

/* Class used to resolve types in the type checking environment:
 * Function ids to signatures and variables to declared types. */
public class SymbolTable {

    // Functions
    private HashMap<String,FunType> signatures;
    // Variables (a stack for scoping)
    private LinkedList<HashMap<String,SrcType>> vars;

    SymbolTable() {
        signatures = new HashMap<String,FunType>();
        vars = new LinkedList<HashMap<String,SrcType>>();
    }

    // Called in a first pass through the program to add all function declarations
    void addFun(String id, FunType ft) {
        if (signatures.containsKey(id))
            throw new TypeException("Re-definition of function " + id);
        signatures.put(id, ft);
    }

    /* When a function body is to be type checked,
     * add its parameters as local variables. */
    void setContext(List<SrcType> paramTypes, List<String> paramIds) {
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
    SrcType lookupVar(String id) {
        SrcType t;
        for (HashMap<String,SrcType> scope : vars) {
            t = scope.get(id);
            if (t != null)
                return t;
        }
        throw new TypeException("Undefined variable " + id);
    }

    void addVar(SrcType t, String id) {
        if (t.isVoid())
            throw new TypeException("Variables cannot have type void");
        HashMap<String,SrcType> outermostScope = vars.peekFirst();
        if (outermostScope.containsKey(id))
            throw new TypeException("Variable " + id + " already defined");
        outermostScope.put(id, t);
    }

    FunType lookupFun(String id) {
        FunType sig = signatures.get(id);
        if (sig != null)
            return sig;
        throw new TypeException("Undefined function " + id);
    }

}
