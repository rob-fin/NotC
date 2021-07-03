package notc;

import notc.antlrgen.NotCLexer;
import notc.antlrgen.NotCParser;
import notc.semantics.ProgramChecker;
import notc.semantics.SemanticException;

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.misc.ParseCancellationException;
import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.Recognizer;
import org.antlr.v4.runtime.RecognitionException;
import com.google.common.io.Files;

import java.io.PrintWriter;
import java.io.IOException;
import java.io.FileNotFoundException;

public class Compiler {

    public static void main(String[] args) {
        if (args.length != 1) {
            System.out.println("Usage: java -jar NotC.jar <source file>");
            return;
        }

        int exitStatus = compile(args[0]);
        System.exit(exitStatus);
    }

    private static int compile(String srcFile) {
        ParseTree tree;
        NotCParser parser;

        try {
            CharStream input = CharStreams.fromFileName(srcFile);
            BailingErrorListener listener = new BailingErrorListener();

            // Lex
            NotCLexer lexer = new NotCLexer(input);
            lexer.removeErrorListeners();
            lexer.addErrorListener(listener);
            CommonTokenStream tokens = new CommonTokenStream(lexer);

            // Parse
            parser = new NotCParser(tokens);
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
            e.printStackTrace();
            return 1;
        }

        // Program is valid: Generate Java assembly
        String jasmText;
        String jasmFile = Files.getNameWithoutExtension(srcFile) + ".jasm";
        try (PrintWriter jasmWriter = new PrintWriter(jasmFile)) {
            // jasmText = tree.accept(new ProgramGenerator());
            // jasmWriter.print(jasmText);
            // Output abstract syntax tree for now...
            jasmWriter.print(tree.toStringTree(parser));
        } catch (FileNotFoundException e) {
            System.err.println("Could not create " + jasmFile);
            e.printStackTrace();
            return 1;
        }

        // TODO: Assemble .class file
        return 0;
    }

    // Error listener that stops the compiler at the first encountered lexical or parsing error
    static class BailingErrorListener extends BaseErrorListener {

        @Override
        public void syntaxError(Recognizer<?, ?> recognizer,
                                Object offendingSymbol,
                                int line,
                                int charPositionInLine,
                                String msg,
                                RecognitionException e) {
            throw new ParseCancellationException("Line " + line + ":" +
                                                 charPositionInLine + ": " + msg);
        }

    }

}
