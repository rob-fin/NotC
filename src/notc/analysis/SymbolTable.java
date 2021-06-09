package notc.analysis;

import notc.analysis.NotCParser.*;
import org.antlr.v4.runtime.tree.TerminalNode;

import java.util.stream.Collectors;
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
    
    
    void pushScope() {
        vars.push(new HashMap<String,SrcType>());
    }
        
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
    
    // Add a variable, unless it's already defined in the current scope
    void addVar(SrcType t, String id) {
        if (t == SrcType.VOID)
            throw new TypeException("Variables cannot have type void");
        HashMap<String,SrcType> scope = vars.peekFirst(); // Get outermost scope
        if (scope.containsKey(id)) {
            throw new TypeException("Variable " + id + " already defined");
        }
        scope.put(id, t);
    }
    
    // Resolve a function
    FunType lookupFun(String id) {
        FunType sig = signatures.get(id);
        if (sig != null)
            return sig;
        throw new TypeException("Undefined function " + id);
    }

    
}
