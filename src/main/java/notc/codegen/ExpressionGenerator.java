package notc.codegen;

import notc.antlrgen.NotCParser.SrcType;
import notc.antlrgen.NotCBaseVisitor;
import notc.antlrgen.NotCParser.ExpContext;
import notc.antlrgen.NotCParser.FalseLitExpContext;
import notc.antlrgen.NotCParser.TrueLitExpContext;
import notc.antlrgen.NotCParser.DoubleLitExpContext;
import notc.antlrgen.NotCParser.IntLitExpContext;
import notc.antlrgen.NotCParser.StringLitExpContext;
import notc.antlrgen.NotCParser.VarExpContext;
import notc.antlrgen.NotCParser.FunCallExpContext;
import notc.antlrgen.NotCParser.AssExpContext;
import java.util.Map;

class ExpressionGenerator extends NotCBaseVisitor<Void> {
    private static Map<String,String> methodSymTab;
    private JvmMethod targetMethod;
    private static ExpressionGenerator instance = new ExpressionGenerator();
    private ExpressionGenerator() {}
    static void setMethodSymTab(Map<String,String> methods) {
        methodSymTab = methods;
    }

    static ExpressionGenerator withTarget(JvmMethod method) {
        instance.targetMethod = method;
        return instance;
    }

    @Override
    public Void visitFalseLitExp(FalseLitExpContext falseLitExp) {
        targetMethod.addInstruction("   ldc 0", 1);
        return null;
    }

    @Override
    public Void visitTrueLitExp(TrueLitExpContext trueLitExp) {
        targetMethod.addInstruction("   ldc 1", 1);
        return null;
    }

    @Override
    public Void visitDoubleLitExp(DoubleLitExpContext doubleLitExp) {
        String srcText = doubleLitExp.DOUBLE_LIT().getText();
        double value = Double.parseDouble(srcText);
        targetMethod.addInstruction("   ldc2_w " + value, 2);
        return null;
    }

    @Override
    public Void visitIntLitExp(IntLitExpContext intLitExp) {
        String srcText = intLitExp.INT_LIT().getText();
        int value = Integer.parseInt(srcText);
        targetMethod.addInstruction("   ldc " + value, 1);
        return null;
    }

    @Override
    public Void visitStringLitExp(StringLitExpContext strLitExp) {
        String strLit = strLitExp.STRING_LIT().getText();
        targetMethod.addInstruction("   ldc " + strLit, 1);
        return null;
    }

    // Variable expression: look up its address and load it
    @Override
    public Void visitVarExp(VarExpContext varExp) {
        int varAddr = targetMethod.lookupVar(varExp.varId);
        if (varExp.typeAnnot.isDouble())
            targetMethod.addInstruction("   dload " + varAddr, 2);
        else if (varExp.typeAnnot.isString())
            targetMethod.addInstruction("   aload " + varAddr, 1);
        else
            targetMethod.addInstruction("   iload " + varAddr, 1);
        return null;
    }

    // Function calls
    @Override
    public Void visitFunCallExp(FunCallExpContext callExp) {
        // Put all arguments on stack and calculate their size
        int argStackSize = 0;
        for (ExpContext arg : callExp.exp()) {
            visit(arg);
            if (arg.typeAnnot.isDouble())
                argStackSize += 2;
            else
                argStackSize += 1; // int or bool arg
        }

        // Return value is left on stack, so calculate its size
        SrcType returnType = callExp.typeAnnot;
        int returnStackSize = 0;
        if (returnType.isDouble())
            returnStackSize = 2;
        else if (!returnType.isVoid()) // int, bool, string
            returnStackSize = 1;

        String invocation = "   invokestatic " + methodSymTab.get(callExp.funId.getText());
        // Arguments are popped, return value is pushed
        targetMethod.addInstruction(invocation, returnStackSize - argStackSize);

        return null;

    }

    // Assignments
    @Override
    public Void visitAssExp(AssExpContext ass) {
        ass.exp().accept(this); // Expression on the right of = goes on stack
        int varAddr = targetMethod.lookupVar(ass.varId);
        String storeInstr;
        String dupInstr;
        int stackSpace;
        SrcType expType = ass.typeAnnot;
        if (expType.isDouble()) {
            storeInstr = "    dstore ";
            dupInstr = "    dup2";
            stackSpace = 2;
        } else if (expType.isString()) {
            storeInstr = "    astore ";
            dupInstr = "    dup";
            stackSpace = 1;
        } else { // ints, bools
            storeInstr = "    istore ";
            dupInstr = "    dup";
            stackSpace = 1;
        }
        // Stored value is value of expression and should be left on stack
        targetMethod.addInstruction(dupInstr, stackSpace);
        targetMethod.addInstruction(storeInstr + varAddr, -stackSpace);
        return null;
    }

}
