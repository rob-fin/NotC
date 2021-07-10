package notc.codegen;

import notc.antlrgen.NotCBaseVisitor;

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
}
