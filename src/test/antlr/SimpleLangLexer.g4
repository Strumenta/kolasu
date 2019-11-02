lexer grammar SimpleLangLexer;

channels {
    WS_CHANNEL
}

WS : [ \t]+ -> channel(WS_CHANNEL);
NEWLINE : '\r\n' | '\r' | '\n' ;

DISPLAY : 'display' ;
SET : 'set' ;
INPUT : 'input' ;
IS : 'is' ;

PLUS : '+' ;
MINUS : '-' ;
MULT : '*' ;
DIV : '/' ;
EQUAL : '=' ;

INT : 'int' ;
DEC : 'dec' ;
STRING : 'string' ;
BOOLEAN : 'boolean' ;

INT_LIT : [0-9]+ ;
DEC_LIT : [0-9]+ '.' [0-9]+ ;
STRING_LIT : '"' '"' ;
BOOLEAN_LIT : 'false' | 'true' ;

ID : [a-zA-Z][a-zA-Z_0-9]* ;
