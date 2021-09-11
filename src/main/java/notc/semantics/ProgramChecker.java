package notc.semantics;

import notc.antlrgen.NotCBaseVisitor;
import notc.antlrgen.NotCParser;
import notc.antlrgen.NotCLexer;
import notc.antlrgen.NotCParser.ProgramContext;
import notc.antlrgen.NotCParser.FunctionDefinitionContext;
import notc.antlrgen.NotCParser.FunctionHeaderContext;
import notc.antlrgen.NotCParser.HeaderDeclarationsContext;

import org.antlr.v4.runtime.CommonToken;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import com.google.common.collect.Lists;

import java.io.InputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

// Entry point for semantic analysis of a program
public class ProgramChecker extends NotCBaseVisitor<SymbolTable> {

    // Instantiates the symbol table and declares the built-in functions.
    // Then checks the functions of the program in two passes:
    // * First by trying to declare them.
    // * Then by checking their definitions in a context where all these functions are declared.
    @Override
    public SymbolTable visitProgram(ProgramContext prog) {

        List<FunctionHeaderContext> headers = getBuiltinHeaders();
        headers.addAll(Lists.transform(prog.funDefs, fun -> fun.header));

        SymbolTable symTab = new SymbolTable();
        for (FunctionHeaderContext header : headers)
            symTab.declareFunction(header);

        FunctionHeaderContext main = symTab.lookupFunction(new CommonToken(NotCParser.ID, "main"));
        if (main == null || !main.params.isEmpty() || !main.returnType.isVoid())
            throw new SemanticException("Function void main() undefined");

        FunctionChecker funChecker = new FunctionChecker(symTab);
        for (FunctionDefinitionContext funDef : prog.funDefs)
            funChecker.checkDefinition(funDef);

        // Program is semantically sound
        return symTab;
    }

    // Retrieves headers of built-in functions so they can be added to symbol table
    private List<FunctionHeaderContext> getBuiltinHeaders() {
        CharStream input;
        try (InputStream is = getClass().getResourceAsStream("/builtin_headers.notc")) {
            input = CharStreams.fromStream(is, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        NotCLexer lexer = new NotCLexer(input);
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        NotCParser parser = new NotCParser(tokens);
        ParseTree tree = parser.headerDeclarations();
        return tree.accept(
            new NotCBaseVisitor<List<FunctionHeaderContext>>() {
                @Override
                public List<FunctionHeaderContext> visitHeaderDeclarations(HeaderDeclarationsContext ctx) {
                    return ctx.headers;
                }
            }
        );
    }

}