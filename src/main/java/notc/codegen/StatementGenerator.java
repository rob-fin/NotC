package notc.codegen;

import notc.antlrgen.NotCBaseVisitor;

class StatementGenerator extends NotCBaseVisitor<Void> {
    private ExpressionGenerator expGen;
    private JvmMethod targetMethod;
    private static StatementGenerator instance = new StatementGenerator();
    private StatementGenerator() {}

    static StatementGenerator withTarget(JvmMethod method) {
        instance.targetMethod = method;
        instance.expGen = ExpressionGenerator.withTarget(method);
        return instance;
    }
}
