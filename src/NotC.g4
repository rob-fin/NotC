grammar NotC;

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

// Built in types
type
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
    | 'return' exp ';'                              # ReturnStm
    | '{' stm* '}'                                  # BlockStm
    | 'while' '(' exp ')' stm                       # WhileStm
    | 'if' '(' exp ')' stm1=stm 'else' stm2=stm     # IfElseStm
    ;

// Expressions in order of decreasing precedence
// Adds type annotation to each Exp class in the abstract syntax
exp locals [SrcType annot]
    : 'false'                                       # FalseLitExp
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

DOUBLE_LIT : DIGIT+ '.' DIGIT+
       | '.' DIGIT+
       ;

INT_LIT : DIGIT+ ;

DIGIT : [0-9] ;

STRING_LIT : '"' (~["\n])* '"' ;

LETTER : [A-Za-z] ;

WHITE : [ \n\t]+ -> skip ;

COMMENT : (
            '//' .*? '\n' |
            '/*' .*? '*/'
          ) -> skip
        ;
