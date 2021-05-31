package NotC.TypeChecker;

import NotC.Absyn.Type;
import java.util.List;
import java.util.LinkedList;
import java.util.Collections;

// Class describing a function type: parameter type list and return type
public class FunType {
    private LinkedList<Type> paramTypes;
    private Type returnType;
    
    FunType(Type returnType, LinkedList<Type> paramTypes) {
        this.returnType = returnType;
        this.paramTypes = paramTypes;
    }
    
    public int getArity() {
        return paramTypes.size();
    }
    
    public List<Type> getParamTypes() {
        return Collections.unmodifiableList(paramTypes);
    }
    
    public Type getReturnType() {
        return returnType;
    }
    
}
