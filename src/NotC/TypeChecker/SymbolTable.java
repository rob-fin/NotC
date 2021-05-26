package NotC.TypeChecker;

import java.util.LinkedList;
import java.util.HashMap;
import NotC.*;
import NotC.Absyn.*;

/* Environment class for the type checker.
 * Contains function signatures, variable contexts,
 * and methods for getting and setting type information. */
class SymbolTable {

    // Functions
    private HashMap<String,FunType> signatures;
    // Variables (a stack for scoping)
    private LinkedList<HashMap<String,Type>> contexts;

    SymbolTable() {
        signatures = new HashMap<String,FunType>();
        contexts = new LinkedList<HashMap<String,Type>>();
    }
        
    void pushContext() {
        contexts.add(new HashMap<String,Type>());
    }
        
    void popContext() {
        contexts.pop();
    }
        
    Type lookupVar(String id) {
        HashMap<String,Type> context;
        Type result;
        // Start in the outermost context and return when a match is found
        for (int i = contexts.size() - 1; i >= 0; i--) {
            context = contexts.get(i);
            result = context.get(id);
            if (result != null)
                return result;
        }
        throw new TypeException("Undefined variable \"" + id + "\"");
    }
        
    FunType lookupFun(String id) {
        FunType result = signatures.get(id);
        if (result != null)
            return result;
        else
            throw new TypeException("Undefined function \"" + id + "\"");
    }

    void addVar(Type t, String id) {
        if (t.accept(new ComparableType.Get(), null) == ComparableType.Cvoid)
            throw new TypeException("Variables cannot have type void");
            
        HashMap<String,Type> context = contexts.peekLast(); // Get outermost context
            
        if (context.containsKey(id)) {
            throw new TypeException("Variable \"" + id + "\" already defined");
        }
            
        context.put(id, t);
    }

    void addFun(String id, FunType ft) {
        if (signatures.containsKey(id))
            throw new TypeException("Re-definition of function \"" + id + "\"");
        signatures.put(id, ft);
    }
}
