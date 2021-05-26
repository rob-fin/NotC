package NotC.TypeChecker;

import java.util.LinkedList;
import NotC.Absyn.Type;

// Class describing a function type: parameter type list and return type
public class FunType {
    public LinkedList<Type> args;
    public Type val;
    
    FunType(Type val, LinkedList<Type> args) {
        this.val = val;
        this.args = args;
    }
}
