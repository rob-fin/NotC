package notc.semantics;

import org.antlr.v4.runtime.Token;

// Exception thrown to report errors encountered during semantic analysis.
// This includes type errors and references to undeclared identifiers.
public class SemanticException extends RuntimeException {

    SemanticException(String msg) {
        super(msg);
    }

    SemanticException(Token tok, String msg) {
        this("Line " + tok.getLine() + " near \"" +
             tok.getText() + "\": " + msg);
    }

}