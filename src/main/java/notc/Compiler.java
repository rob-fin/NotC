package notc;

import notc.analysis.BailingErrorListener;
import notc.analysis.NotCLexer;
import notc.analysis.NotCParser;
import notc.analysis.ProgramChecker;
import notc.analysis.SemanticException;

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.misc.ParseCancellationException;

import java.io.PrintWriter;
import java.io.IOException;

public class Compiler {

    public static void main(String[] args) {
        if (args.length != 1)
            return;

        String srcFile = args[0];
        String outFile = stripExtension(srcFile) + ".output";

        // 0: ok, 1: syntax error, 2: semantic error, 3: source file not found
        int exitCode = 0;

        try (PrintWriter out = new PrintWriter(outFile)) {
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
            ParseTree tree = parser.program();

            // Type check
            tree.accept(new ProgramChecker());

            // TODO: Generate Java bytecode
            // Output abstract syntax tree for now...
            out.print(tree.toStringTree(parser));

        } catch (ParseCancellationException e) {
            System.err.println("Syntax error\n" + e.getMessage());
            exitCode = 1;
        } catch (SemanticException e) {
            System.err.println("Semantic error\n" + e.getMessage());
            exitCode = 2;
        } catch (IOException e) {
            System.err.println(srcFile + ": No such file");
            e.printStackTrace();
            exitCode = 3;
        }

        System.exit(exitCode);
    }

    // Utility for removing extensions from file names
    private static String stripExtension(String srcFile) {
        int lastDot = srcFile.lastIndexOf(".");
        return lastDot < 0 ? srcFile : srcFile.substring(0, lastDot);
    }

}
