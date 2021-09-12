package notc;

import notc.antlrgen.NotCLexer;
import notc.antlrgen.NotCParser;
import notc.semantics.ProgramChecker;
import notc.semantics.SemanticException;
import notc.semantics.SymbolTable;
import notc.codegen.ProgramGenerator;

import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.misc.ParseCancellationException;
import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.Recognizer;
import org.antlr.v4.runtime.RecognitionException;
import com.github.stefanbirkner.systemlambda.SystemLambda;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class Compiler {

    // Attempts to compile a NotC program given by srcFile into a class named className
    // and place it in destDir. Returns true on success, false otherwise.
    boolean compile(Path srcFile, String className, Path destDir) {
        ParseTree tree;
        SymbolTable symTab;

        try {
            CharStream input = CharStreams.fromPath(srcFile);
            BailingErrorListener listener = new BailingErrorListener();

            NotCLexer lexer = new NotCLexer(input);
            lexer.removeErrorListeners();
            lexer.addErrorListener(listener);
            CommonTokenStream tokens = new CommonTokenStream(lexer);

            NotCParser parser = new NotCParser(tokens);
            parser.removeErrorListeners();
            parser.addErrorListener(listener);
            tree = parser.program();

            symTab = tree.accept(new ProgramChecker());
        } catch (IOException e) {
            System.err.println(srcFile + ": No such file");
            return false;
        } catch (ParseCancellationException e) {
            System.err.println("Syntax error: " + e.getMessage());
            return false;
        } catch (SemanticException e) {
            System.err.println("Semantic error: " + e.getMessage());
            return false;
        }
        // Program is valid: Generate Jasm representation and assemble it
        String jasmText = tree.accept(new ProgramGenerator(symTab, className));
        return assembleClass(jasmText, destDir);
    }

    private boolean assembleClass(String jasmText, Path destDir) {
        int exitCode;

        try {
            Path jasmFile = Files.createTempFile("tmp", "jasm");
            Files.writeString(jasmFile, jasmText);

            // Maybe rework this...
            exitCode = SystemLambda.catchSystemExit( () ->
                org.openjdk.asmtools.Main.jasm(
                    new String[]{"-d", destDir.toString(), jasmFile.toString()}
                )
            );

        } catch (Exception e) { // SystemLambda
            throw new RuntimeException("No means to handle", e);
        }

        if (exitCode != 0) {
            System.err.println("Assembly failed:");
            System.err.println(jasmText);
            return false;
        }
        return true;
    }

    // Stops the compiler at the first encountered lexical or parsing error
    private static class BailingErrorListener extends BaseErrorListener {

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

}