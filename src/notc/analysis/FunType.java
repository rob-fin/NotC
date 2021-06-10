package notc.analysis;

import java.util.List;
import java.util.Collections;

// Class describing a function type: parameter type list and return type
public class FunType {
    private List<SrcType> paramTypes;
    private SrcType returnType;
    
    public FunType(SrcType returnType, List<SrcType> paramTypes) {
        this.returnType = returnType;
        this.paramTypes = paramTypes;
    }
    
    public int getArity() {
        return paramTypes.size();
    }
    
    public List<SrcType> getParamTypes() {
        return Collections.unmodifiableList(paramTypes);
    }
    
    public SrcType getReturnType() {
        return returnType;
    }
    
}