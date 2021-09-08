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
    private final SymbolTable symTab;
    private final String className;

    public ProgramGenerator(SymbolTable symTab, String className) {
        this.symTab = symTab;
        this.className = className;
    }

    // Entry point for code generator
    @Override
    public String visitProgram(ProgramContext prog) {
        TextStringBuilder finalOutput = new TextStringBuilder();
        finalOutput.appendln("super public class " + className + "{");

        // Load resource containing built-in functions.
        // Also contains JVM entry point main, which calls the generated main.
        ClassLoader classLoader = getClass().getClassLoader();
        try (InputStream is = classLoader.getResourceAsStream("boilerplate.jasm")) {
            finalOutput.appendln(IOUtils.toString(is, StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new RuntimeException("Shouldn't happen because file exists", e);
        }

        ExpressionGenerator exprGen = new ExpressionGenerator(symTab);
        FunctionGenerator funGen = new FunctionGenerator(exprGen);

        // Generate JVM methods from parse trees rooted at function definitions
        for (FunctionDefinitionContext funDef : prog.funDefs) {
            JvmMethod method = funGen.generate(funDef);
            finalOutput.appendln(method.collectCode());
        }

        finalOutput.appendln("}");

        return finalOutput.toString();
    }

}
