package notc;

import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.Arguments;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.assertj.core.api.Assertions.assertThat;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import com.google.common.math.DoubleMath;
import com.google.common.primitives.Doubles;
import com.google.common.primitives.Ints;
import com.github.stefanbirkner.systemlambda.SystemLambda;
import org.jooq.lambda.Unchecked;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Path;
import java.nio.file.Files;
import static java.nio.charset.StandardCharsets.UTF_8;
import java.util.function.Function;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

// Runs the compiler with test program source files: ones that should compile, ones that
// should be rejected by the parser, and ones that should be rejected during semantic analysis.
// Semantic errors rejected by the parser and syntax errors rejected during semantic analysis
// are considered failed tests.
// The valid programs that compiled are then executed and their outputs checked.
@TestInstance(Lifecycle.PER_CLASS)
@TestMethodOrder(OrderAnnotation.class)
class ProgramsTest {
    private final ClassLoader cl = ProgramsTest.class.getClassLoader();
    // Test directories
    private final Path syntaxErrors   = Path.of(cl.getResource("syntax_errors").getFile());
    private final Path semanticErrors = Path.of(cl.getResource("semantic_errors").getFile());
    private final Path validPrograms  = Path.of(cl.getResource("valid_programs").getFile());


    // Compilation tests

    @Order(1)
    @ParameterizedTest
    @MethodSource("provideTestSources")
    void compileProgram(Path srcFile) throws Exception {
        String className = FilenameUtils.getBaseName(srcFile.toString());
        Path testDir = srcFile.getParent();
        String sysErr = SystemLambda.tapSystemErr( () ->
            new Compiler().compile(srcFile, className, testDir)
        );
        Function<String,Boolean> oracle = oracleByTestDirectory.get(testDir);
        assertTrue(oracle.apply(sysErr), "Compiler wrote the following to System.err: " + sysErr);
    }

    private Stream<Arguments> provideTestSources() {
        return Stream.of(syntaxErrors, semanticErrors, validPrograms)
            .map(Unchecked.function(Files::list))
            .flatMap(pathStream -> pathStream)
            .filter(path -> path.toString().endsWith(".notc"))
            .map(Arguments::of);
    }

    private final Map<Path,Function<String,Boolean>> oracleByTestDirectory = Map.of(
        syntaxErrors,   sysErr -> sysErr.startsWith("Syntax error"),
        semanticErrors, sysErr -> sysErr.startsWith("Semantic error"),
        validPrograms,  String::isEmpty
    );


    // Execution tests

    @Order(2)
    @ParameterizedTest
    @MethodSource("provideCompiledPrograms")
    void runCompiledProgram(Path classFile) throws IOException, InterruptedException {
        String programPath = classFile.getParent().toString();
        String className = FilenameUtils.getBaseName(classFile.toString());
        Process proc = new ProcessBuilder(javaBin, "-cp", programPath, className).start();
        Path testInput = Path.of(programPath, className + ".input");
        if (Files.exists(testInput)) {
            String input = FileUtils.readFileToString(testInput.toFile(), UTF_8);
            OutputStream stdin = proc.getOutputStream();
            IOUtils.write(input, stdin, UTF_8);
            stdin.flush();
        }
        assertEquals(0, proc.waitFor(), "Program finished with nonzero exit code");
        Path testOutput = Path.of(programPath, className + ".output");
        if (!Files.exists(testOutput))
            return;
        List<String> expectedOutput = FileUtils.readLines(testOutput.toFile(), UTF_8);
        List<String> actualOutput = IOUtils.readLines(proc.getInputStream(), UTF_8);
        assertThat(actualOutput)
            .withFailMessage("Program output " + actualOutput +
                             " when " + expectedOutput + " was expected")
            .usingElementComparator(outputComparator)
            .isEqualTo(expectedOutput);
    }

    private Stream<Arguments> provideCompiledPrograms() throws IOException {
        return Files.list(validPrograms)
            .filter(path -> path.toString().endsWith(".class"))
            .map(Arguments::of);
    }

    private final String javaBin = String.join(File.separator,
        System.getProperty("java.home"), "bin", "java"
    );

    Comparator<String> outputComparator = new Comparator<>() {
        // Makes a numerical comparison if expected output is numerical
        @Override
        public int compare(String actual, String expected) {
            Integer expectedInt = Ints.tryParse(expected);
            if (expectedInt != null) {
                int actualInt = Integer.parseInt(actual);
                return actualInt - expectedInt;
            }
            Double expectedDouble = Doubles.tryParse(expected);
            if (expectedDouble != null) {
                double actualDouble = Double.parseDouble(actual);
                return DoubleMath.fuzzyCompare(actualDouble, expectedDouble, 0.000001);
            }
            return actual.compareTo(expected);
        }
    };

}
