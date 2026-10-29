grammar Fenix;

program: usingDeclaration* functionDeclaration* EOF;
usingDeclaration: 'using' STRING ';';

functionDeclaration: 'func' ID '(' (arg (',' arg)*)? ')' ('->' type)? (block | '=' expr);
arg: ID ':' type;

expr:
      expr op=('/' | '*' | '%') expr        #Multiplicative
    | expr op=('+' | '-') expr              #Additive
    | ID                                    #Id
    | INT                                   #Int
    | STRING                                #String
    | FLOAT                                 #Float
    | BOOL                                  #Bool
    | expr op=('==' | '!=' | '>=' | '<=' | '>' | '<') expr #EqCmp
    | 'if' '('? expr ')'? statement ('else' statement)? #IfElse
    | ID '.' ID '(' (expr (',' expr)*)? ')' #InvokeStatic
    | ID '(' (expr (',' expr)*)? ')'        #InvokeLocal
    | (VAR | CONST) ID (':' type)? ('=' expr)? #VariableDeclaration
    | 'return' expr?                        #Return
    ;

statement: expr | block;
block: '{' (expr ';')* '}';

type: base=('Int' | 'Str' | 'Float' | 'Bool') ('[' ']')*;

VAR: 'var';
CONST: 'const';

INT: [0-9]+;
FLOAT: [0-9]+ '.' [0-9]+;
STRING: '"' (~["\\"] | '\\' .)* '"' | '"""' ('"'? ~["\\"] | '\\' .)* '"""';
BOOL: 'true' | 'false';
ID: [a-zA-Z_] [a-zA-Z0-9_]*;

LINE_COMMENT: '//' ~[\r\n]* -> skip;
BLOCK_COMMENT: '/*' .*? '*/' -> skip;
WS: [ \t\r\n]+ -> skip;