package notc.analysis;

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.LexerNoViableAltException;

// A lexer that actually reports lexical errors
public class BailingLexer extends NotCLexer {

    public BailingLexer(CharStream input) {
        super(input);
    }

    @Override
    public void recover(LexerNoViableAltException e) {
        throw e;
    }

}
