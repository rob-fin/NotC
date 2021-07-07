package notc;

import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.assertj.core.api.Assertions.assertThat;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.filefilter.SuffixFileFilter;
import com.google.common.math.DoubleMath;
import com.google.common.primitives.Doubles;
import com.google.common.primitives.Ints;
import com.github.stefanbirkner.systemlambda.SystemLambda;

import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.OutputStream;
import java.io.BufferedWriter;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import static java.nio.charset.StandardCharsets.UTF_8;

// The compiler is run with a lot of test program source files: ones that should compile, ones that
// should be rejected by the parser, and ones that should be rejected during semantic analysis.
// The class expects that files in these test categories reside on the classpath in directories
// named "valid_programs", "syntax_errors", and "semantic_errors". Syntax errors rejected during
// semantic analysis and semantic errors rejected by the parser are considered failed tests.
// The successfully compiled programs are then executed and their outputs checked.
class ProgramsTest {

    private ClassLoader classLoader = getClass().getClassLoader();

    // Iterate over the source files in the test directories and run the compiler with them.
    // For each file run, capture the compiler's System.err output and check it
    // against the expected output for the category to which the file belongs.
    @Test
    @Order(1)
    void compilePrograms() throws Exception { // Exception from SystemLambda

        // Test category -> Assertion function over compiler error messages
        Map<String,Function<String,Boolean>> assertorByCategory = Map.of(
            "valid_programs",  sysErr -> sysErr.isEmpty(),
            "syntax_errors",   sysErr -> sysErr.startsWith("Syntax error"),
            "semantic_errors", sysErr -> sysErr.startsWith("Semantic error")
        );

        SuffixFileFilter notcFilter = new SuffixFileFilter("notc");

        for (String testCategory : assertorByCategory.keySet()) {
            String categoryPath = classLoader.getResource(testCategory).getFile();
            File testDir = new File(categoryPath);
            Function<String,Boolean> assertor = assertorByCategory.get(testCategory);

            for (File sourceFile : FileUtils.listFiles(testDir, notcFilter, null)) {
                String filePath = sourceFile.getAbsolutePath();
                // Instead of launching a new process (and JVM) for every file:
                // Call main method and prevent JVM from terminating.
                String sysErr = SystemLambda.tapSystemErr( () -> {
                    SystemLambda.catchSystemExit( () -> {
                        Main.main(new String[]{"-o", categoryPath, filePath});
                    });
                });
                assertTrue(assertor.apply(sysErr), sourceFile.getName() + " in " + testCategory + ":" +
                                                   System.lineSeparator() +
                                                   FileUtils.readFileToString(sourceFile, UTF_8) +
                                                   System.lineSeparator() +
                                                   "Compiler's System.err: " + sysErr);
            }
        }
    }

    // Run the programs that compiled
    @Test
    @Order(2)
    void runValidPrograms() throws IOException, InterruptedException {
        String javaBin = System.getProperty("java.home") +
                         File.separator + "bin" +
                         File.separator + "java";
        String validsPath = classLoader.getResource("valid_programs").getFile();
        File validsDir = new File(validsPath);
        SuffixFileFilter classFilter = new SuffixFileFilter("class");

        for (File classFile : FileUtils.listFiles(validsDir, classFilter, null)) {
            String className = FilenameUtils.getBaseName(classFile.getName());
            Process proc = new ProcessBuilder(javaBin, "-cp", validsPath, className).start();

            // Supply input to the program under test, if there is any
            File testInput = new File(validsPath + File.separator + className + ".input");
            if (testInput.exists()) {
                String input = FileUtils.readFileToString(testInput, UTF_8);
                OutputStream stdin = proc.getOutputStream();
                BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(stdin));
                writer.write(input);
                writer.flush();
            }

            int actualExit = proc.waitFor();
            assertEquals(actualExit, 0, "Program " + classFile.getName() +
                                        " finished with exit code " + actualExit);

            // If the program under test has some expected output, check it
            File testOutput = new File(validsPath + File.separator + className + ".output");
            if (!testOutput.exists())
                continue;
            List<String> actualOutput = IOUtils.readLines(proc.getInputStream(), UTF_8);
            List<String> expectedOutput = FileUtils.readLines(testOutput, UTF_8);
            assertThat(actualOutput)
                .withFailMessage(className + " wrote " + actualOutput +
                                 " when " + expectedOutput + " was expected")
                .usingElementComparator(outputComparator)
                .isEqualTo(expectedOutput);
        }
    }

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
