package notc;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.io.FilenameUtils;

import java.nio.file.Path;

// Validates command line input before calling the compiler
public class Main {
    private static final String LINE_SEP = System.lineSeparator();

    public static void main(String[] args) {
        // Parses flags and their arguments
        Options options = setUpOptions();
        CommandLine cmd = null;
        try {
            cmd = new DefaultParser().parse(options, args);
        } catch (ParseException e) {
            error(e.getMessage());
        }

        if (cmd.hasOption("help")) {
            printUsage(options);
            return;
        }

        // What should be left is the source file
        String[] remainingArgs = cmd.getArgs();
        if (remainingArgs.length < 1) {
            printUsage(options);
            error("Missing source file argument");
        }
        Path srcFile = Path.of(remainingArgs[0]);

        String className = cmd.getOptionValue("class",
            FilenameUtils.getBaseName(srcFile.toString())
        );
        if (!isLegalClassName(className))
            error("Illegal class name");

        String dirArg = cmd.getOptionValue("directory",
            System.getProperty("user.dir")
        );
        Path destDir = Path.of(dirArg);

        boolean result = new Compiler().compile(srcFile, className, destDir);
        System.exit(result ? 0 : 1);
    }

    private static void printUsage(Options options) {
        String usage = String.join(LINE_SEP,
            "java -jar notcc.jar <options> <source file>",
            "where options include:"
        );
        new HelpFormatter().printHelp(usage, options);
    }

    private static boolean isLegalClassName(String s) {
        if (!Character.isJavaIdentifierStart(s.charAt(0)))
            return false;
        int len = s.length();
        for (int i = 1; i < len; ++i) {
            if (!Character.isJavaIdentifierPart(s.charAt(i)))
                return false;
        }
        return true;
    }

    private static void error(String... messages) {
        System.err.println(String.join(LINE_SEP, messages));
        System.exit(1);
    }

    private static Options setUpOptions() {
        Options options = new Options();
        options.addOption(Option.builder("c")
            .longOpt("class")
            .hasArg()
            .argName("name")
            .desc("Name of generated class." +
                  LINE_SEP +
                  "Defaults to base name of source file.")
            .build());
        options.addOption(Option.builder("d")
            .longOpt("directory")
            .hasArg()
            .argName("path")
            .desc("Destination directory of generated class file." +
                  LINE_SEP +
                  "Defaults to working directory of invoking process.")
            .build());
        options.addOption(Option.builder("h")
            .longOpt("help")
            .desc("Print this message and exit.")
            .build());

        return options;
    }

}