package notc;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.AfterAll;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.SuffixFileFilter;
import com.github.stefanbirkner.systemlambda.SystemLambda;

import java.io.File;
import java.util.Map;

/* The compiler is run with a lot of test program source files, both ones that
 * should compile and ones that should be rejected at different compilation stages.
 * For each file run, the exit code returned from the compiler is checked against
 * the one expected for the file.
 * Instead of launching a new process for every file, the main method of the
 * compiler is called.
 * TODO once code generator is in place:
 * Also check if produced output matches expected output. */
class ProgramsTest {
    private static int nRun = 0;
    private static int nPassed = 0;

    // Test program directories are on the classpathh
    ClassLoader classLoader = getClass().getClassLoader();

    // Test category -> Expected exit code
    private Map<String,Integer> exitByCategory = Map.of(
        "good_programs",   0,
        "syntax_errors",   1,
        "semantic_errors", 2
    );

    // Actual exit code -> Test failure message
    private Map<Integer,String> msgByExit = Map.of(
        0, "Not caught",
        1, "Rejected when parsing",
        2, "Rejected during semantic analysis"
    );

    /* Iterate over each .notc file in the test directories,
     * run the compiler with it, and check exit status. */
    @Test
    void compilePrograms() throws Exception {
        SuffixFileFilter notcFilter = new SuffixFileFilter("notc");

        for (Map.Entry<String,Integer> entry : exitByCategory.entrySet()) {

            String testCategory = entry.getKey();
            int expectedExit = entry.getValue();

            String categoryPath = classLoader.getResource(testCategory).getFile();
            File testDir = new File(categoryPath);

            for (File sourceFile : FileUtils.listFiles(testDir, notcFilter, null)) {
                String filePath = sourceFile.getAbsolutePath();
                // Read the exit code and prevent the JVM from terminating
                int actualExit = SystemLambda.catchSystemExit( () ->
                    Compiler.main(new String[]{filePath})
                );
                if (actualExit == expectedExit) {
                    nPassed += 1;
                } else {
                    // System.err is suppressed to avoid compiler output, so use System.out
                    System.out.println(sourceFile.getName() + " in " + testCategory + ":");
                    System.out.println(FileUtils.readFileToString(sourceFile, "UTF-8"));
                    System.out.println(msgByExit.getOrDefault(actualExit,
                                                              "Unexpected system error"));
                }
                nRun += 1;
            }
        }
    }

    @AfterAll
    static void checkResults() {
        Assertions.assertEquals(nRun, nPassed);
    }

}
