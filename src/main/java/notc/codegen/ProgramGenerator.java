package notc.codegen;

import notc.antlrgen.NotCParser.ProgramContext;
import notc.antlrgen.NotCParser.DefContext;
import notc.antlrgen.NotCParser.TypeContext;
import notc.antlrgen.NotCParser.SrcType;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.LinkedList;
import java.io.InputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class ProgramGenerator extends notc.antlrgen.NotCBaseVisitor<String> {
    private String className;
    private LinkedList<String> output;
    SymbolTable symTab;

    public ProgramGenerator(String className) {
        this.className = className;
        output = new LinkedList<String>();
        symTab = new SymbolTable();
    }

    // Entry point for code generator
    @Override
    public String visitProgram(ProgramContext prog) {
        // Add boilerplate
        output.add(".class public " + className);
        output.add(".super java/lang/Object");
        // The JVM entry point main calls the generated main
        output.add(".method public static main([LJava/lang/String;)V");
        output.add("    .limit locals 1");
        output.add("    .limit stack 1");
        output.add("    invokestatic " + className + "/main()I");
        output.add("    pop");
        output.add("    return");
        output.add(".end method");

        // Load resource containing the language's built-in functions implemented
        // as methods in Jasmin assembly. They become part of the class.
        ClassLoader classLoader = getClass().getClassLoader();
        String jasmBoilerplate;
        try (InputStream is = classLoader.getResourceAsStream("builtins.j")) {
            String builtins = IOUtils.toString(is, StandardCharsets.UTF_8);
            output.add(builtins);
        } catch (IOException e) {
            throw new RuntimeException("No intention to handle", e);
        }

        // Add functions to symbol table
        for (DefContext def : prog.def()) {
            StringBuilder JvmSignature = new StringBuilder("(");
            for (TypeContext tCtx : def.params().type())
                JvmSignature.append(JvmTypeSymbol(tCtx.srcType));
            JvmSignature.append(")" + JvmTypeSymbol(def.returnType.srcType));
            symTab.addFun(def.funId, JvmSignature.toString());
        }

        // Generate all functions
        FunctionGenerator funGen = new FunctionGenerator(symTab);
        for (DefContext def : prog.def())
            output.add(def.accept(funGen));

        // Return Jasmin assembly text
        return String.join(System.lineSeparator(), output);
    }

    // Resolves SrcTypes to their corresponding JVM type symbols
    private String JvmTypeSymbol(SrcType srcType) {
        switch(srcType) {
            case BOOL:
                return "Z";
            case VOID:
                return "V";
            case STRING:
                return "LJava/lang/String;";
            case INT:
                return "I";
            case DOUBLE:
                return "D";
            default:
                return "";
        }
    }
}
