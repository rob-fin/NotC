package notc.codegen;

class FunctionGenerator extends notc.antlrgen.NotCBaseVisitor<String> {
    private SymbolTable symTab;
    FunctionGenerator(SymbolTable symTab) {
        this.symTab = symTab;
    }
}
