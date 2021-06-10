package notc;

import notc.analysis.BailingLexer;
import notc.analysis.NotCParser;
import notc.analysis.ProgramChecker;
import notc.analysis.TypeException;

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.LexerNoViableAltException;
import org.antlr.v4.runtime.BailErrorStrategy;
import org.antlr.v4.runtime.BailErrorStrategy;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.misc.ParseCancellationException;

import java.io.IOException;

public class Compiler {

    public static void main(String[] args) {

        if (args.length != 1)
            return;

        String srcFile = args[0];
        int exitCode = 0;

        try {
            CharStream input = CharStreams.fromFileName(srcFile);

            // Lex
            BailingLexer lexer = new BailingLexer(input);
            CommonTokenStream tokens = new CommonTokenStream(lexer);

            // Parse
            NotCParser parser = new NotCParser(tokens);
            parser.setErrorHandler(new BailErrorStrategy());
            ParseTree tree = parser.program(); ;

            // Type check
            tree.accept(new ProgramChecker());

            // Generate code...

        } catch (LexerNoViableAltException e) {
            System.out.print(e.getMessage());
            exitCode = 1;
        } catch (ParseCancellationException e) {
            System.err.println(e.getMessage());
            exitCode = 1;
        } catch (TypeException e) {
            System.err.println(e.getMessage());
            exitCode = 2;
        } catch (IOException e) {
            System.err.println(srcFile + ": No such file");
            e.printStackTrace();
            exitCode = 3;
        }
        System.exit(exitCode);
    }

}
