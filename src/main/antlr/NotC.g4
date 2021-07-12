grammar NotC;

/* Type patch */

@parser::members {
    // Enum representing the different types in the language. Instances of
    // ANTLR-generated abstract syntax classes are a bit awkward to compare to
    // each other, so this is injected among them and used instead of TypeContexts.
    public enum SrcType {
        BOOL,
        STRING,
        VOID,
        INT,
        DOUBLE;

        public boolean isBool() {
            return compareTo(BOOL) == 0;
        }

        public boolean isString() {
            return compareTo(STRING) == 0;
        }

        public boolean isVoid() {
            return compareTo(VOID) == 0;
        }

        public boolean isInt() {
            return compareTo(INT) == 0;
        }

        public boolean isDouble() {
            return compareTo(DOUBLE) == 0;
        }

        public boolean isNumerical() {
            return compareTo(INT) >= 0;
        }

        // Resolves abstract syntax types to instances of
        // this enum at runtime using a utility visitor
        public static SrcType resolve(TypeContext ctx) {
            return ctx.accept(resolver);
        }

        private static TypeVisitor resolver = new TypeVisitor();

        static class TypeVisitor extends NotCBaseVisitor<SrcType> {

            @Override
            public SrcType visitBoolType(BoolTypeContext ctx) {
                ctx.srcType = BOOL;
                return BOOL;
            }

            @Override
            public SrcType visitDoubleType(DoubleTypeContext ctx) {
                ctx.srcType = DOUBLE;
                return DOUBLE;
            }

            @Override
            public SrcType visitIntType(IntTypeContext ctx) {
                ctx.srcType = INT;
                return INT;
            }

            @Override
            public SrcType visitStringType(StringTypeContext ctx) {
                ctx.srcType = STRING;
                return STRING;
            }

            @Override
            public SrcType visitVoidType(VoidTypeContext ctx) {
                ctx.srcType = VOID;
                return VOID;
            }

        }
    }
}


/* Grammar start */

// Program: list of function definitions
program
    : def* EOF
    ;

// Function definition: type, name, parameter list, body
def
    : returnType=type funId=ID '(' params ')' '{' stm* '}'
    ;

params
    : (type ID (',' type ID)*)? ;

// Built in types. Adds SrcType annotations to the generated
// TypeContext classes to make them easier to work with.
type locals [SrcType srcType]
    : 'bool'                                        # BoolType
    | 'double'                                      # DoubleType
    | 'int'                                         # IntType
    | 'string'                                      # StringType
    | 'void'                                        # VoidType
    ;

// Statements
stm
    : type ID '=' exp ';'                           # InitStm
    | type ID (',' ID)* ';'                         # DeclStm
    | exp ';'                                       # ExpStm
    | 'return' exp? ';'                             # ReturnStm
    | '{' stm* '}'                                  # BlockStm
    | 'while' '(' exp ')' stm                       # WhileStm
    | 'if' '(' exp ')' stm1=stm 'else'              # IfStm
    | 'if' '(' exp ')' stm1=stm 'else' stm2=stm     # IfElseStm
    ;

// Expressions in order of decreasing precedence.
// Adds type annotation to each Exp class in the abstract syntax.
exp locals [SrcType typeAnnot]
    : '(' expr=exp ')'                              # ParenthesizedExp
    | 'false'                                       # FalseLitExp
    | 'true'                                        # TrueLitExp
    | DOUBLE_LIT                                    # DoubleLitExp
    | INT_LIT                                       # IntLitExp
    | STRING_LIT                                    # StringLitExp
    | varId=ID                                      # VarExp
    | funId=ID '(' (exp (',' exp)*)? ')'            # FunCallExp
    | varId=ID '++'                                 # PostIncrExp
    | varId=ID '--'                                 # PostDecrExp
    | '++' varId=ID                                 # PreIncrExp
    | '--' varId=ID                                 # PreDecrExp
    | opnd1=exp '*'  opnd2=exp                      # MulExp
    | opnd1=exp '/'  opnd2=exp                      # DivExp
    | opnd1=exp '+'  opnd2=exp                      # AddExp
    | opnd1=exp '-'  opnd2=exp                      # SubExp
    | opnd1=exp '<'  opnd2=exp                      # LtExp
    | opnd1=exp '>'  opnd2=exp                      # GtExp
    | opnd1=exp '>=' opnd2=exp                      # GEqExp
    | opnd1=exp '<=' opnd2=exp                      # LEqExp
    | opnd1=exp '==' opnd2=exp                      # EqExp
    | opnd1=exp '!=' opnd2=exp                      # NEqExp
    | opnd1=exp '&&' opnd2=exp                      # AndExp
    | opnd1=exp '||' opnd2=exp                      # OrExp
    | varId=ID  '=' exp                             # AssExp
    ;

// Lexer rules for identifiers, literals, white space, and comments

ID
    : LETTER (LETTER | DIGIT | '_')*
    ;

DOUBLE_LIT : '-'? (DIGIT+ '.' DIGIT+ | '.' DIGIT+) ;

INT_LIT : '-'? DIGIT+ ;

DIGIT : [0-9] ;

STRING_LIT : '"' (~["\n])* '"' ;

LETTER : [A-Za-z] ;

WHITE : [ \n\t]+ -> skip ;

COMMENT : (
            '//' .*? '\n' |
            '/*' .*? '*/'
          ) -> skip
        ;
