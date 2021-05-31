package NotC.TypeChecker;

import NotC.Absyn.FunParam;
import NotC.Absyn.Param;
import NotC.Absyn.Type;
import NotC.TypeResolver;

import java.util.LinkedList;
import java.util.HashMap;

/* Class used to resolve types in the type checking environment:
 * Function ids to signatures and variables to declared types. */
public class SymbolTable {
    
    // Functions
    private HashMap<String,FunType> signatures;
    // Variables (a stack for scoping)
    private LinkedList<HashMap<String,Type>> vars;
    
    SymbolTable() {
        signatures = new HashMap<String,FunType>();
        vars = new LinkedList<HashMap<String,Type>>();
    }
    
    // Used to add all function declarations in a first pass through the program
    void addFun(String id, FunType ft) {
        if (signatures.containsKey(id))
            throw new TypeException("Re-definition of function \"" + id + "\"");
        signatures.put(id, ft);
    }
    
    /* When a function body is to be type checked,
     * add its parameters as local variables. */
    void setContext(LinkedList<Param> paramList) {
        vars.clear();
        pushScope();
        Param.Visitor<FunParam,Void> castToFunParam = (funParam, Void) -> funParam;
        for (Param p : paramList) {
            FunParam funParam = p.accept(castToFunParam, null);
            addVar(funParam.type_, funParam.id_);
        }
    }
    
    void pushScope() {
        vars.push(new HashMap<String,Type>());
    }
    
    void popScope() {
        vars.pollFirst();
    }
    
    /// Start looking in outermost scope and return when a match is found
    Type lookupVar(String id) {
        Type result;
        for (HashMap<String,Type> scope : vars) {
            result = scope.get(id);
            if (result != null)
                return result;
        }
        throw new TypeException("Undefined variable \"" + id + "\"");
    }
    
    // Resolve a function
    FunType lookupFun(String id) {
        FunType result = signatures.get(id);
        if (result != null)
            return result;
        else
            throw new TypeException("Undefined function \"" + id + "\"");
    }
    
    // Add a variable, unless it's already defined in the current scope
    void addVar(Type t, String id) {
        if (TypeResolver.isVoid(t))
            throw new TypeException("Variables cannot have type void");
        HashMap<String,Type> scope = vars.peekFirst();
        if (scope.containsKey(id)) {
            throw new TypeException("Variable \"" + id + "\" already defined");
        }
        scope.put(id, t);
    }
    
}
