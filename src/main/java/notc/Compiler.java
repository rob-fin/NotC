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
        if (args.length != 1) {
            System.out.println("Usage: java -jar NotC.jar <source file>");
            return;
        }

        int exitStatus = compile(args[0]);
        System.exit(exitStatus);
    }

    private static int compile(String srcFile) {

        String outFile = stripExtension(srcFile) + ".class";

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

        return 0;
    }

    // Utility for removing extensions from file names
    private static String stripExtension(String srcFile) {
        int lastDot = srcFile.lastIndexOf(".");
        return lastDot < 0 ? srcFile : srcFile.substring(0, lastDot);
    }

}
