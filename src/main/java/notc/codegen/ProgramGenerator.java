package notc.codegen;

import notc.antlrgen.NotCBaseVisitor;
import notc.antlrgen.NotCParser.ProgramContext;
import notc.antlrgen.NotCParser.DefContext;
import notc.antlrgen.NotCParser.TypeContext;
import notc.antlrgen.NotCParser.SrcType;

import org.antlr.v4.runtime.tree.ParseTreeProperty;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.TextStringBuilder;

import java.util.Map;
import java.util.HashMap;
import java.io.InputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class ProgramGenerator extends NotCBaseVisitor<String> {
    private String className;

    public ProgramGenerator(String className) {
        this.className = className;
    }

    // Entry point for code generator
    @Override
    public String visitProgram(ProgramContext prog) {
        TextStringBuilder finalOutput = new TextStringBuilder();

        // Load resource containing the language's built-in functions
        // implemented as methods in Jasmin assembly. They become part of the class.
        // Also contains JVM entry point main, which calls the generated main.
        ClassLoader classLoader = getClass().getClassLoader();
        try (InputStream is = classLoader.getResourceAsStream("boilerplate.j")) {
            String boilerplate = IOUtils.toString(is, StandardCharsets.UTF_8);
            finalOutput.append(StringUtils.replace(boilerplate, "$CLASSNAME$", className));
        } catch (IOException e) {
            throw new RuntimeException("No intention to handle", e);
        }

        // Put built-ins in a symbol table: id -> fully qualified JVM specification
        Map<String,String> methodSymTab = new HashMap<>();
        methodSymTab.put("printInt",    className + "/printInt(I)V");
        methodSymTab.put("readInt",     className + "/readInt()I");
        methodSymTab.put("printDouble", className + "/printDouble(D)V");
        methodSymTab.put("readDouble",  className + "/readDouble()D");
        methodSymTab.put("printString", className + "/printString(Ljava/lang/String;)V");
        methodSymTab.put("readString",  className + "/readString()Ljava/lang/String;");

        ParseTreeProperty<String> JvmSpecs = new ParseTreeProperty<>();

        // Then add the functions defined in the program
        for (DefContext def : prog.def()) {
            String funId = def.funId.getText();
            StringBuilder sb = new StringBuilder(funId + "(");
            for (TypeContext tCtx : def.params().type())
                sb.append(JvmTypeSymbol(tCtx.srcType));
            sb.append(")" + JvmTypeSymbol(def.returnType.srcType));
            String methodSpec = sb.toString();
            String qualifiedMethod = className + "/" + methodSpec;
            methodSymTab.put(funId, qualifiedMethod);
            JvmSpecs.put(def, methodSpec); // Needed again when generating method headers
        }

        // The symbol table for methods does not change and is only needed when generating
        // function call expressions, so make it a class member of ExpressionGenerator.
        ExpressionGenerator.setMethodSymTab(methodSymTab);

        // Generate JVM methods from the function definitions of the program
        for (DefContext def : prog.def()) {
            String spec = JvmSpecs.get(def);
            JvmMethod method = JvmMethod.from(def, spec);
            TextStringBuilder methodOutput = method.collectCode();
            finalOutput.append(methodOutput);
        }

        // Return assembly text
        return finalOutput.toString();
    }

    // Resolves SrcTypes to their corresponding JVM type symbols
    private String JvmTypeSymbol(SrcType srcType) {
        switch(srcType) {
            case BOOL:
                return "Z";
            case VOID:
                return "V";
            case STRING:
                return "Ljava/lang/String;";
            case INT:
                return "I";
            case DOUBLE:
                return "D";
            default:
                return "";
        }
    }
}
