grammar NotC;


/* Type patch */

// Convenience classes for the language's types.
// Injected among the ANTLR-generated abstract syntax classes
// and used instead of the generated TypeContexts
// in the tree's type annotations.
@parser::header {
import com.google.common.collect.Lists;
import java.util.Collections;
import java.util.List;
import java.util.Set;
}

@parser::members {
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

        public char prefix() {
            switch (this) {
                case STRING: return 'a';
                case DOUBLE: return 'd';
                case INT:
                case BOOL:   return 'i'; // Treated as integer
                default:     return ' ';
            }
        }

        public String descriptor() {
            switch (this) {
                case BOOL:   return "Z";
                case VOID:   return "V";
                case STRING: return "Ljava/lang/String;";
                case INT:    return "I";
                case DOUBLE: return "D";
                default:     return "";
            }
        }

        public int size() {
            if (isDouble())
                return 2;
            if (isVoid())
                return 0;
            return 1;
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


    public static class Signature {
        private List<Type> paramTypes;
        private Type returnType;

        public Signature(Type returnType, List<Type> paramTypes) {
            this.returnType = returnType;
            this.paramTypes = paramTypes;
        }

        public int arity() {
            return paramTypes.size();
        }

        public List<Type> paramTypes() {
            return Collections.unmodifiableList(paramTypes);
        }

        public Type returnType() {
            return returnType;
        }

        public String methodDescriptor() {
            StringBuilder sb = new StringBuilder("\"(");
            for (Type t : paramTypes())
                sb.append(t.descriptor());
            sb.append(")" + returnType().descriptor() + "\"");
            return sb.toString();
        }
    }
}


/* Grammar start */

// Program: list of function definitions
program
    : funDefs+=functionDefinition* EOF
    ;

// Function definition: type, name, parameter list, body.
// Saves static type annotation in context object when parsing.
functionDefinition locals [Signature signature]
@after {
    Type returnType = $ctx.parsedReturn.type;
    List<Type> paramTypes = Lists.transform($ctx.parsedParamTypes, t -> t.type);
    $ctx.signature = new Signature(returnType, paramTypes);
}
    :
      parsedReturn=typeToken
      id=ID
      LEFT_PAREN (parsedParamTypes+=typeToken paramIds+=ID (COMMA parsedParamTypes+=typeToken paramIds+=ID)*)? RIGHT_PAREN
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

// The types of expressions are inferred during semantic analysis
expression locals [Type type, Type runtimeConversion]
    : LEFT_PAREN expr=expression RIGHT_PAREN                                      # ParenthesizedExpression
    | 'false'                                                                     # FalseLiteralExpression
    | 'true'                                                                      # TrueLiteralExpression
    | value=DOUBLE_LITERAL                                                        # DoubleLiteralExpression
    | value=INT_LITERAL                                                           # IntLiteralExpression
    | value=STRING_LITERAL                                                        # StringLiteralExpression
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

DOUBLE_LITERAL : '-'? (DIGIT+ '.' DIGIT+ | '.' DIGIT+) ;

INT_LITERAL : '-'? DIGIT+ ;

DIGIT : [0-9] ;

STRING_LITERAL : '"' (~["\n])* '"' ;

LETTER : [A-Za-z] ;

WHITE : [ \n\t]+ -> skip ;

COMMENT : (
            '//' .*? '\n' |
            '/*' .*? '*/'
          ) -> skip
        ;
