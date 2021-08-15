package notc.codegen;

import notc.antlrgen.NotCBaseVisitor;
import notc.antlrgen.NotCParser.ProgramContext;
import notc.antlrgen.NotCParser.FunctionDefinitionContext;
import notc.antlrgen.NotCParser.StatementContext;
import notc.semantics.SymbolTable;

import org.apache.commons.io.IOUtils;
import org.apache.commons.text.TextStringBuilder;

import java.io.InputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class ProgramGenerator extends NotCBaseVisitor<String> {
    private SymbolTable symTab;
    private String className;

    public ProgramGenerator(SymbolTable symTab, String className) {
        this.symTab = symTab;
        this.className = className;
    }

    // Entry point for code generator
    @Override
    public String visitProgram(ProgramContext prog) {
        TextStringBuilder finalOutput = new TextStringBuilder();
        finalOutput.appendln("super public class " + className + "{");

        // Load resource containing the language's built-in functions
        // implemented as methods in Jasm assembly. They become part of the class.
        // Also contains JVM entry point main, which calls the generated main.
        ClassLoader classLoader = getClass().getClassLoader();
        try (InputStream is = classLoader.getResourceAsStream("boilerplate.jasm")) {
            finalOutput.appendln(IOUtils.toString(is, StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new RuntimeException("No intention to handle", e);
        }

        StatementGenerator stmGen = new StatementGenerator(symTab);

        // Generate JVM methods from parse trees rooted at function definitions
        for (FunctionDefinitionContext funDef : prog.funDefs) {
            String name = funDef.id.getText();
            String descriptor = funDef.signature.methodDescriptor();
            String specification = name + ":" + descriptor;
            JvmMethod targetMethod = new JvmMethod(specification);
            stmGen.setTarget(targetMethod);
            targetMethod.pushScope();
            int paramListLen = funDef.signature.paramTypes().size();
            // (Guaranteed by parser to be of same length)
            for (int i = 0; i < paramListLen; i++)
                targetMethod.addVar(funDef.paramIds.get(i), funDef.signature.paramTypes().get(i));
            for (StatementContext stm : funDef.body)
                stm.accept(stmGen);
            // Assembler requires void method bodies to end with return (language does not)
            if (funDef.signature.returnType().isVoid())
                targetMethod.addInstruction("return", 0);
            finalOutput.appendln(targetMethod.collectCode());
        }

        finalOutput.appendln("}");

        return finalOutput.toString();
    }

}
