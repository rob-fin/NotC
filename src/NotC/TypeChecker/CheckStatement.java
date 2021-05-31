package NotC.TypeChecker;

import NotC.*;
import NotC.Absyn.*;

/* Visitor that type checks statements */
public class CheckStatement implements Stm.Visitor<Void,SymbolTable> {
    
    public static CheckStatement instance = new CheckStatement();
    private CheckStatement() {}
    
    /* Check if an expression has some expected type */
    private static void checkExp(Exp exp, Type expected, SymbolTable symTab) {
        Type actual = exp.accept(InferExpression.instance, symTab);
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
    /* Initialization: Type Id "=" Exp ";" -> StmDeclInit */
    public Void visit(StmDeclInit decl, SymbolTable symTab) {
        checkExp(decl.exp_, decl.type_, symTab);
        symTab.addVar(decl.type_, decl.id_);
        return null;
    }
    /* Declaration: Type [Id] ";" -> StmDeclNoInit */
    public Void visit(StmDeclNoInit decl, SymbolTable symTab) {
        for (String id : decl.listid_)
            symTab.addVar(decl.type_, id);
        return null;
    }
    /* Expression used as statement: Exp ";" -> StmExp */
    public Void visit(StmExp se, SymbolTable symTab) {
        se.exp_.accept(InferExpression.instance, symTab);
        return null;
    }
    /* Return statement: "return" Exp ";" -> StmReturn */
    public Void visit(StmReturn ret, SymbolTable symTab) {
        Type expectedReturn = symTab.lookupFun(symTab.getContext()).getReturnType();
        checkExp(ret.exp_, expectedReturn, symTab);
        return null;
    }
    /* Block statement: "{" [Stm] "}" -> StmBlock */
    public Void visit(StmBlock bl, SymbolTable symTab) {
        symTab.pushScope();
        for (Stm s : bl.liststm_)
            s.accept(this, symTab);
        symTab.popScope();
        return null;
    }
    /* while statement: "while" "(" Exp ")" Stm -> StmWhile */
    public Void visit(StmWhile wh, SymbolTable symTab) {
        checkExp(wh.exp_, new Tbool(), symTab);
        symTab.pushScope();
        wh.stm_.accept(this, symTab);
        symTab.popScope();
        return null;
    }
    /* if-else statement: "if" "(" Exp ")" Stm "else" Stm -> StmIfElse */
    public Void visit(StmIfElse ie, SymbolTable symTab) {
        checkExp(ie.exp_, new Tbool(), symTab);
        symTab.pushScope();
        ie.stm_1.accept(this, symTab);
        ie.stm_2.accept(this, symTab);
        symTab.popScope();
        return null;
    }

    static class InferExpression implements Exp.Visitor<Type,SymbolTable> {
        
        static InferExpression instance = new InferExpression();
        private InferExpression() {}
    
        /* Literals */
        public Type visit(ExpFalse falseLit, SymbolTable symTab) {
            return null;
        }
        public Type visit(ExpTrue trueLit, SymbolTable symTab) {
            return null;
        }
        public Type visit(ExpDouble doubleLit, SymbolTable symTab) {
            return null;
        }
        public Type visit(ExpInt intLit, SymbolTable symTab) {
            return null;
        }
        public Type visit(ExpString strLit, SymbolTable symTab) {
            return null;
        }
        /* Variable */
        public Type visit(ExpId var, SymbolTable symTab) {
            return null;
        }
        /* Function call: Id "(" [Exp] ")" -> ExpFunCall */
        public Type visit(ExpFunCall call, SymbolTable symTab) {
            FunType fun = symTab.lookupFun(call.id_);
            if (fun.getArity() != call.listexp_.size())
                throw new TypeException("Wrong number of arguments to function \"" +
                                        call.id_ + "\"");
            // Check types of arguments
            int i = 0;
            for (Type t : fun.getParamTypes())
                checkExp(call.listexp_.get(i), t, symTab);
            call.setType(fun.getReturnType());
            return null;
        }
        /* Arithmetic operations */
        public Type visit(ExpPostIncr postIncr, SymbolTable symTab) {
            return null;
        }
        public Type visit(ExpPostDecr postDecr, SymbolTable symTab) {
            return null;
        }
        public Type visit(ExpPreIncr preIncr, SymbolTable symTab) {
            return null;
        }
        public Type visit(ExpPreDecr preDecr, SymbolTable symTab) {
            return null;
        }
        public Type visit(ExpMul mul, SymbolTable symTab) {
            return null;
        }
        public Type visit(ExpDiv div, SymbolTable symTab) {
            return null;
        }
        public Type visit(ExpAdd add, SymbolTable symTab) {
            return null;
        }
        public Type visit(ExpSub sub, SymbolTable symTab) {
            return null;
        }
        /* Boolean operations */
        public Type visit(ExpLt lt, SymbolTable symTab) {
            return null;
        }
        public Type visit(ExpGt gt, SymbolTable symTab) {
            return null;
        }
        public Type visit(ExpGEq gEq, SymbolTable symTab) {
            return null;
        }
        public Type visit(ExpLEq lEq, SymbolTable symTab) {
            return null;
        }
        public Type visit(ExpEq eq, SymbolTable symTab) {
            return null;
        }
        public Type visit(ExpNEq nEq, SymbolTable symTab) {
            return null;
        }
        public Type visit(ExpAnd and, SymbolTable symTab) {
            return null;
        }
        public Type visit(ExpOr or, SymbolTable symTab) {
            return null;
        }
        /* Assignment */
        public Type visit(ExpAss ass, SymbolTable symTab) {
            return null;
        }
    }

}

