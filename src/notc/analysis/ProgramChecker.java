package notc.analysis;

import notc.analysis.NotCParser.ProgramContext;
import notc.analysis.NotCParser.DefContext;
import notc.analysis.NotCParser.TypeContext;
import notc.analysis.NotCParser.ParamsContext;
import notc.analysis.NotCParser.ExpContext;

import org.antlr.v4.runtime.tree.ParseTreeVisitor;
import org.antlr.v4.runtime.tree.ParseTreeProperty;

import org.antlr.v4.runtime.tree.TerminalNode;

import java.util.List;
import java.util.LinkedList;
import java.util.Collections;
import java.util.stream.Collectors;
import java.util.function.*;


public class ProgramChecker extends NotCBaseVisitor<Void> {

    @Override
    public Void visitProgram(ProgramContext ctx) {

        SymbolTable symTab = new SymbolTable();

        for (DefContext def : ctx.def()) {
            SrcType returnType = SrcType.resolve(def.returnType);
            List<SrcType> paramTypes = convertAstList(def.params().type(),
                                                      t -> SrcType.resolve(t));
            FunType signature = new FunType(returnType, paramTypes);
            String id = def.funId.getText();
            symTab.addFun(id, signature);
        }

        FunType mainType = symTab.lookupFun("main");
        if (mainType.getArity() != 0)
            throw new TypeException("Non-empty parameter list in function main");
        SrcType mainReturn = mainType.getReturnType();
        if (mainReturn != SrcType.VOID)
            throw new TypeException("Non-void return type declared for function main");

        FunctionChecker funChecker = new FunctionChecker(symTab);
        for (DefContext def : ctx.def()) {
            def.accept(funChecker);
        }
        return null;

    }

    // Utility function to convert lists with elements of ANTLR generated types
    // to lists with elements of some other type (e.g. TerminalNode list to String list)
    private <A,R> List<R> convertAstList(List<A> astList, Function<A,R> op) {
        if (astList == null)
            return Collections.<R>emptyList();
        return astList.stream().map(op).collect(Collectors.toList());
    }

}

