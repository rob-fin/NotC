// Patch that adds type annotations to abstract syntax trees in BNFC-generated Java code

package NotC.Absyn;

public class TypeAnnotatedNode {

    // Type annotation
    private Type type;
  
    // Called by type checker
    public Type setType(Type type) {
      this.type = type;
      return type;
    }
  
    // Called by code generator
    public Type getType() {
        return type;
    }

}
