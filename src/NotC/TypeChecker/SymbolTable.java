package NotC.TypeChecker;

import java.util.LinkedList;
import java.util.HashMap;
import NotC.*;
import NotC.Absyn.*;

/* Symbol table used to resolve identifiers to their types.
 * Contains function signatures and variables. */
public class SymbolTable {
    
    // Functions (id -> function type)
    private HashMap<String,FunType> signatures;
    // Variables (a stack for scoping)
    private LinkedList<HashMap<String,Type>> vars;
    
    // Id of function whose body currently is being type checked
    private String context;
    
    public String getContext() {
        return context;
    }
        
    
    SymbolTable() {
        signatures = new HashMap<String,FunType>();
        vars = new LinkedList<HashMap<String,Type>>();
    }
    
    void addFun(String id, FunType ft) {
        if (signatures.containsKey(id))
            throw new TypeException("Re-definition of function \"" + id + "\"");
        signatures.put(id, ft);
    }
    
    /* When a new function definition is to be type checked, add its parameters as local variables */
    void setContext(String context, LinkedList<Param> paramList) {
        this.context = context;
        vars.clear();
        pushScope();
        Param.Visitor<FunParam,Void> castToFunParam = (funParam, Void) -> funParam;
        for (Param p : paramList) {
            FunParam funParam = p.accept(castToFunParam, null);
            addVar(funParam.type_, funParam.id_);
        }
    }
    
        
    void pushScope() {
        vars.add(new HashMap<String,Type>());
    }
        
    void popScope() {
        vars.pop();
    }
        
    Type lookupVar(String id) {
        HashMap<String,Type> scope;
        Type result;
        // Start in the outermost scope and return when a match is found
        for (int i = vars.size() - 1; i >= 0; i--) {
            scope = vars.get(i);
            result = scope.get(id);
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
        if (TypeResolver.isVoid(t))
            throw new TypeException("Variables cannot have type void");
            
        HashMap<String,Type> context = vars.peekLast(); // Get outermost context
            
        if (context.containsKey(id)) {
            throw new TypeException("Variable \"" + id + "\" already defined");
        }
            
        context.put(id, t);
    }
    
}
