package notc.codegen;

import notc.antlrgen.NotCBaseVisitor;
import notc.antlrgen.NotCParser.ProgramContext;
import notc.antlrgen.NotCParser.FunctionDefinitionContext;
import notc.semantics.SymbolTable;

import org.apache.commons.io.IOUtils;
import org.apache.commons.text.TextStringBuilder;

import java.io.InputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
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

        // Makes JVM entry point "void main(String[])" call generated "void main()"
        finalOutput
            .appendln(".class public " + className)
            .appendln(".super java/lang/Object")
            .appendln(".method public static main([Ljava/lang/String;)V")
            .appendln("invokestatic " + className + "/main()V")
            .appendln("return")
            .appendln(".end method");

        // Adds built-in functions
        try (InputStream is = getClass().getResourceAsStream("/builtin_definitions.j")) {
            String builtins = IOUtils.toString(is, StandardCharsets.UTF_8);
            finalOutput.appendln(builtins);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        ExpressionGenerator exprGen = new ExpressionGenerator(symTab);
        FunctionGenerator funGen = new FunctionGenerator(exprGen);

        // Generates JVM methods from parse trees rooted at function definitions
        for (FunctionDefinitionContext funDef : prog.funDefs) {
            JvmMethod method = funGen.generate(funDef);
            finalOutput.appendln(method.collectCode());
        }

        return finalOutput.toString();
    }

}