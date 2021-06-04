grammar NotC;

// Program: list of function definitions
program
    : def+
    ;

// Function definition: type, name, parameter list, body
def
    : type ID '(' params? ')' '{' stm* '}' 
    ;

// Built in types
type
    : 'bool'                              # BoolType
    | 'double'                            # DoubleType
    | 'int'                               # IntType
    | 'string'                            # StringType
    | 'void'                              # VoidType
    ;

// Parameter list
params
    : type ID (',' type ID)*           
    ;

// Statements
stm
    : type ID '=' exp ';'               # InitStm
    | type ID (',' ID)* ';'             # DeclStm
    | exp ';'                           # ExpStm
    | 'return' exp ';'                  # ReturnStm
    | '{' stm* '}'                      # BlockStm
    | 'while' '(' exp ')' stm           # WhileStm
    | 'if' '(' exp ')' stm 'else' stm   # IfElseStm
    ;

// Expressions in order of decreasing precedence
// Adds type annotation to each Exp class in the abstract syntax
exp returns [SrcType annot]
    : 'false'                           # FalseLitExp
    | 'true'                            # TrueLitExp
    | DOUBLE_LIT                        # DoubleLitExp
    | INT_LIT                           # IntLitExp
    | STRING_LIT                        # StringLitExp
    | ID                                # VarExp
    | ID '(' funCallArg? ')'            # FunCallExp
    | ID '++'                           # PostIncrExp
    | ID '--'                           # PostDecrExp
    | '++' ID                           # PreIncrExp
    | '--' ID                           # PreDecrExp
    | exp '*'  exp                      # MulExp
    | exp '/'  exp                      # DivExp
    | exp '+'  exp                      # AddExp
    | exp '-'  exp                      # SubExp
    | exp '<'  exp                      # LtExp
    | exp '>'  exp                      # GtExp
    | exp '>=' exp                      # GEqExp
    | exp '<=' exp                      # LEqExp
    | exp '==' exp                      # EqExp
    | exp '!=' exp                      # NEqExp
    | exp '&&' exp                      # AndExp
    | exp '||' exp                      # OrExp
    | ID '=' exp                        # AssExp
    ;

funCallArg : exp (',' exp)* ;


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
