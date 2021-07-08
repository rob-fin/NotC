package notc.codegen;

import notc.antlrgen.NotCParser.ProgramContext;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.InputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class ProgramGenerator extends notc.antlrgen.NotCBaseVisitor<String> {
    private String className;
    private String jasmBoilerplate;

    public ProgramGenerator(String className) {
        this.className = className;
        ClassLoader classLoader = getClass().getClassLoader();
        try (InputStream is = classLoader.getResourceAsStream("boilerplate.j")) {
            jasmBoilerplate = IOUtils.toString(is, StandardCharsets.UTF_8);
            jasmBoilerplate = StringUtils.replace(jasmBoilerplate, "$_CLASSNAME_$", className);
        } catch (IOException e) {
            throw new RuntimeException("No intention to handle", e);
        }
    }

    @Override
    public String visitProgram(ProgramContext prog) {
        return jasmBoilerplate;
    }

}
