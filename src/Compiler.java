package NotC;

import java.io.*;
import NotC.Absyn.*;

public class Compiler {
    
    public static void main(String[] args) {
        
        // 0: ok, 1: compile/usage error, -1: unexpected system error
        int exitCode = 0;
        
        Reader src = null;
        Yylex  lex = null;
        parser par = null;
        
        try {
            src = new InputStreamReader(System.in);
            if (!src.ready()) {
                throw new IllegalStateException();
            }
            
            // Parse
            lex = new Yylex(src);
            par = new parser(lex);
            NotC.Absyn.Program ast = par.pProgram();
            
            // TODO: typecheck, generate code
            // Print the abstract syntax tree for now
            System.out.println(PrettyPrinter.show(ast));
        } catch (IOException e) {
            e.printStackTrace();
            exitCode = -1;
        } catch (IllegalStateException e) {
            System.err.println("Usage: docker run -i --rm <image> < <sourcefile>");
            exitCode = 1;
        } catch (Throwable e) {
            System.err.println("Syntax Error on line " + lex.line_num() +
                               " near \"" + lex.buff() + "\": " + e.getMessage());
            exitCode = 1;
        } finally {
            try {
                src.close();
            }
            catch (IOException e) {
                e.printStackTrace();
                exitCode = -1;
            }
            System.exit(exitCode);
        }
        
    }
    
}
