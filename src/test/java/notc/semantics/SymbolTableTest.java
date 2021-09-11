package notc.semantics;

import notc.antlrgen.NotCParser;
import notc.antlrgen.NotCParser.Type;
import notc.antlrgen.NotCParser.FunctionHeaderContext;
import notc.antlrgen.NotCParser.VariableDeclarationContext;

import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.CommonToken;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class SymbolTableTest {

    private SymbolTable symTab;

    @BeforeEach
    void init() {
        symTab = new SymbolTable();
        symTab.pushScope();
    }

    @Test
    void AddFun_LookupSucceeds() {
        FunctionHeaderContext funHeader = new FunctionHeaderContext(null, 0);
        String funName = "fffun";
        funHeader.id = new CommonToken(NotCParser.ID, funName);
        symTab.declareFunction(funHeader);
        Token funRef = new CommonToken(NotCParser.ID, funName);
        FunctionHeaderContext lookedUpFun = symTab.lookupFunction(funRef);
        assertSame(funHeader, lookedUpFun);
    }

    @Test
    void RedefineFun_SemanticExceptionThrown() {
        String funName = "funFun";
        FunctionHeaderContext funHeader1 = new FunctionHeaderContext(null, 0);
        funHeader1.id = new CommonToken(NotCParser.ID, funName);
        FunctionHeaderContext funHeader2 = new FunctionHeaderContext(null, 0);
        funHeader2.id = new CommonToken(NotCParser.ID, funName);
        symTab.declareFunction(funHeader1);
        SemanticException thrown = assertThrows(SemanticException.class, () ->
            symTab.declareFunction(funHeader2)
        );
        String actualMessage = thrown.getMessage();
        assertTrue(actualMessage.contains("Redefinition of function"));
    }

    @Test
    void AddVar_LookupSucceeds() {
        String varName = "var";
        VariableDeclarationContext varDecl = new VariableDeclarationContext(null, 0);
        varDecl.type = Type.STRING;
        varDecl.id = new CommonToken(NotCParser.ID, varName);
        symTab.declareVariable(varDecl);
        Token varRef = new CommonToken(NotCParser.ID, varName);
        symTab.resolveVarReference(varRef);
        VariableDeclarationContext lookedUpVar = symTab.lookupVariable(varRef);
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

        symTab.declareVariable(varDecl1);
        symTab.pushScope();
        assertDoesNotThrow( () ->
            symTab.declareVariable(varDecl2)
        );
    }

    @Test
    void LookupVarInDeepScope_LookupSucceeds() {
        VariableDeclarationContext varDecl = new VariableDeclarationContext(null, 0);
        String varName = "iAmDeep";
        varDecl.type = Type.DOUBLE;
        varDecl.id = new CommonToken(NotCParser.ID, varName);
        symTab.declareVariable(varDecl);
        for (int i = 0; i < 20; i++)
            symTab.pushScope();
        Token varRef = new CommonToken(NotCParser.ID, varName);
        symTab.resolveVarReference(varRef);
        assertSame(varDecl, symTab.lookupVariable(varRef));
    }

    @Test
    void ResolveUndefinedVar_SemanticExceptionThrown() {
        SemanticException thrown = assertThrows(SemanticException.class, () ->
            symTab.resolveVarReference(new CommonToken(NotCParser.ID, "undefVar"))
        );
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
        symTab.declareVariable(varDecl);
        symTab.popScope();
        Token varRef = new CommonToken(NotCParser.ID, varName);
        SemanticException thrown = assertThrows(SemanticException.class, () ->
            symTab.resolveVarReference(varRef)
        );
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

        symTab.declareVariable(varDecl1);
        SemanticException thrown = assertThrows(SemanticException.class, () ->
            symTab.declareVariable(varDecl2)
        );
        String actualMessage = thrown.getMessage();
        assertTrue(actualMessage.contains("Redefinition of variable"));

    }

    @Test
    void AddVoidVar_SemanticExceptionThrown() {
        VariableDeclarationContext varDecl = new VariableDeclarationContext(null, 0);
        varDecl.type = Type.VOID;
        varDecl.id = new CommonToken(NotCParser.ID, "varName");
        SemanticException thrown = assertThrows(SemanticException.class, () ->
            symTab.declareVariable(varDecl)
        );
        String actualMessage = thrown.getMessage();
        assertTrue(actualMessage.contains("Variables cannot have type void"));
    }

}
