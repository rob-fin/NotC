package NotC;

import java.io.*;
import NotC.Absyn.*;
import NotC.TypeChecker.*;

public class Compiler {
    
    public static void main(String[] args) {
        
        // 0: ok, 1: lexing error, 2: parsing error, 3: type error, -1: unexpected system error
        int exitCode = 0;
        
        Yylex  lex = null;
        parser par = null;
        
        try {
            String srcFile = args[0];
            String outFile = stripExtension(srcFile) + ".output";
            FileReader in = new FileReader(srcFile);
            
            // Lex and parse
            lex = new Yylex(in);
            par = new parser(lex);
            NotC.Absyn.Program ast = par.pProgram();
            in.close();
            
            // Type check
            ast.accept(new NotC.TypeChecker.CheckProgram(), null);
            
            // Output the abstract syntax tree for now
            PrintWriter out = new PrintWriter(outFile);
            out.print(PrettyPrinter.show(ast));
            out.close();
        } catch (TypeException e) {
            System.err.println(e.getMessage());
            exitCode = 3;
        } catch (RuntimeException e) {
            e.printStackTrace();
            exitCode = -1;
        } catch (IOException e) {
            e.printStackTrace();
            exitCode = -1;
        } catch (Throwable e) {
            System.err.println("Line " + lex.line_num() + " near \"" +
                               lex.buff() + "\": " + e.getMessage());
            // BNFC-generated lexers throw Errors, parsers Exceptions
            exitCode = e instanceof Error ? 1 : 2;
        }
            
        System.exit(exitCode);
        
    }
    
    // Utility method for removing extensions from file names
    private static String stripExtension(String srcFile) {
        int lastDot = srcFile.lastIndexOf(".");
        return lastDot < 0 ? srcFile : srcFile.substring(0, lastDot);
    }
    
}
