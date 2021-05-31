// Patch that adds type annotations to abstract syntax trees in BNFC-generated Java code

package NotC.Absyn;

public class TypeAnnotatedNode {

    // Type annotation
    private Type t;
  
    // Called by type checker
    public Type setType(Type t) {
        this.t = t;
        return t;
    }
  
    // Called by code generator
    public Type getType() {
        return t;
    }

}
