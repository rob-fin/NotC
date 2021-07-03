package notc.analysis;

import notc.analysis.NotCParser.SrcType;
import notc.analysis.NotCParser.ProgramContext;
import notc.analysis.NotCParser.DefContext;

import org.antlr.v4.runtime.CommonToken;
import com.google.common.collect.Lists;

import java.util.List;

// Visitor for the highest-level construct in the grammar. Entry point for semantic analysis.
public class ProgramChecker extends NotCBaseVisitor<Void> {

    /* Check the list of function definitions in two passes:
     * First to collect the id and type signature of each function.
     * Then to check their definitions in a context where these functions are declared. */
    @Override
    public Void visitProgram(ProgramContext prog) {
        SymbolTable symTab = new SymbolTable();

        // Populate symbol table with functions
        for (DefContext def : prog.def()) {
            SrcType returnType = SrcType.resolve(def.returnType);
            List<SrcType> paramTypes = Lists.transform(def.params().type(),
                                                       t -> SrcType.resolve(t));
            FunType signature = new FunType(returnType, paramTypes);
            symTab.addFun(def.funId, signature);
        }

        // Check that main is present and ok
        FunType mainType = symTab.lookupFun(new CommonToken(NotCParser.ID, "main"));
        if (mainType.arity() != 0)
            throw new SemanticException("Non-empty parameter list in function main");
        SrcType mainReturn = mainType.returnType();
        if (!mainReturn.isVoid())
            throw new SemanticException("Non-void return type declared for function main");

        // Check each function definition
        FunctionChecker funChecker = new FunctionChecker(symTab);
        for (DefContext def : prog.def())
            def.accept(funChecker);

        /* Program is semantically sound and its parse tree now
         * has type annotations to be used by the code generator. */
        return null;
    }

}
