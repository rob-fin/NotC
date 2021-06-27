package notc;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.SuffixFileFilter;
import com.github.stefanbirkner.systemlambda.SystemLambda;

import java.io.File;
import java.util.function.Function;
import java.util.Map;

/* The compiler is run with a lot of test program source files: ones that should compile, ones that
 * should be rejected by the parser, and ones that should be rejected during semantic analysis.
 * The class expects that files in these test categories reside on the classpath
 * in directories named "good_programs", "syntax_errors", and "semantic_errors", */
class ProgramsTest {

    private static int nRun = 0;
    private static int nPassed = 0;

    private ClassLoader classLoader = getClass().getClassLoader();

    /* Iterate over the source files in the test directories and run the compiler with them.
     * For each file run, capture the compiler's System.err output
     * and check it against the expectation for the category to which the file belongs. */
    @Test
    @Order(1)
    void compilePrograms() throws Exception {

        // Test category -> Assertion function over compiler error messages
        Map<String,Function<String,Boolean>> assertorByCategory = Map.of(
            "good_programs",   sysErr -> sysErr.isEmpty(),
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
                /* Instead of launching a new process (and JVM) for every file:
                 * Call main method and prevent JVM from terminating. */
                String sysErr = SystemLambda.tapSystemErr( () -> {
                    SystemLambda.catchSystemExit( () ->
                        Compiler.main(new String[]{filePath})
                    );
                });

                nRun += 1;
                if (assertor.apply(sysErr)) {
                    nPassed += 1;
                } else {
                    System.err.println(sourceFile.getName() + " in " + testCategory + ":");
                    System.err.println(FileUtils.readFileToString(sourceFile, "UTF-8"));
                    System.err.println("Compiler's System.err: " + sysErr);
                }
            }
        }
    }

    /* TODO once code generator is in place: Run the programs that compiled
     * and check if their produced output matches their expected output. */
    @Test
    @Order(2)
    void checkOutput() {
    }

    @AfterAll
    static void checkResults() {
        Assertions.assertEquals(nRun, nPassed);
    }

}
