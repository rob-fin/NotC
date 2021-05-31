package NotC.TypeChecker;

import NotC.Absyn.Program;
import NotC.Absyn.ProgramDefs;
import NotC.Absyn.Def;
import NotC.Absyn.FunDef;
import NotC.Absyn.Type;
import NotC.Absyn.Param;
import NotC.Absyn.Stm;
import NotC.TypeResolver;

import java.util.LinkedList;

/*
 * Visitor class that checks entire programs. Deals with the highest-level
 * constructs in the grammar, some of which have single production rules:
 *  - Program, which means there is only a single visit method here.
 *  - Def and Param, which means we can treat their visitors as functional interfaces
 *    to avoid some visitor boilerplate.
 */
public class CheckProgram implements Program.Visitor<Program,Void> {

    // Entry point for type checker. Check the list of function definitions in two passes:
    // First to collect the id and type signature of each function.
    // Then to check their statements in a context where these functions are declared.
    public Program visit(ProgramDefs defs, Void arg) {

        SymbolTable symTab = new SymbolTable();
        Def.Visitor<FunDef,Void> castToFunDef = (funDef, Void) -> funDef;
        Param.Visitor<Type,Void> getParamType = (funParam, Void) -> funParam.type_;

        // Populate symbol table with functions
        for (Def d : defs.listdef_) {
            FunDef funDef = d.accept(castToFunDef, null);
            Type returnType = funDef.type_;
            String funId = funDef.id_;
            LinkedList<Type> paramTypes = new LinkedList<>();
            for (Param p : funDef.listparam_) {
                Type t = p.accept(getParamType, null);
                paramTypes.add(t);
            }
            FunType signature = new FunType(returnType, paramTypes);
            symTab.addFun(funId, signature);
        }

        FunType mainType = symTab.lookupFun("main");
        if (mainType.getArity() != 0)
            throw new TypeException("Non-empty parameter list in function main");
        Type mainReturn = mainType.getReturnType();
        if (!TypeResolver.isVoid(mainReturn))
            throw new TypeException("Non-void return type declared for function main");

        // Add parameters as local variables, then check statements
        for (Def d : defs.listdef_) {
            FunDef funDef = d.accept(castToFunDef, null);
            symTab.setContext(funDef.id_, funDef.listparam_);
            for (Stm s : funDef.liststm_)
                s.accept(CheckStatement.instance, symTab);
        }

        // Program is well-typed and now has type annotations to be used by the code generator
        return defs;
    }

}
