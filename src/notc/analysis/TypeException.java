package notc.analysis;

import org.antlr.v4.runtime.Token;

/* Exceptions thrown when type checking.
 * Contains one constructor for when the encountered type error
 * does not pertain to any particular input token, and one for when it does. */
public class TypeException extends RuntimeException {

    TypeException(String msg) {
        super(msg);
    }

    TypeException(Token tok, String msg) {
        this("Line " + tok.getLine() + " near \"" +
             tok.getText() + "\": " + msg);
    }

}
