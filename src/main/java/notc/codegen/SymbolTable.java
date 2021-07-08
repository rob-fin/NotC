package notc.codegen;

import notc.antlrgen.NotCParser.SrcType;

import org.antlr.v4.runtime.Token;

import java.util.HashMap;
import java.util.LinkedList;

// Class used to resolve types in the code generation environment:
// Function ids to JVM type signatures and variables to their allocated addresses.
class SymbolTable {
    HashMap<String,String> signatures;
    public LinkedList<HashMap<String,Integer>> vars;

    SymbolTable() {
        signatures = new HashMap<String,String>();
        vars = new LinkedList<HashMap<String,Integer>>();
    }

    public void addFun(Token idTok, String JvmType) {
        String funId = idTok.getText();
        signatures.put(funId, JvmType);
    }

}
