package NotC.TypeChecker;

import NotC.Absyn.*;

public class ComparableType {

    public static final Integer Cint    = 0;
    public static final Integer Cbool   = 1;
    public static final Integer Cdouble = 2;
    public static final Integer Cvoid   = 3;
    public static final Integer Cstring = 4;
    
    // Visitor that converts abstract syntax Types to ComparableType members,
    // so that types can be checked for equality without instanceofs
    public static class Get implements Type.Visitor<Integer,Void> {
    
        public Integer visit(Tint t, Void arg) {
            return ComparableType.Cint;
        }
    
        public Integer visit(Tbool t, Void arg) {
            return ComparableType.Cbool;
        }
    
        public Integer visit(Tdouble t, Void arg) {
            return ComparableType.Cdouble;
        }
    
        public Integer visit(Tvoid t, Void arg) {
            return ComparableType.Cvoid;
        }
    
        public Integer visit(Tstring t, Void arg) {
            return ComparableType.Cstring;
        }
    
    }

}
