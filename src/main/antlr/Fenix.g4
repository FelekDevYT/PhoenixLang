grammar Fenix;

program: usingDeclaration* functionDeclaration* EOF;
usingDeclaration: 'using' STRING ';';

functionDeclaration: 'func' ID '(' (arg (',' arg)*)? ')' ('->' type)? (block | '=' expr);
arg: ID ':' type;

expr:
        '(' expr ')'                          #Parens
      | INT                                   #Int
      | STRING                                #String
      | FLOAT                                 #Float
      | BOOL                                  #Bool
      | left=expr ('[' main+=expr ']')+       #ArrayGet
      | type ('[' expr ']')+                  #ArrayCreate
      | 'new' ID '(' (expr (',' expr)*)? ')'  #New
      | ID '.' ID '(' (expr (',' expr)*)? ')' #InvokeStatic
      | ID '(' (expr (',' expr)*)? ')'        #InvokeLocal
      | ID                                    #Id
      | expr 'as' type                        #Cast
      | '-' expr                              #UnaryMinus
      | '!' expr                              #Not
      | expr op=('/' | '*' | '%') expr        #Multiplicative
      | expr op=('+' | '-') expr              #Additive
      | expr op=('==' | '!=' | '>=' | '<=' | '>' | '<') expr #EqCmp
      | expr op=('&&' | '||' | '^') expr      #Logic
      | 'if' '('? expr ')'? statement ('else' statement)? #IfElse
      | 'while' '('? expr ')'? statement      #While
      | 'for' '('? varDecl? ';' expr ';' (assignmentDecl | inccDec)')'? statement #For
      | assignmentDecl                        #Assignment
      | inccDec                               #IncDec
      | varDecl                               #VariableDeclaration
      | 'break'                               #Break
      | 'continue'                            #Continue
      | 'return' expr?                        #Return
      ;

inccDec: ID (INC='++' | DEC='--' );
statement: expr | block;
block: '{' (expr ';')* '}';
varDecl: (VAR | CONST) ID (':' type)? ('=' expr)?;
assignmentDecl: (ID | ID ('[' index+=expr ']')+) '=' right=expr;

type: base=('Int' | 'Str' | 'Float' | 'Bool' | 'Void' | ID) ('[' ']')*;

VAR: 'var';
CONST: 'const';
INT: [0-9]+;
FLOAT: [0-9]+ '.' [0-9]+;
STRING: '"' (~["\\"] | '\\' .)* '"';
BOOL: 'true' | 'false';
ID: [a-zA-Z_] [a-zA-Z0-9_]*;

LINE_COMMENT: '//' ~[\r\n]* -> skip;
BLOCK_COMMENT: '/*' .*? '*/' -> skip;
WS: [ \t\r\n]+ -> skip;