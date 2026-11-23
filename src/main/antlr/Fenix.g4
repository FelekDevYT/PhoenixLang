grammar Fenix;

program: moduleName? usingDeclaration* (classDeclaration | functionDeclaration)* EOF;
moduleName: 'module' STRING ';';
usingDeclaration: 'using' STRING ';';

classDeclaration: BASE_MODIFIER 'class' ID '{' classMember* '}';
functionDeclaration: BASE_MODIFIER 'func' ID '(' (arg (',' arg)*)? ')' ('->' type)? (block | '=' expr ';');
arg: ID ':' type;

classMember: classField ';' | classMethod;
classField: BASE_MODIFIER varDecl;
classMethod: functionDeclaration;

expr:
        '(' expr ')'                          #Parens
      | INT                                   #Int
      | STRING                                #String
      | FLOAT                                 #Float
      | BOOL                                  #Bool
      | NULL                                  #Null
      | expr 'is' type                        #Is
      | left=expr ('[' main+=expr ']')+       #ArrayGet
      | 'new' (ID | primitiveType) ('[' expr ']')+ #ArrayNewCreate
      | 'new' ID '(' (expr (',' expr)*)? ')'  #New
      | expr '.' ID '(' (expr (',' expr)*)? ')' #InvokeVirtualStatic
      | ID '(' (expr (',' expr)*)? ')'        #InvokeLocal
      | expr '.' ID                           #GetField
      | expr '.' ID '=' expr                  #SetField
      | ID                                    #Id
      | expr 'as' type                        #Cast
      | '-' expr                              #UnaryMinus
      | '!' expr                              #Not
      | expr op=('/' | '*' | '%') expr        #Multiplicative
      | expr op=('+' | '-') expr              #Additive
      | expr op=('==' | '!=' | '>=' | '<=' | '>' | '<') expr #EqCmp
      | expr op=('&&' | '||' | '^') expr      #Logic
      | 'if' (expr | '(' expr ')') statement ('else' statement)? #IfElse
      | 'while' ('(' expr ')' | expr) statement      #While
      |'do' statement 'while' ('(' expr ')' | expr) #DoWhile
      | 'for' (varDecl? ';' expr? ';' (assignmentDecl | inccDec)? | '(' varDecl? ';' expr? ';' (assignmentDecl | inccDec)? ')') statement #For
      | assignmentDecl                        #Assignment
      | inccDec                               #IncDec
      | varDecl                               #VariableDeclaration
      | 'break'                               #Break
      | 'continue'                            #Continue
      | 'return' expr?                        #Return
      | block                                 #BlockExpr
      ;

inccDec: ID (INC='++' | DEC='--' );
statement: expr | block;
block: '{' (expr ';')* '}';
varDecl: (VAR | CONST) ID (':' type)? ('=' expr)?;
assignmentDecl: (ID | ID ('[' index+=expr ']')+) op=('=' | '+=' | '-=' | '*=' | '/=' | '%=') right=expr;

type: base=('Int' | 'Str' | 'Float' | 'Bool' | 'Void' | 'Null' | ID) ('[' ']')*;
primitiveType: 'Int' | 'Str' | 'Float' | 'Bool';

BASE_MODIFIER: 'pub' | 'loc';

VAR: 'var';
CONST: 'const';
INT: [0-9]+;
FLOAT: [0-9]+ '.' [0-9]+;
STRING: '"' (~["\\"] | '\\' .)* '"';
BOOL: 'true' | 'false';
NULL: 'null';
ID: [a-zA-Z_] [a-zA-Z0-9_]*;

LINE_COMMENT: '//' ~[\r\n]* -> skip;
BLOCK_COMMENT: '/*' .*? '*/' -> skip;
WS: [ \t\r\n]+ -> skip;