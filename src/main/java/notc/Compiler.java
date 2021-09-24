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
import org.antlr.v4.runtime.ANTLRErrorListener;
import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.Recognizer;
import org.antlr.v4.runtime.RecognitionException;
import jasmin.ClassFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.io.OutputStream;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;

class Compiler {

    // Attempts to compile a NotC program given by srcFile into a class named className
    // and place it in destDir. Returns true on success, false otherwise.
    boolean compile(Path srcFile, String className, Path destDir) {
        ParseTree tree;
        SymbolTable symTab;

        // Analyzes program
        try {
            CharStream input = CharStreams.fromPath(srcFile);
            ANTLRErrorListener listener = new BailingErrorListener();

            NotCLexer lexer = new NotCLexer(input);
            lexer.removeErrorListeners();
            lexer.addErrorListener(listener);
            CommonTokenStream tokens = new CommonTokenStream(lexer);

            NotCParser parser = NotCParser.from(tokens, className);
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

        // Generates Jasmin representation and assembles it
        String jasmText = tree.accept(new ProgramGenerator(symTab, className));
        ClassFile classFile = new ClassFile();
        Path outFile = destDir.resolve(Path.of(className + ".class"));
        try (StringReader sr = new StringReader(jasmText);
            OutputStream os = Files.newOutputStream(outFile)) {
            classFile.readJasmin(sr, className, /* numberLines = */ true);
            classFile.write(os);
        } catch (IOException e) {
            throw new UncheckedIOException("No means to handle", e);
        } catch (Exception e) {
            throw new AssemblyException(jasmText, e);
        }

        return true;
    }

    // Wraps general Exceptions from the Jasmin API
    static class AssemblyException extends RuntimeException {
        AssemblyException(String jasmText, Exception e) {
            super("Assembly failed:" + System.lineSeparator() + jasmText, e);
        }
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