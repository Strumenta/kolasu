lexer grammar AntlrScriptLexer;

options { 
    caseInsensitive=true; //Opzione aggiunta recentemente, ignora il case
}

// Token per i tipi
INTEGER: 'integer'; // I nomi di token iniziano con la maiuscola
BOOLEAN: 'boolean'; // Per convenzione si scrivono in ALL_CAPS
STRING: 'string';   // Ogni token ha il suo pattern
// Questi token corrispondono ad una stringa fissa (case insensitive)

// Idem per le keyword (parole chiave)
ENTITY: 'entity';
MODULE: 'module';

// Segni di punteggiatura
COLON: ':';
SEMI: ';';
LSQRD: '[';
RSQRD: ']';
LCRLY: '{';
RCRLY: '}';
LPAREN: '(';
RPAREN: ')';

DIV: '/';
MULT: '*';
PLUS: '+';
MINUS: '-';
HASH: '#';

CREATE: 'create';
AS: 'as';
SET: 'set';
OF: 'of';
TO: 'to';
PRINT: 'print';
CONCAT: 'concat';
AND: 'and';

// Nomi (o identifier)
ID: [a-zA-Z][a-zA-Z0-9_]*; // Notare il pattern tipo espressione regolare
INT_VALUE: '0'|[1-9][0-9]*;
STR_VALUE: '\'' ~['\r\n]* '\'';

// I caratteri di spaziatura non ci interessano, 
// dunque li nascondiamo al parser
WS: [ \r\n\t]+ -> channel(HIDDEN);