package notc.codegen;

import notc.antlrgen.NotCBaseVisitor;
import notc.antlrgen.NotCParser.ProgramContext;
import notc.antlrgen.NotCParser.FunctionDefinitionContext;
import notc.antlrgen.NotCParser.Type;

import org.antlr.v4.runtime.tree.ParseTreeProperty;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.TextStringBuilder;
import com.google.common.collect.Lists;

import java.util.Map;
import java.util.List;
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

        // Then add the functions defined by the program
        for (FunctionDefinitionContext funDef : prog.funDefs) {
            String funId = funDef.id.getText();
            StringBuilder sb = new StringBuilder(funId + "(");
            List<Type> paramTypes = Lists.transform(funDef.paramTypes, t -> Type.resolve(t));
            for (Type t : paramTypes)
                sb.append(JvmTypeSymbol(t));
            sb.append(")" + JvmTypeSymbol(Type.resolve(funDef.returnType)));
            String methodSpec = sb.toString();
            String qualifiedMethod = className + "/" + methodSpec;
            methodSymTab.put(funId, qualifiedMethod);
            JvmSpecs.put(funDef, methodSpec); // Needed again when generating method headers
        }

        // The symbol table for methods does not change and is only needed when generating
        // function call expressions, so make it a class member of ExpressionGenerator.
        ExpressionGenerator.setMethodSymTab(methodSymTab);

        // Generate JVM methods from the function definitions of the program
        for (FunctionDefinitionContext funDef : prog.funDefs) {
            String spec = JvmSpecs.get(funDef);
            JvmMethod method = JvmMethod.from(funDef, spec);
            TextStringBuilder methodOutput = method.collectCode();
            finalOutput.append(methodOutput);
        }

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
