package notc.codegen;

import notc.antlrgen.NotCParser.ProgramContext;

public class ProgramGenerator extends notc.antlrgen.NotCBaseVisitor<String> {
    private String className;

    public ProgramGenerator(String className) {
        this.className = className;
    }

    @Override
    public String visitProgram(ProgramContext prog) {
        return jasmBoilerplate();
    }

    /* Makes the JVM entry point call the generated "int main()".
     * For now, also include implementation of it. */
    private String jasmBoilerplate() {
        final String NL = System.lineSeparator();
        return
            "super public class " + className                                   + NL +
            "{                                                                " + NL +
            "    public Method \"<init>\":\"()V\"                             " + NL +
            "        stack 1 locals 1                                         " + NL +
            "    {                                                            " + NL +
            "        aload_0;                                                 " + NL +
            "        invokespecial Method java/lang/Object.\"<init>\":\"()V\";" + NL +
            "        return;                                                  " + NL +
            "    }                                                            " + NL +
            "    public static Method main:\"([Ljava/lang/String;)V\"         " + NL +
            "        stack 1 locals 1                                         " + NL +
            "    {                                                            " + NL +
            "        invokestatic Method " + className + ".main:\"()I\";      " + NL +
            "        pop;                                                     " + NL +
            "        return;                                                  " + NL +
            "    }                                                            " + NL +
            "    public static Method main:\"()I\"                           " + NL +
            "       stack 1 locals 1                                          " + NL +
            "    {                                                            " + NL +
            "        ldc 1;                                                   " + NL +
            "        ireturn;                                                 " + NL +
            "    }                                                            " + NL +
            "}                                                                " + NL;
    }
}
