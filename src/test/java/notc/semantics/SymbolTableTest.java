package notc.semantics;

import notc.antlrgen.NotCParser;
import notc.antlrgen.NotCParser.Type;
import notc.antlrgen.NotCParser.Signature;
import notc.antlrgen.NotCParser.VariableDeclarationContext;

import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.CommonToken;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertSame;
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
        Token funIdTok = new CommonToken(NotCParser.ID, "fffun");
        Type returnType = Type.DOUBLE;
        List<Type> paramTypes = List.of(Type.INT,
                                        Type.BOOL,
                                        Type.STRING);
        symTab.addFun(funIdTok, new Signature(returnType, paramTypes));
        Signature lookedUpFun = symTab.lookupFun(funIdTok);
        assertEquals(lookedUpFun.returnType(), returnType);
        assertEquals(paramTypes, lookedUpFun.paramTypes());
    }

    @Test
    void RedefineFun_SemanticExceptionThrown() {
        String funName = "funFun";
        Token funIdTok1 = new CommonToken(NotCParser.ID, funName);
        Token funIdTok2 = new CommonToken(NotCParser.ID, funName);
        symTab.addFun(funIdTok1, null);
        SemanticException thrown = assertThrows(SemanticException.class, () -> {
            symTab.addFun(funIdTok2, null);
        });
        String actualMessage = thrown.getMessage();
        assertTrue(actualMessage.contains("Redefinition of function"));
    }

    @Test
    void AddVar_LookupSucceeds() {
        String varName = "var";
        VariableDeclarationContext varDecl = new VariableDeclarationContext(null, 0);
        varDecl.type = Type.STRING;
        varDecl.id = new CommonToken(NotCParser.ID, varName);
        symTab.addVar(varDecl);
        Token varRef = new CommonToken(NotCParser.ID, varName);
        symTab.resolveVarReference(varRef);
        VariableDeclarationContext lookedUpVar = symTab.lookupVar(varRef);
        assertSame(varDecl, lookedUpVar);
    }

    @Test
    void AddIdenticalVarsInDifferentScopes_BothSuccessfullyAdded() {
        Type varType = Type.BOOL;
        String varName = "varWithThisName";
        Token varIdTok = new CommonToken(NotCParser.ID, varName);
        VariableDeclarationContext varDecl1 = new VariableDeclarationContext(null, 0);
        VariableDeclarationContext varDecl2 = new VariableDeclarationContext(null, 0);
        varDecl1.type = varType;
        varDecl2.type = varType;
        varDecl1.id = varIdTok;
        varDecl2.id = varIdTok;

        symTab.addVar(varDecl1);
        symTab.pushScope();
        symTab.addVar(varDecl2);
    }

    @Test
    void LookupVarInDeepScope_LookupSucceeds() {
        VariableDeclarationContext varDecl = new VariableDeclarationContext(null, 0);
        String varName = "iAmDeep";
        varDecl.type = Type.DOUBLE;
        varDecl.id = new CommonToken(NotCParser.ID, varName);
        symTab.addVar(varDecl);
        int nScopes = 20;
        for (int i = 0; i < nScopes; i++)
            symTab.pushScope();
        Token varRef = new CommonToken(NotCParser.ID, varName);
        symTab.resolveVarReference(varRef);
        assertSame(varDecl, symTab.lookupVar(varRef));
    }

    @Test
    void ResolveUndefinedVar_SemanticExceptionThrown() {
        SemanticException thrown = assertThrows(SemanticException.class, () -> {
            symTab.resolveVarReference(new CommonToken(NotCParser.ID, "undefVar"));
        });
        String actualMessage = thrown.getMessage();
        assertTrue(actualMessage.contains("Undefined variable"));
    }

    @Test
    void ResolveOutOufScopeVar_SemanticExceptionThrown() {
        VariableDeclarationContext varDecl = new VariableDeclarationContext(null, 0);
        String varName = "var";
        varDecl.type = Type.INT;
        varDecl.id = new CommonToken(NotCParser.ID, varName);
        symTab.pushScope();
        symTab.addVar(varDecl);
        symTab.popScope();
        Token varRef = new CommonToken(NotCParser.ID, varName);
        SemanticException thrown = assertThrows(SemanticException.class, () -> {
            symTab.resolveVarReference(varRef);
        });
        String actualMessage = thrown.getMessage();
        assertTrue(actualMessage.contains("Undefined variable"));
    }

    @Test
    void AddIdenticalVarsInSameScope_SemanticExceptionThrown() {
        Type varType = Type.INT;
        String varName = "varrr";

        VariableDeclarationContext varDecl1 = new VariableDeclarationContext(null, 0);
        varDecl1.type = varType;
        varDecl1.id = new CommonToken(NotCParser.ID, varName);

        VariableDeclarationContext varDecl2 = new VariableDeclarationContext(null, 0);
        varDecl2.type = varType;
        varDecl2.id = new CommonToken(NotCParser.ID, varName);

        symTab.addVar(varDecl1);
        SemanticException thrown = assertThrows(SemanticException.class, () -> {
            symTab.addVar(varDecl2);
        });
        String actualMessage = thrown.getMessage();
        assertTrue(actualMessage.contains("Redefinition of variable"));

    }

    @Test
    void AddVoidVar_SemanticExceptionThrown() {
        SemanticException thrown = assertThrows(SemanticException.class, () -> {
            VariableDeclarationContext varDecl = new VariableDeclarationContext(null, 0);
            varDecl.type = Type.VOID;
            varDecl.id = new CommonToken(NotCParser.ID, "varName");
            symTab.addVar(varDecl);
        });
        String actualMessage = thrown.getMessage();
        assertTrue(actualMessage.contains("Variables cannot have type void"));
    }

}
