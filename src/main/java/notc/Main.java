package notc;

import notc.antlrgen.NotCLexer;
import notc.antlrgen.NotCParser;
import notc.semantics.ProgramChecker;
import notc.semantics.SemanticException;
import notc.codegen.ProgramGenerator;

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.Recognizer;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.misc.ParseCancellationException;
import org.antlr.v4.runtime.tree.ParseTree;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import com.google.common.io.Files;
import com.google.common.io.MoreFiles;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class Main {

    // Compiles a NotC program given by parameter srcFile to Jasmin assembly text,
    // defining a single class with a name given by parameter className
    private static String compile(Path srcFile, String className) {
        ParseTree tree = null;
        try {
            CharStream input = CharStreams.fromPath(srcFile);
            BailingErrorListener listener = new BailingErrorListener();

            // Lex
            NotCLexer lexer = new NotCLexer(input);
            lexer.removeErrorListeners();
            lexer.addErrorListener(listener);
            CommonTokenStream tokens = new CommonTokenStream(lexer);

            // Parse
            NotCParser parser = new NotCParser(tokens);
            parser.removeErrorListeners();
            parser.addErrorListener(listener);
            tree = parser.program();

            // Perform semantic checks
            tree.accept(new ProgramChecker());
        } catch (ParseCancellationException e) {
            error("Syntax error:", e.getMessage());
        } catch (SemanticException e) {
            error("Semantic error:", e.getMessage());
        } catch (IOException e) {
            error(srcFile + ": No such file");
        }
        // Input program is valid: Generate Jasmin assembly and return it
        return tree.accept(new ProgramGenerator(className));
    }

    // Write Jasmin assembly to a temporary file and assemble it
    private static void assemble(String jasminText, String outputDir) {
        try {
            File jasmFile = File.createTempFile("temp", "j");
            PrintWriter jasmWriter = new PrintWriter(jasmFile);
            jasmWriter.print(jasminText);
            jasmWriter.close();
            // ...in a new process so we can avoid System.exit() and read exit status
            String javaBin = System.getProperty("java.home")
                           + File.separator + "bin"
                           + File.separator + "java";
            String classpath = System.getProperty("java.class.path");
            List<String> jasmCmd = List.of(javaBin, "-cp", classpath,
                                           "jasmin.Main", "-d", outputDir,
                                           jasmFile.getAbsolutePath());
            Process proc = new ProcessBuilder(jasmCmd).inheritIO().start();
            if (proc.waitFor() != 0)
                error("Assembly failed:", jasminText);
        } catch (IOException e) {
            throw new RuntimeException("No intention to handle", e);
        } catch (InterruptedException e) {
            throw new RuntimeException("No intention to handle", e);
        }
    }
    // Prints an error message and exits
    private static void error(String... messages) {
        System.err.println(String.join(System.lineSeparator(), messages));
        System.exit(1);
    }

    // Entry point for the NotC compiler
    public static void main(String[] args) {
        // Set up command line options
        Options options = new Options();
        options.addOption(Option.builder("c")
            .longOpt("class")
            .hasArg()
            .argName("name")
            .desc("Name of generated class file. "
                + System.lineSeparator()
                + "Defaults to base name of source file.")
            .build());
        options.addOption(Option.builder("o")
            .longOpt("output")
            .hasArg()
            .argName("dir")
            .desc("Output directory of generated class file."
                + System.lineSeparator()
                + "Defaults to working directory of invoking process.")
            .build());
        options.addOption(Option.builder("h")
            .longOpt("help")
            .desc("Print this message and exit.")
            .build());

        // Parse command line
        CommandLine cmd = null;
        try {
            cmd = new DefaultParser().parse(options, args);
        } catch (ParseException e) {
            error(e.getMessage());
        }

        if (cmd.hasOption("h")) {
            StringBuilder usage = new StringBuilder();
            usage.append("java -jar NotC <options> <source file>");
            usage.append(System.lineSeparator());
            usage.append("where options include:");
            new HelpFormatter().printHelp(usage.toString(), options);
            System.exit(0);
        }

        // After any options, what should be left on the command line is the source file argument
        String[] remainingCmdLine = cmd.getArgs();
        if (remainingCmdLine.length < 1)
            error("Missing source file argument");
        Path srcFile = Paths.get(remainingCmdLine[0]);

        String className = cmd.getOptionValue("c", MoreFiles.getNameWithoutExtension(srcFile));
        if (!className.matches("[a-zA-Z_]+[a-zA-Z0-9_]*")) {
            error(className + ": Illegal class name",
                  "May contain letters, digits, and underscores, but not start with a digit");
        }

        String outputDir = cmd.getOptionValue("o", System.getProperty("user.dir"));
        String jasminText = compile(srcFile, className);
        assemble(jasminText, outputDir);
        System.exit(0);
    }

    // Error listener that stops the compiler at the first encountered lexical or parsing error
    private static class BailingErrorListener extends BaseErrorListener {

        @Override
        public void syntaxError(Recognizer<?,?> recognizer,
                                Object offendingSymbol,
                                int line,
                                int charPositionInLine,
                                String msg,
                                RecognitionException e) {
            throw new ParseCancellationException("Line " + line + ":"
                                               + charPositionInLine + ": " + msg);
        }

    }

}
