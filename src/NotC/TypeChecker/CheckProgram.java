package NotC.TypeChecker;

import NotC.Absyn.*;

public class CheckProgram implements Program.Visitor<Void,Void> {
    public Void visit(NotC.Absyn.PDefs p, Void arg) {
        // Build function type symbol table
        SymbolTable symTab = new SymbolTable();
        for (Def d : p.listdef_) {
            d.accept(new BuildSigSymTab(), symTab);
        }
        return null;
    }
    
    static class BuildSigSymTab implements Def.Visitor<Void,SymbolTable> {
        public Void visit(NotC.Absyn.FunDef p, SymbolTable symTab) {
            //p.type_.accept(new TypeVisitor<R,A>(), arg);
            //p.id_;
            //p.parlist_.accept(new ParListVisitor<R,A>(), arg);
            //p.funbody_.accept(new FunBodyVisitor<R,A>(), arg);
            return null;
        }
    }
}
