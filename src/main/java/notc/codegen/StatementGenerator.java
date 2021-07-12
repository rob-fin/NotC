package notc.codegen;

import notc.antlrgen.NotCParser.SrcType;
import notc.antlrgen.NotCBaseVisitor;
import notc.antlrgen.NotCParser.StmContext;
import notc.antlrgen.NotCParser.DeclStmContext;
import notc.antlrgen.NotCParser.ExpStmContext;
import notc.antlrgen.NotCParser.IfStmContext;
import notc.antlrgen.NotCParser.IfElseStmContext;
import notc.antlrgen.NotCParser.WhileStmContext;
import notc.antlrgen.NotCParser.BlockStmContext;
import notc.antlrgen.NotCParser.ReturnStmContext;
import notc.antlrgen.NotCParser.InitStmContext;

import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.TerminalNode;
import com.google.common.collect.Lists;

import java.util.List;

// Visitor that generates Jasmin instructions from statements.
// Most instructions from here do something with what's put on the stack
// by the statements' constituent expressions (e.g. store, pop).
class StatementGenerator extends NotCBaseVisitor<Void> {
    private ExpressionGenerator expGen;
    private JvmMethod targetMethod; // Generated code goes here
    private static StatementGenerator instance = new StatementGenerator();
    private StatementGenerator() {}

    // Reuse same instance
    static StatementGenerator withTarget(JvmMethod method) {
        instance.targetMethod = method;
        instance.expGen = ExpressionGenerator.withTarget(method);
        return instance;
    }

    @Override
    public Void visitDeclStm(DeclStmContext decl) {
        SrcType t = decl.type().srcType;
        List<Token> varIds = Lists.transform(decl.ID(),
                                             TerminalNode::getSymbol);
        for (Token idTok : varIds)
            targetMethod.addVar(idTok, t);
        return null;
    }

    @Override
    public Void visitInitStm(InitStmContext init) {
        Token varIdTok = init.ID().getSymbol();
        SrcType varType = init.type().srcType;
        targetMethod.addVar(varIdTok, varType);
        expGen.visit(init.exp());
        String storeInstr;
        int stackChange;
        if (varType.isDouble()) {
            storeInstr = "dstore ";
            stackChange = -2;
        } else if (varType.isString()) {
            storeInstr = "astore ";
            stackChange = -1;
        } else { // ints, bools
            storeInstr = "istore ";
            stackChange = -1;
        }
        int varAddr = targetMethod.lookupVar(varIdTok);
        targetMethod.addInstruction(storeInstr + varAddr, stackChange);
        return null;
    }

    // Expression used as statement
    @Override
    public Void visitExpStm(ExpStmContext es) {
        expGen.visit(es.exp()); // Generate it
        SrcType expStmType = es.exp().typeAnnot;
        // Value is not used and should be popped
        if (expStmType.isVoid())
            return null; // Leaves nothing on the stack anyway
        else if (expStmType.isDouble())
            targetMethod.addInstruction("   pop2", -2);
        else // ints, bools, strings
            targetMethod.addInstruction("   pop", -1);
        return null;
    }

    @Override
    public Void visitReturnStm(ReturnStmContext ret) {
        if (ret.exp() == null) { // void return
            targetMethod.addInstruction("   return", 0);
            return null;
        }
        expGen.visit(ret.exp());
        SrcType returnedType = ret.exp().typeAnnot;
        if (returnedType.isDouble())
            targetMethod.addInstruction("   dreturn", -2);
        else if (returnedType.isString())
            targetMethod.addInstruction("   areturn", -1);
        else
            targetMethod.addInstruction("   ireturn", -1);
        return null;
    }

    @Override
    public Void visitBlockStm(BlockStmContext bl) {
        targetMethod.pushScope();
        for (StmContext stm : bl.stm())
            visit(stm);
        targetMethod.popScope();
        return null;
    }

    @Override
    public Void visitIfStm(IfStmContext _if) {
        String trueLabel = targetMethod.newLabel();
        String endLabel = targetMethod.newLabel();
        expGen.visit(_if.exp());
        targetMethod.addInstruction("    ifne " + trueLabel, -1);
        targetMethod.addInstruction("    goto " + endLabel, 0);
        targetMethod.addInstruction(trueLabel + ":", 0);
        targetMethod.pushScope();
        visit(_if.stm1);
        targetMethod.popScope();
        targetMethod.addInstruction(endLabel + ":", 0);
        return null;
    }

}
