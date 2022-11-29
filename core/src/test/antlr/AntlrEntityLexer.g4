lexer grammar AntlrEntityLexer;

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

// Nomi (o identifier)
ID: [A-Z]+; // Notare il pattern tipo espressione regolare

// I caratteri di spaziatura non ci interessano, 
// dunque li nascondiamo al parser
WS: [ \r\n\t]+ -> channel(HIDDEN);