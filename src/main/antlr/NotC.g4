grammar NotC;


/* Type patch */

@parser::header {
import com.google.common.collect.Lists;
import java.util.List;
import java.util.Set;
}

@parser::members {
    // Enum representing the different types in the language.
    // Injected among the ANTLR-generated abstract syntax classes
    // and used instead of the generated TypeContexts
    // in the tree's type annotations.
    public enum Type {
        STRING,
        VOID,
        DOUBLE,
        INT,
        BOOL;

        private static final Set<Type> CONVERTIBLES = Set.of(
            BOOL, DOUBLE, INT
        );

        public boolean isBool() {
            return compareTo(BOOL) == 0;
        }

        public boolean isString() {
            return compareTo(STRING) == 0;
        }

        public boolean isVoid() {
            return compareTo(VOID) == 0;
        }

        public boolean isNumerical() {
            return compareTo(DOUBLE) >= 0;
        }

        public boolean isDouble() {
            return compareTo(DOUBLE) == 0;
        }

        public boolean isInt() {
            return compareTo(INT) == 0;
        }

        public boolean isConvertibleTo(Type t) {
            return CONVERTIBLES.contains(this) && CONVERTIBLES.contains(t);
        }

        // Resolves abstract syntax types to instances
        // of this enum using a utility visitor
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
// Saves type signature in context object's annotation fields when parsing
functionDefinition locals [Type returnType, List<Type> paramTypes]
@after {
    $ctx.returnType = $ctx.readReturnType.type;
    $ctx.paramTypes = Lists.transform($ctx.readParamTypes, t -> t.type);
}
    :
      readReturnType=typeToken
      id=ID
      LEFT_PAREN (readParamTypes+=typeToken paramIds+=ID (COMMA readParamTypes+=typeToken paramIds+=ID)*)? RIGHT_PAREN
      LEFT_BRACE body+=statement* RIGHT_BRACE
    ;

// Matches type tokens in a parser rule so that the Type enum can be used for static annotations
typeToken locals [Type type]
@after {
    $ctx.type = Type.resolve($ctx);
}
    : BOOL    # BoolType
    | DOUBLE  # DoubleType
    | INT     # IntType
    | STRING  # StringType
    | VOID    # VoidType
    ;

statement
    : typeDeclaration=typeToken varId=ID ASSIGN expr=expression STM_TERM                # InitializationStatement
    | typeDeclaration=typeToken varIds+=ID (COMMA varIds+=ID)* STM_TERM                 # DeclarationStatement
    | expr=expression STM_TERM                                                          # ExpressionStatement
    | 'return' expr=expression? STM_TERM                                                # ReturnStatement
    | LEFT_BRACE statements+=statement* RIGHT_BRACE                                     # BlockStatement
    | 'while' LEFT_PAREN expr=expression RIGHT_PAREN stm=statement                      # WhileStatement
    | 'if' '(' expr=expression ')' stm=statement                                        # IfStatement
    | 'if' LEFT_PAREN expr=expression RIGHT_PAREN stm1=statement 'else' stm2=statement  # IfElseStatement
    ;

// The type annotation is inferred and set during semantic analysis,
// as is a flag that denotes whether a widening primitive conversion should occur (e.g. ints used as doubles)
expression locals [Type type, Type runtimeConversion]
    : LEFT_PAREN expr=expression RIGHT_PAREN                                      # ParenthesizedExpression
    | 'false'                                                                     # FalseLiteralExpression
    | 'true'                                                                      # TrueLiteralExpression
    | value=DOUBLE_LIT                                                            # DoubleLiteralExpression
    | value=INT_LIT                                                               # IntLiteralExpression
    | value=STRING_LIT                                                            # StringLiteralExpression
    | varId=ID                                                                    # VariableExpression
    | id=ID LEFT_PAREN (args+=expression (COMMA args+=expression)*)? RIGHT_PAREN  # FunctionCallExpression
    | (preOp=INCR varId=ID  |
       varId=ID postOp=INCR |
       preOp=DECR varId=ID  |
       varId=ID postOp=DECR)                                                      # IncrementDecrementExpression
    | opnd1=expression op=(MUL | DIV | REM) opnd2=expression                      # ArithmeticExpression
    | opnd1=expression op=(ADD | SUB) opnd2=expression                            # ArithmeticExpression
    | opnd1=expression op=(LT | GT | GE | LE | EQ | NE) opnd2=expression          # ComparisonExpression
    | opnd1=expression op=AND opnd2=expression                                    # AndOrExpression
    | opnd1=expression op=OR  opnd2=expression                                    # AndOrExpression
    | varId=ID ASSIGN rhs=expression                                              # AssignmentExpression
    ;


// Lexer rules

BOOL   : 'bool' ;
DOUBLE : 'double' ;
INT    : 'int' ;
STRING : 'string';
VOID   : 'void' ;

COMMA       : ',' ;
LEFT_PAREN  : '(' ;
RIGHT_PAREN : ')' ;
LEFT_BRACE  : '{' ;
RIGHT_BRACE : '}' ;
STM_TERM    : ';' ;
ASSIGN      : '=' ;

INCR : '++' ;
DECR : '--' ;

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
