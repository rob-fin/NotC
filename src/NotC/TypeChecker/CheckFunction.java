package NotC.TypeChecker;

import NotC.*;
import NotC.Absyn.*;

/* Visitor that type checks function definitions */
public class CheckFunction implements Def.Visitor<Void,Void> {
    
    private SymbolTable symTab;
    private Type expectedReturn;
    private CheckStatement checkStm;
    private InferExpression inferExp;
    
    CheckFunction(SymbolTable symTab) {
        this.symTab = symTab;
        checkStm = new CheckStatement();
        inferExp = new InferExpression();
    }
    
    // Add parameters as local variables, then check statements
    public Void visit(FunDef funDef, Void arg) {
        symTab.setContext(funDef.listparam_);
        expectedReturn = funDef.type_;
        for (Stm s : funDef.liststm_)
            s.accept(checkStm, null);
        return null;
    }
    
    /* Check if an expression has some expected type */
    private void checkExp(Exp exp, Type expected) {
        Type actual = exp.accept(inferExp, null);
        if (actual.equals(expected))
            return;
        else if (TypeResolver.isInt(actual) && TypeResolver.isDouble(expected))
            return; // int is acceptable where double is expected
        
        throw new TypeException("Expression of type " +
                                actual.getClass().getSimpleName() +
                                " where expression of type " +
                                expected.getClass().getSimpleName() +
                                " was expected");
    }
    
    private class CheckStatement implements Stm.Visitor<Void,Void> {
        /* Initialization: Type Id "=" Exp ";" -> StmDeclInit */
        public Void visit(StmDeclInit decl, Void arg) {
            checkExp(decl.exp_, decl.type_);
            symTab.addVar(decl.type_, decl.id_);
            return null;
        }
        /* Declaration: Type [Id] ";" -> StmDeclNoInit */
        public Void visit(StmDeclNoInit decl, Void arg) {
            for (String id : decl.listid_)
                symTab.addVar(decl.type_, id);
            return null;
        }
        /* Expression used as statement: Exp ";" -> StmExp */
        public Void visit(StmExp se, Void arg) {
            se.exp_.accept(inferExp, null);
            return null;
        }
        /* Return statement: "return" Exp ";" -> StmReturn */
        public Void visit(StmReturn ret, Void arg) {
            checkExp(ret.exp_, expectedReturn);
            return null;
        }
        /* Block statement: "{" [Stm] "}" -> StmBlock */
        public Void visit(StmBlock bl, Void arg) {
            symTab.pushScope();
            for (Stm s : bl.liststm_)
                s.accept(this, null);
            symTab.popScope();
            return null;
        }
        /* while statement: "while" "(" Exp ")" Stm -> StmWhile */
        public Void visit(StmWhile wh, Void arg) {
            checkExp(wh.exp_, new Tbool());
            symTab.pushScope();
            wh.stm_.accept(this, null);
            symTab.popScope();
            return null;
        }
        /* if-else statement: "if" "(" Exp ")" Stm "else" Stm -> StmIfElse */
        public Void visit(StmIfElse ie, Void arg) {
            checkExp(ie.exp_, new Tbool());
            symTab.pushScope();
            ie.stm_1.accept(this, null);
            ie.stm_2.accept(this, null);
            symTab.popScope();
            return null;
        }
    }

    private class InferExpression implements Exp.Visitor<Type,Void> {
    
        /* Literals */
        public Type visit(ExpFalse falseLit, Void arg) {
            return falseLit.setType(new Tbool());
        }
        public Type visit(ExpTrue trueLit, Void arg) {
            return trueLit.setType(new Tbool());
        }
        public Type visit(ExpDouble doubleLit, Void arg) {
            return doubleLit.setType(new Tdouble());
        }
        public Type visit(ExpInt intLit, Void arg) {
            return intLit.setType(new Tint());
        }
        public Type visit(ExpString strLit, Void arg) {
            return null;
        }
        /* Variable */
        public Type visit(ExpId var, Void arg) {
            return null;
        }
        /* Function call: Id "(" [Exp] ")" -> ExpFunCall */
        public Type visit(ExpFunCall call, Void arg) {
            FunType fun = symTab.lookupFun(call.id_);
            if (fun.getArity() != call.listexp_.size())
                throw new TypeException("Wrong number of arguments to function \"" +
                                        call.id_ + "\"");
            // Check types of arguments
            int i = 0;
            for (Type t : fun.getParamTypes())
                checkExp(call.listexp_.get(i), t);
            call.setType(fun.getReturnType());
            return null;
        }
        /* Arithmetic operations */
        public Type visit(ExpPostIncr postIncr, Void arg) {
            return null;
        }
        public Type visit(ExpPostDecr postDecr, Void arg) {
            return null;
        }
        public Type visit(ExpPreIncr preIncr, Void arg) {
            return null;
        }
        public Type visit(ExpPreDecr preDecr, Void arg) {
            return null;
        }
        public Type visit(ExpMul mul, Void arg) {
            return null;
        }
        public Type visit(ExpDiv div, Void arg) {
            return null;
        }
        public Type visit(ExpAdd add, Void arg) {
            return null;
        }
        public Type visit(ExpSub sub, Void arg) {
            return null;
        }
        /* Boolean operations */
        public Type visit(ExpLt lt, Void arg) {
            return null;
        }
        public Type visit(ExpGt gt, Void arg) {
            return null;
        }
        public Type visit(ExpGEq gEq, Void arg) {
            return null;
        }
        public Type visit(ExpLEq lEq, Void arg) {
            return null;
        }
        public Type visit(ExpEq eq, Void arg) {
            return null;
        }
        public Type visit(ExpNEq nEq, Void arg) {
            return null;
        }
        public Type visit(ExpAnd and, Void arg) {
            return null;
        }
        public Type visit(ExpOr or, Void arg) {
            return null;
        }
        /* Assignment */
        public Type visit(ExpAss ass, Void arg) {
            return null;
        }
    }

}

