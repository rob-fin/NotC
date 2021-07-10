package notc.codegen;

import notc.antlrgen.NotCBaseVisitor;
import notc.antlrgen.NotCParser.FalseLitExpContext;
import notc.antlrgen.NotCParser.TrueLitExpContext;
import notc.antlrgen.NotCParser.DoubleLitExpContext;
import notc.antlrgen.NotCParser.IntLitExpContext;
import notc.antlrgen.NotCParser.StringLitExpContext;
import notc.antlrgen.NotCParser.VarExpContext;

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

}
