package notc.analysis;

import org.antlr.v4.runtime.Token;

/* Exception thrown to report errors encountered in semantic analysis.
 * This includes type errors and references to undeclared identifiers.
 * Contains one constructor for when the encountered error does not
 * pertain to any particular input token, and one for when it does. */
public class SemanticException extends RuntimeException {

    SemanticException(String msg) {
        super(msg);
    }

    SemanticException(Token tok, String msg) {
        this("Line " + tok.getLine() + " near \"" +
             tok.getText() + "\": " + msg);
    }

}
