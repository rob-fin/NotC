package notc.semantics;

import notc.antlrgen.NotCParser.Type;

import java.util.List;
import java.util.Collections;

// Function types in the semantic analysis environment
class FunctionType {
    private List<Type> paramTypes;
    private Type returnType;

    FunctionType(Type returnType, List<Type> paramTypes) {
        this.returnType = returnType;
        this.paramTypes = paramTypes;
    }

    int arity() {
        return paramTypes.size();
    }

    List<Type> paramTypes() {
        return Collections.unmodifiableList(paramTypes);
    }

    Type returnType() {
        return returnType;
    }

}
