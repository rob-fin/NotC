package notc.semantics;

import notc.antlrgen.NotCBaseVisitor;
import notc.antlrgen.NotCParser;
import notc.antlrgen.NotCParser.Type;
import notc.antlrgen.NotCParser.ProgramContext;
import notc.antlrgen.NotCParser.FunctionDefinitionContext;

import org.antlr.v4.runtime.CommonToken;

import java.util.List;

// Visitor for the highest-level construct in the grammar. Entry point for semantic analysis.
public class ProgramChecker extends NotCBaseVisitor<Void> {

    // Check the list of function definitions in two passes:
    // First to collect the id and type signature of each function.
    // Then to check their definitions in a context where these functions are declared.
    @Override
    public Void visitProgram(ProgramContext prog) {
        SymbolTable symTab = new SymbolTable();

        // Populate symbol table with functions
        for (FunctionDefinitionContext funDef : prog.funDefs) {
            Type returnType = funDef.returnType;
            FunctionType signature = new FunctionType(returnType, funDef.paramTypes);
            symTab.addFun(funDef.id, signature);
        }

        // Check that main is present and ok
        FunctionType mainType = symTab.lookupFun(new CommonToken(NotCParser.ID, "main"));
        if (mainType.arity() != 0)
            throw new SemanticException("Non-empty parameter list in function main");
        Type mainReturn = mainType.returnType();
        if (!mainReturn.isVoid())
            throw new SemanticException("Non-void return type declared for function main");

        // Check each function definition
        FunctionChecker funChecker = new FunctionChecker(symTab);
        for (FunctionDefinitionContext funDef : prog.funDefs)
            funChecker.checkDefinition(funDef);

        // Program is semantically sound and its parse tree now
        // has type annotations to be used by the code generator.
        return null;
    }

}