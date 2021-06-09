package notc;

import notc.analysis.*;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.misc.ParseCancellationException;

import java.io.*;



public class Compiler {
    
    public static void main(String[] args) {
        
        if (args.length != 1)
            return;
        
        String srcFile = args[0];
        
        int exitCode = 0;
        try {
            CharStream input = CharStreams.fromFileName(srcFile);
            NotCLexer lexer = new NotCLexer(input);
            CommonTokenStream tokens = new CommonTokenStream(lexer);
            NotCParser parser = new NotCParser(tokens);
            parser.setErrorHandler(new BailErrorStrategy());
            ParseTree ast = parser.program(); // parse
        
            //new CheckProgram().visit(ast);
            ast.accept(new ProgramChecker());

        } catch (LexerNoViableAltException e) {
            System.out.print(e.getMessage());
            exitCode = 1;
        } catch (ParseCancellationException e) {
            System.out.print(e.getMessage());
            exitCode = 1;
        } catch (TypeException e) {
            System.out.print(e.getMessage());
            exitCode = 2;
        } catch (IOException e) {
            System.err.print(srcFile + ": No such file");
            e.printStackTrace();
            exitCode = 3;
        }
        System.exit(exitCode);
    }
    
}
