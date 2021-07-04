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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;

public class Compiler {

    private static int compile(String srcFile, String outputDir) {
        ParseTree tree;

        try {
            CharStream input = CharStreams.fromFileName(srcFile);
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
            System.err.println("Syntax error" + System.lineSeparator() + e.getMessage());
            return 1;
        } catch (SemanticException e) {
            System.err.println("Semantic error" + System.lineSeparator() + e.getMessage());
            return 1;
        } catch (IOException e) {
            System.err.println(srcFile + ": No such file");
            return 1;
        }
        try (PrintWriter pw = new PrintWriter("hejfil")) {
            pw.print("HFHJFH");
        } catch (FileNotFoundException e) {}

        // Input program is valid: Generate Jasm assembly
        String className = Files.getNameWithoutExtension(srcFile);
        String jasmFile = className + ".jasm";
        try (PrintWriter jasmWriter = new PrintWriter(jasmFile)) {
            String jasmText = tree.accept(new ProgramGenerator(className));
            jasmWriter.print(jasmText);
        } catch (FileNotFoundException e) {
            System.err.println("Could not create " + jasmFile);
            return 1;
        }

        // Assemble it
        try {
            return assemble(jasmFile, outputDir);
        } catch (IOException e) {
            e.printStackTrace();
            return 1;
        } catch (InterruptedException e) {
            e.printStackTrace();
            return 1;
        }

    }

    private static int assemble(String jasmFile, String outputDir) throws IOException,
                                                                          InterruptedException {
        String javaBin = System.getProperty("java.home") +
                         File.separator + "bin" +
                         File.separator + "java";
        String classpath = System.getProperty("java.class.path");
        String[] jasmCmd = new String[]{javaBin, "-cp", classpath,
                                       "org.openjdk.asmtools.Main",
                                       "jasm", "-d", outputDir, jasmFile};
        Process proc = Runtime.getRuntime().exec(jasmCmd);
        int exitCode = proc.waitFor();
        if (exitCode != 0) {
            System.err.println("Jasm assembly failed");
            return 1;
        }
        return 0;
    }

    // Error listener that stops the compiler at the first encountered lexical or parsing error
    static class BailingErrorListener extends BaseErrorListener {

        @Override
        public void syntaxError(Recognizer<?,?> recognizer,
                                Object offendingSymbol,
                                int line,
                                int charPositionInLine,
                                String msg,
                                RecognitionException e) {
            throw new ParseCancellationException("Line " + line + ":" +
                                                 charPositionInLine + ": " + msg);
        }

    }

    public static void main(String[] args) {
        Options options = new Options();
        options.addOption(Option.builder("o")
            .longOpt("output")
            .hasArg()
            .desc("output directory of .class file")
            .build());
        options.addOption(Option.builder("h")
            .longOpt("help")
            .desc("prints this message")
            .build());

        CommandLine cmd = null;
        try {
            cmd = new DefaultParser().parse(options, args);
        } catch (ParseException e) {
            System.err.println(e.getMessage());
            System.exit(1);
        }

        if (cmd.hasOption("h")) {
            StringBuilder usage = new StringBuilder();
            usage.append("java -jar NotC <options> <source file>");
            usage.append(System.lineSeparator());
            usage.append("where options include:");
            new HelpFormatter().printHelp(usage.toString(), options);
            return;
        }

        String outputDir;
        if (cmd.hasOption("o"))
            outputDir = cmd.getOptionValue("o");
        else
            outputDir = System.getProperty("user.dir");

        // After options, what should be left on the command line is the source file
        String[] remainingCmdLine = cmd.getArgs();
        if (remainingCmdLine.length < 1) {
            System.err.println("Missing source file argument");
            System.exit(1);
        }

        String srcFile = remainingCmdLine[0];

        int exitStatus = compile(srcFile, outputDir);
        System.exit(exitStatus);
    }

}
