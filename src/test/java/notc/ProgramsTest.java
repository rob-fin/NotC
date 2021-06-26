package notc;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.AfterAll;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.SuffixFileFilter;

import java.io.File;
import java.util.Map;

/* The compiler is run with a lot of test program source files, both ones that
 * should compile and ones that should be rejected at different compilation stages.
 * For each file run, the exit code returned from the compiler is checked against
 * the one expected for the file.
 * TODO once code generator is in place:
 * Also check if produced output matches expected output. */
class ProgramsTest {
    private static int nRun = 0;
    private static int nPassed = 0;

    // Test program directories are on the classpathh
    private ClassLoader classLoader = getClass().getClassLoader();
    // The launched processes should have same classpath and run in new JVMs
    private String javaBin = System.getProperty("java.home") +
            File.separator + "bin" +
            File.separator + "java";
    private String classpath = System.getProperty("java.class.path");

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
                ProcessBuilder pb = new ProcessBuilder(javaBin, "-cp", classpath,
                                                       "notc.Compiler", filePath);
                Process p = pb.start();
                p.waitFor();
                int actualExit = p.exitValue();
                if (actualExit == expectedExit) {
                    nPassed += 1;
                } else {
                    // stderr is suppressed to avoid compiler output, so use stdout
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
