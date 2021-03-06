grammar NotC;


/* Type patch */

@parser::header {
    import com.google.common.collect.Lists;
    import java.util.Collections;
    import java.util.Set;
}

// Convenience class for the language's types.
// Injected among the ANTLR-generated abstract syntax classes
// and used instead of the generated TypeContexts
// in the parse tree type annotations.
@parser::members {
    public enum Type {
        STRING(1, "Ljava/lang/String;"),
        VOID(0, "V"),
        DOUBLE(2, "D"),
        INT(1, "I"),
        BOOL(1, "Z");

        private static final Set<Type> CONVERTIBLES = Set.of(
            BOOL, DOUBLE, INT
        );

        private final int size;
        private final String descriptor;

        private Type(int size, String descriptor) {
            this.size = size;
            this.descriptor = descriptor;
        }

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

        public String descriptor() {
            return descriptor;
        }

        public int size() {
            return size;
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
                return BOOL;
            }

            @Override
            public Type visitDoubleType(DoubleTypeContext ctx) {
                return DOUBLE;
            }

            @Override
            public Type visitIntType(IntTypeContext ctx) {
                return INT;
            }

            @Override
            public Type visitStringType(StringTypeContext ctx) {
                return STRING;
            }

            @Override
            public Type visitVoidType(VoidTypeContext ctx) {
                return VOID;
            }
        }

        @Override
        public String toString() {
            return name().toLowerCase();
        }
    }

    // Makes parser rules aware of program's name for FQN precomputations
    static String programId;
    public static NotCParser from(TokenStream tokens, String id) {
        programId = id;
        return new NotCParser(tokens);
    }

}


/* Grammar start. See notc.ebnf for a more readable version. */

// List of function definitions
program
    : funDefs+=functionDefinition* EOF
    ;

// Header, body
functionDefinition
    : header=functionHeader
      LEFT_BRACE body+=statement* RIGHT_BRACE
    ;

// Type, name, parameter list
functionHeader locals [Type returnType, String specification, String fqn]
@after {
    $ctx.returnType = $ctx.parsedReturn.type;

    // Precomputes JVM specification
    StringBuilder sb = new StringBuilder($ctx.id.getText() + "(");
    for (Type t : Lists.transform($ctx.params, p -> p.type))
        sb.append(t.descriptor());
    sb.append(")").append($ctx.returnType.descriptor());
    $ctx.specification = sb.toString();
    $ctx.fqn = NotCParser.programId + "/" + $ctx.specification;
}
    : parsedReturn=typeToken
      id=ID
      LEFT_PAREN (params+=variableDeclaration (COMMA params+=variableDeclaration)*)? RIGHT_PAREN
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


variableDeclaration locals [Type type]
    : typeToken id=ID {$ctx.type = $typeToken.ctx.type;}
    ;


statement
    : varDecls+=variableDeclaration (COMMA additionalIds+=ID)* STM_TERM {
        // Desugars e.g. "int a, b" to "int a int b"
        for (Token t : $additionalIds) {
            VariableDeclarationContext varDecl =
                new VariableDeclarationContext($variableDeclaration.ctx.getParent(),
                                               $variableDeclaration.ctx.invokingState);
            varDecl.type = $variableDeclaration.ctx.type;
            varDecl.id = t;
            $varDecls.add(varDecl);
        }
    }                                                                              # DeclarationStatement
    | varDecl=variableDeclaration ASSIGN expr=expression STM_TERM                  # InitializationStatement
    | expr=expression STM_TERM                                                     # ExpressionStatement
    | 'return' expr=expression? STM_TERM                                           # ReturnStatement
    | LEFT_BRACE statements+=statement* RIGHT_BRACE                                # BlockStatement
    | 'for' LEFT_PAREN initExpr=expression? ';' conditionExpr=expression? ';'
          advanceExpr=expression? RIGHT_PAREN body=statement                       # ForStatement
    | 'while' LEFT_PAREN conditionExpr=expression RIGHT_PAREN loopedStm=statement  # WhileStatement
    | 'if' LEFT_PAREN conditionExpr=expression RIGHT_PAREN consequentStm=statement # IfStatement
    | 'if' LEFT_PAREN conditionExpr=expression RIGHT_PAREN
      consequentStm=statement 'else' altStm=statement                              # IfElseStatement
    | STM_TERM                                                                     # EmptyStatement
    ;


// The types of expressions are inferred during semantic analysis
expression locals [Type type, Type runtimeConversion]
    : '-' opnd=expression                                                          # NegationExpression
    | LEFT_PAREN expr=expression RIGHT_PAREN                                       # ParenthesizedExpression
    | 'false'                                                                      # FalseLiteralExpression
    | 'true'                                                                       # TrueLiteralExpression
    | value=DOUBLE_LITERAL                                                         # DoubleLiteralExpression
    | value=INT_LITERAL                                                            # IntLiteralExpression
    | value=STRING_LITERAL                                                         # StringLiteralExpression
    | varId=ID                                                                     # VariableExpression
    | id=ID LEFT_PAREN (args+=expression (COMMA args+=expression)*)? RIGHT_PAREN   # FunctionCallExpression
    | (preOp=INCR varId=ID  |
       varId=ID postOp=INCR)                                                       # IncrementExpression
    | (preOp=DECR varId=ID  |
       varId=ID postOp=DECR)                                                       # DecrementExpression
    | opnd1=expression op=(MUL | DIV | REM) opnd2=expression                       # ArithmeticExpression
    | opnd1=expression op=(ADD | SUB) opnd2=expression                             # ArithmeticExpression
    | opnd1=expression op=(LT | GT | GE | LE | EQ | NE) opnd2=expression           # ComparisonExpression
    | opnd1=expression op=AND opnd2=expression                                     # BinaryBooleanExpression
    | opnd1=expression op=OR  opnd2=expression                                     # BinaryBooleanExpression
    | varId=ID ASSIGN rhs=expression                                               # AssignmentExpression
    ;

headerDeclarations : headers+=functionHeader* ;


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


ID : LETTER (LETTER | DIGIT | '_')* ;

DOUBLE_LITERAL : DIGIT+ '.' DIGIT+ | '.' DIGIT+ ;

INT_LITERAL : DIGIT+ ;

DIGIT : [0-9] ;

STRING_LITERAL : '"' (~["\r\n])* '"' ;

LETTER : [A-Za-z] ;

WHITE : [ \r\n\t]+ -> skip ;

COMMENT : (
            '//' .*? '\r'? '\n' |
            '/*' .*? '*/'
          ) -> skip
        ;