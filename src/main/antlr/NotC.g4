grammar NotC;

/* Type patch */

@parser::members {
    // Enum representing the different types in the language. Instances of
    // ANTLR-generated abstract syntax classes are a bit awkward to compare to
    // each other, so this is injected among them and used instead of TypeContexts.
    public enum Type {
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
        public static Type resolve(TypeTokenContext ctx) {
            return ctx.accept(resolver);
        }

        private static TypeVisitor resolver = new TypeVisitor();

        static class TypeVisitor extends NotCBaseVisitor<Type> {

            @Override
            public Type visitBoolType(BoolTypeContext ctx) {
                ctx.type = BOOL;
                return BOOL;
            }

            @Override
            public Type visitDoubleType(DoubleTypeContext ctx) {
                ctx.type = DOUBLE;
                return DOUBLE;
            }

            @Override
            public Type visitIntType(IntTypeContext ctx) {
                ctx.type = INT;
                return INT;
            }

            @Override
            public Type visitStringType(StringTypeContext ctx) {
                ctx.type = STRING;
                return STRING;
            }

            @Override
            public Type visitVoidType(VoidTypeContext ctx) {
                ctx.type = VOID;
                return VOID;
            }

        }
    }
}


/* Grammar start */

// Program: list of function definitions
program
    : funDefs+=functionDefinition* EOF
    ;

// Function definition: type, name, parameter list, body
functionDefinition
    : returnType=typeToken
      id=ID
      '(' (paramTypes+=typeToken paramIds+=ID (',' paramTypes+=typeToken paramIds+=ID)*)? ')'
      '{' body+=statement* '}'
    ;

typeToken locals [Type type]
    : 'bool'   # BoolType
    | 'double' # DoubleType
    | 'int'    # IntType
    | 'string' # StringType
    | 'void'   # VoidType
    ;

statement
    : typeDecl=typeToken varId=ID '=' expr=expression ';'                # InitializationStatement
    | typeDecl=typeToken varIds+=ID (',' varIds+=ID)* ';'                # DeclarationStatement
    | expr=expression ';'                                                # ExpressionStatement
    | 'return' expr=expression? ';'                                      # ReturnStatement
    | '{' statements+=statement* '}'                                     # BlockStatement
    | 'while' '(' expr=expression ')' stm=statement                      # WhileStatement
    | 'if' '(' expr=expression ')' stm=statement                         # IfStatement
    | 'if' '(' expr=expression ')' stm1=statement 'else' stm2=statement  # IfElseStatement
    ;

// Expressions in order of decreasing precedence.
// Adds type annotation to each expression class in the abstract syntax.
expression locals [Type type]
    : '(' expr=expression ')'                                             # ParenthesizedExpression
    | 'false'                                                             # FalseLiteralExpression
    | 'true'                                                              # TrueLiteralExpression
    | value=DOUBLE_LIT                                                    # DoubleLiteralExpression
    | value=INT_LIT                                                       # IntLiteralExpression
    | value=STRING_LIT                                                    # StringLiteralExpression
    | varId=ID                                                            # VariableExpression
    | id=ID '(' (args+=expression (',' args+=expression)*)? ')'           # FunctionCallExpression
    | varId=ID '++'                                                       # PostIncrementExpression
    | varId=ID '--'                                                       # PostDecrementExpression
    | '++' varId=ID                                                       # PreIncrementExpression
    | '--' varId=ID                                                       # PreDecrementExpression
    | opnd1=expression op=(MUL | DIV | REM) opnd2=expression              # ArithmeticExpression
    | opnd1=expression op=(ADD | SUB)       opnd2=expression              # ArithmeticExpression
    | opnd1=expression op=(LT | GT | GE | LE | EQ | NE) opnd2=expression  # ComparisonExpression
    | opnd1=expression op=(AND | OR) opnd2=expression                     # AndOrExpression
    | varId=ID  ASS rhs=expression                                        # AssignmentExpression
    ;

// Lexer rules for operators, identifiers, literals, white space, and comments


MUL : '*'  ;
DIV : '/'  ;
REM : '%'  ;
ADD : '+'  ;
SUB : '-'  ;
LT  : '<'  ;
GT  : '>'  ;
GE  : '>=' ;
LE  : '<=' ;
EQ  : '==' ;
NE  : '!=' ;
AND : '&&' ;
OR  : '||' ;
ASS : '='  ;


ID  : LETTER (LETTER | DIGIT | '_')* ;

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
