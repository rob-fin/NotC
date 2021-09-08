package notc.semantics;

import notc.antlrgen.NotCBaseVisitor;
import notc.antlrgen.NotCParser;
import notc.antlrgen.NotCParser.Type;
import notc.antlrgen.NotCParser.Signature;
import notc.antlrgen.NotCParser.ProgramContext;
import notc.antlrgen.NotCParser.FunctionDefinitionContext;

import org.antlr.v4.runtime.CommonToken;

import java.util.List;

// Visitor for the highest-level construct in the grammar. Entry point for semantic analysis.
public class ProgramChecker extends NotCBaseVisitor<SymbolTable> {

    // Check the list of function definitions in two passes:
    // First to collect the id and type signature of each function.
    // Then to check their definitions in a context where these functions are declared.
    @Override
    public SymbolTable visitProgram(ProgramContext prog) {
        SymbolTable symTab = new SymbolTable();

        for (FunctionDefinitionContext funDef : prog.funDefs)
            symTab.addFun(funDef.id, funDef.signature);

        Signature mainSig = symTab.lookupFun(new CommonToken(NotCParser.ID, "main"));
        if (mainSig == null || mainSig.arity() != 0 || !mainSig.returnType().isVoid())
            throw new SemanticException("Function void main() undefined");

        FunctionChecker funChecker = new FunctionChecker(symTab);
        for (FunctionDefinitionContext funDef : prog.funDefs)
            funChecker.checkDefinition(funDef);

        // Program is semantically sound and its parse tree now
        // has type annotations to be used by the code generator.
        return symTab;
    }

}