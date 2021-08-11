package notc.codegen;

import notc.antlrgen.NotCBaseVisitor;
import notc.antlrgen.NotCParser.ProgramContext;
import notc.antlrgen.NotCParser.FunctionDefinitionContext;
import notc.antlrgen.NotCParser.Type;

import org.antlr.v4.runtime.tree.ParseTreeProperty;
import org.apache.commons.io.IOUtils;
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

        // Put built-ins in a symbol table: id -> fully qualified JVM specification
        Map<String,String> methodSymTab = new HashMap<>();
        methodSymTab.put("printInt",    "printInt:\"(I)V\"");
        methodSymTab.put("readInt",     "readInt:\"()I\"");
        methodSymTab.put("printDouble", "printDouble:\"(D)V\"");
        methodSymTab.put("readDouble",  "readDouble:\"()D\"");
        methodSymTab.put("printString", "printString:\"(Ljava/lang/String;)V\"");
        methodSymTab.put("readString",  "readString:\"()Ljava/lang/String;\"");

        // Then add the functions defined by the program
        for (FunctionDefinitionContext funDef : prog.funDefs) {
            String funId = funDef.id.getText();
            StringBuilder sb = new StringBuilder(funId + ":\"(");
            for (Type t : funDef.signature.paramTypes())
                sb.append(JvmTypeSymbol(t));
            sb.append(")" + JvmTypeSymbol(funDef.signature.returnType()) + "\"");
            String methodSpec = sb.toString();
            String qualifiedMethod = methodSpec;
            methodSymTab.put(funId, qualifiedMethod);
        }

        // The symbol table for methods is needed when generating function
        // call expressions, so make it a class member of ExpressionGenerator.
        ExpressionGenerator.setMethodSymTab(methodSymTab);

        // Generate JVM methods from the function definitions of the program
        for (FunctionDefinitionContext funDef : prog.funDefs) {
            String spec = methodSymTab.get(funDef.id.getText());
            JvmMethod method = JvmMethod.from(funDef, spec);
            TextStringBuilder methodOutput = method.collectCode();
            finalOutput.appendln(methodOutput);
        }

        finalOutput.appendln("}");

        // Return assembly text
        return finalOutput.toString();
    }

    // Resolves Types to their corresponding JVM type symbols
    private String JvmTypeSymbol(Type t) {
        switch (t) {
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
