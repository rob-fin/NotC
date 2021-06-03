package NotC.TypeChecker;

import NotC.Absyn.*;

import static NotC.TypeResolver.isInt;
import static NotC.TypeResolver.isDouble;
import static NotC.TypeResolver.isNumerical;
import static NotC.TypeResolver.isBool;

// Visitor that type checks function definitions.
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
    
    // Add parameters as local variables, then check each statement
    public Void visit(FunDef funDef, Void arg) {
        symTab.setContext(funDef.listparam_);
        expectedReturn = funDef.type_;
        for (Stm s : funDef.liststm_)
            s.accept(checkStm, null);
        return null;
    }
    
    // Check if an expression has some expected type 
    private void checkExp(Exp exp, Type expected) {
        Type actual = exp.accept(inferExp, null);
        if (actual.equals(expected) ||
            isInt(actual) && isDouble(expected)) // Also acceptable
            return;
        
        throw new TypeException("Expression of type " +
                                actual.getClass().getSimpleName() +
                                " where expression of type " +
                                expected.getClass().getSimpleName() +
                                " was expected");
    }
    
    /* Visitor class to type check statements.
     * This involves type checking any constituent expressions. */
    private class CheckStatement implements Stm.Visitor<Void,Void> {
    
        // Initialization: Type Id "=" Exp ";" -> StmDeclInit 
        public Void visit(StmDeclInit decl, Void arg) {
            checkExp(decl.exp_, decl.type_);
            symTab.addVar(decl.type_, decl.id_);
            return null;
        }
        
        // Declaration: Type [Id] ";" -> StmDeclNoInit 
        public Void visit(StmDeclNoInit decl, Void arg) {
            for (String id : decl.listid_)
                symTab.addVar(decl.type_, id);
            return null;
        }
        
        // Expression used as statement: Exp ";" -> StmExp 
        public Void visit(StmExp se, Void arg) {
            se.exp_.accept(inferExp, null);
            return null;
        }
        
        // Return statement: "return" Exp ";" -> StmReturn 
        public Void visit(StmReturn ret, Void arg) {
            checkExp(ret.exp_, expectedReturn);
            return null;
        }
        
        // Block statement: "{" [Stm] "}" -> StmBlock 
        public Void visit(StmBlock bl, Void arg) {
            symTab.pushScope();
            for (Stm s : bl.liststm_)
                s.accept(this, null);
            symTab.popScope();
            return null;
        }
        
        // while statement: "while" "(" Exp ")" Stm -> StmWhile 
        public Void visit(StmWhile wh, Void arg) {
            checkExp(wh.exp_, new Tbool());
            symTab.pushScope();
            wh.stm_.accept(this, null);
            symTab.popScope();
            return null;
        }
        
        // if-else statement: "if" "(" Exp ")" Stm "else" Stm -> StmIfElse 
        public Void visit(StmIfElse ie, Void arg) {
            checkExp(ie.exp_, new Tbool());
            symTab.pushScope();
            ie.stm_1.accept(this, null);
            symTab.popScope();
            symTab.pushScope();
            ie.stm_2.accept(this, null);
            symTab.popScope();
            return null;
        }
    }
    
    /* Visitor class to infer types of expressions.
     * The abstract syntax objects are annotated with their inferred types. 
     * If a method cannot successfully infer a type, it throws a TypeException,
     * so the expressions are also type checked implicitly. */
    private class InferExpression implements Exp.Visitor<Type,Void> {
    
        // Literal expressions has the type of the literal 
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
            return strLit.setType(new Tstring());
        }
        
        // Variables 
        public Type visit(ExpId var, Void arg) {
            Type t = symTab.lookupVar(var.id_);
            return var.setType(t);
        }
        
        // Function calls: Id "(" [Exp] ")" -> ExpFunCall 
        public Type visit(ExpFunCall call, Void arg) {
            FunType fun = symTab.lookupFun(call.id_);
            if (fun.getArity() != call.listexp_.size())
                throw new TypeException("Wrong number of arguments to function \"" +
                                        call.id_ + "\"");
            // Check types of argument expressions against parameters
            int i = 0;
            for (Type t : fun.getParamTypes())
                checkExp(call.listexp_.get(i++), t);
            return call.setType(fun.getReturnType());
        }
        
        // Assignment: Id "=" Exp -> ExpAss 
        public Type visit(ExpAss ass, Void arg) {
            Type t = symTab.lookupVar(ass.id_);
            checkExp(ass.exp_, t);
            return ass.setType(t);
        }
        
        // Utility function to infer and check arithmetic expressions 
        private Type checkArithmetic(Exp op, Exp opnd1, Exp opnd2) {
            Type t1 = opnd1.accept(this, null);
            Type t2 = opnd2.accept(this, null);
            if (isNumerical(t1) && isNumerical(t2)) {
                if (isDouble(t1) || isDouble(t2))
                    return op.setType(new Tdouble());
                else
                    return op.setType(new Tint());
            }
            throw new TypeException("Attempted arithmetic on non-numerical expression");
        }
        
        // Exp "*" Exp -> ExpMul 
        public Type visit(ExpMul mul, Void arg) {
            return checkArithmetic(mul, mul.exp_1, mul.exp_2);
        }
        
        // Exp "/" Exp -> ExpDiv 
        public Type visit(ExpDiv div, Void arg) {
            return checkArithmetic(div, div.exp_1, div.exp_2);
        }
        
        // Exp "+" Exp -> ExpAdd 
        public Type visit(ExpAdd add, Void arg) {
            return checkArithmetic(add, add.exp_1, add.exp_2);
        }
        
        // Exp "-" Exp -> ExpSub 
        public Type visit(ExpSub sub, Void arg) {
            return checkArithmetic(sub, sub.exp_1, sub.exp_2);
        }
        
        // Utility function to infer and check increments and decrements 
        private Type checkIncrDecr(Exp exp, String id) {
            Type t = symTab.lookupVar(id);
            if (isNumerical(t))
                return exp.setType(t);
            throw new TypeException("Attempted increment or decrement of " +
                                    "variable that was not int or double");
        }
        
        // Id "++" -> ExpPostIncr 
        public Type visit(ExpPostIncr postIncr, Void arg) {
            return checkIncrDecr(postIncr, postIncr.id_);
        }
        
        // Id "--" -> ExpPostDecr 
        public Type visit(ExpPostDecr postDecr, Void arg) {
            return checkIncrDecr(postDecr, postDecr.id_);
        }
        
        // "++" Id -> ExpPreIncr 
        public Type visit(ExpPreIncr preIncr, Void arg) {
            return checkIncrDecr(preIncr, preIncr.id_);
        }
        
        // "--" Id -> ExpPreDecr 
        public Type visit(ExpPreDecr preDecr, Void arg) {
            return checkIncrDecr(preDecr, preDecr.id_);
        }
                
        /*  Utility function to check boolean expressions.
         *  <, >, >=, and <= take numerical operands, 
         *  && and || take boolean, == and != take both kinds. */
        private Type checkBoolean(Exp op, Exp opnd1, Exp opnd2,
                                  boolean takesNums, boolean takesBools) {
            
            Type t1 = opnd1.accept(this, null);
            Type t2 = opnd2.accept(this, null);
            if (takesNums && isNumerical(t1) && isNumerical(t2) ||
                takesBools && isBool(t1) && isBool(t2))
                return op.setType(new Tbool());
                
            throw new TypeException("Ill-typed boolean operation");
        }
        
        // Exp "<" Exp -> ExpLt 
        public Type visit(ExpLt lt, Void arg) {
            return checkBoolean(lt, lt.exp_1, lt.exp_2, true, false);
        }
        
        // Exp ">" Exp -> ExpGt 
        public Type visit(ExpGt gt, Void arg) {
            return checkBoolean(gt, gt.exp_1, gt.exp_2, true, false);
        }
        
        // Exp ">=" Exp -> ExpGEq 
        public Type visit(ExpGEq gEq, Void arg) {
            return checkBoolean(gEq, gEq.exp_1, gEq.exp_2, true, false);
        }
        
        // Exp "<=" Exp -> ExpLEq 
        public Type visit(ExpLEq lEq, Void arg) {
            return checkBoolean(lEq, lEq.exp_1, lEq.exp_2, true, false);
        }
        
        // Exp "==" Exp -> ExpEq 
        public Type visit(ExpEq eq, Void arg) {
            return checkBoolean(eq, eq.exp_1, eq.exp_2, true, true);
        }
        
        // Exp "!=" Exp -> ExpNEq 
        public Type visit(ExpNEq nEq, Void arg) {
            return checkBoolean(nEq, nEq.exp_1, nEq.exp_2, true, true);
        }
        
        // Exp "&&" Exp -> ExpAnd 
        public Type visit(ExpAnd and, Void arg) {
            return checkBoolean(and, and.exp_1, and.exp_2, false, true);
        }
        
        // Exp "||" Exp -> ExpOr 
        public Type visit(ExpOr or, Void arg) {
            return checkBoolean(or, or.exp_1, or.exp_2, false, true);
        }
        
    }
    
}

