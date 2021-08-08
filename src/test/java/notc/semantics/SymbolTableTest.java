package notc.semantics;

import notc.antlrgen.NotCParser;
import notc.antlrgen.NotCParser.Type;
import notc.antlrgen.NotCParser.Signature;

import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.CommonToken;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.ArrayList;

class SymbolTableTest {

    private SymbolTable symTab;

    @BeforeEach
    void init() {
        symTab = new SymbolTable();
        symTab.pushScope();
    }

    @Test
    void AddFun_LookupSucceeds() {
        Token funIdTok = new CommonToken(NotCParser.ID, "fun");
        Type returnType = Type.DOUBLE;
        List<Type> paramTypes = List.of(Type.INT,
                                        Type.BOOL,
                                        Type.STRING);
        symTab.addFun(funIdTok, new Signature(returnType, paramTypes));
        Signature lookedUpFun = symTab.lookupFun(funIdTok);
        assertTrue(lookedUpFun.returnType() == returnType);
        assertTrue(paramTypes.equals(lookedUpFun.paramTypes()));
    }

    @Test
    void RedefineFun_SemanticExceptionThrown() {
        Token funIdTok1 = new CommonToken(NotCParser.ID, "funFun");
        Token funIdTok2 = new CommonToken(NotCParser.ID, "funFun");
        symTab.addFun(funIdTok1, null);
        Exception thrownException = assertThrows(SemanticException.class, () -> {
            symTab.addFun(funIdTok2, null);
        });
        String actualMessage = thrownException.getMessage();
        assertTrue(actualMessage.contains("Redefinition of function"));
    }

    @Test
    void LookupUndefinedFun_SemanticExceptionThrown() {
        Exception thrownException = assertThrows(SemanticException.class, () -> {
            symTab.lookupFun(new CommonToken(NotCParser.ID, "undefinedFun"));
        });
        String actualMessage = thrownException.getMessage();
        assertTrue(actualMessage.contains("Undefined function"));
    }

    @Test
    void SetContextWithParams_ParamsGetDefinedAsVars() {
        int nParams = 10;
        List<Type> paramTypes = new ArrayList<>(nParams);
        List<Token> paramIdToks = new ArrayList<>(nParams);
        for (int i = 0; i < nParams; i++) {
            paramTypes.add(Type.INT);
            paramIdToks.add(new CommonToken(NotCParser.ID, "someVar" + i));
        }
        symTab.setContext(paramTypes, paramIdToks);
        for (int i = 0; i < nParams; i++) {
            Token idTok = paramIdToks.get(i);
            Type lookedUpType = symTab.lookupVar(idTok);
            assertEquals(paramTypes.get(i), lookedUpType);
        }
    }

    @Test
    void AddVar_LookupSucceeds() {
        Type declaredType = Type.STRING;
        Token varIdTok = new CommonToken(NotCParser.ID, "var");
        symTab.addVar(declaredType, varIdTok);
        Type lookedUpType = symTab.lookupVar(varIdTok);
        assertEquals(declaredType, lookedUpType);
    }

    @Test
    void AddIdenticalVarsInDifferentScopes_BothSuccessfullyAdded() {
        Type varType = Type.BOOL;
        String varId = "varWithThisName";
        Token varIdTok1 = new CommonToken(NotCParser.ID, varId);
        Token varIdTok2 = new CommonToken(NotCParser.ID, varId);
        symTab.addVar(varType, varIdTok1);
        symTab.pushScope();
        symTab.addVar(varType, varIdTok2);
    }

    @Test
    void LookupVarInDeepScope_LookupSucceeds() {
        Type varType = Type.DOUBLE;
        String varId = "iAmDeep";
        Token varIdTok = new CommonToken(NotCParser.ID, varId);
        symTab.addVar(varType, varIdTok);
        int nScopes = 20;
        for (int i = 0; i < nScopes; i++)
            symTab.pushScope();
        Type lookedUpType = symTab.lookupVar(varIdTok);
        assertEquals(varType, lookedUpType);
    }

    @Test
    void LookupUndefinedVar_SemanticExceptionThrown() {
        Exception thrownException = assertThrows(SemanticException.class, () -> {
            symTab.lookupVar(new CommonToken(NotCParser.ID, "undefVar"));
        });
        String actualMessage = thrownException.getMessage();
        assertTrue(actualMessage.contains("Undefined variable"));
    }

    @Test
    void OutOfScopeVarIsLookedUp_SemanticExceptionThrown() {
        symTab.pushScope();
        Token varIdTok = new CommonToken(NotCParser.ID, "var");
        symTab.addVar(Type.STRING, varIdTok);
        symTab.popScope();
        Exception thrownException = assertThrows(SemanticException.class, () -> {
            symTab.lookupVar(varIdTok);
        });
        String actualMessage = thrownException.getMessage();
        assertTrue(actualMessage.contains("Undefined variable"));
    }

    @Test
    void AddIdenticalVarsInSameScope_SemanticExceptionThrown() {
        Type varType = Type.INT;
        String varId = "varrr";
        Token varIdTok1 = new CommonToken(NotCParser.ID, varId);
        Token varIdTok2 = new CommonToken(NotCParser.ID, varId);
        symTab.addVar(varType, varIdTok1);
        Exception thrownException = assertThrows(SemanticException.class, () -> {
            symTab.addVar(varType, varIdTok2);
        });
        String actualMessage = thrownException.getMessage();
        assertTrue(actualMessage.contains("Redefinition of variable"));

    }

    @Test
    void AddVoidVar_SemanticExceptionThrown() {
        Exception thrownException = assertThrows(SemanticException.class, () -> {
            symTab.addVar(Type.VOID, new CommonToken(NotCParser.ID, "var"));
        });
        String actualMessage = thrownException.getMessage();
        assertTrue(actualMessage.contains("Variables cannot have type void"));
    }

    @Test
    void AddVarTwice_SemanticExceptionThrown() {
        Type declaredType = Type.BOOL;
        Token varIdTok = new CommonToken(NotCParser.ID, "var");
        symTab.addVar(declaredType, varIdTok);
        Exception thrownException = assertThrows(SemanticException.class, () -> {
            symTab.addVar(declaredType, varIdTok);
        });
        String actualMessage = thrownException.getMessage();
        assertTrue(actualMessage.contains("Redefinition of variable"));
    }

}
