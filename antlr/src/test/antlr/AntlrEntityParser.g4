parser grammar AntlrEntityParser;

options {
    tokenVocab=AntlrEntityLexer; // Riferimento al lexer
}

// Dichiarazione di una regola (o produzione).
// Le regole iniziano con la lettera minuscola.
module: 
    MODULE name=ID LCRLY  // Match di 3 token: MODULE, ID e LCRLY
        entities+=entity* // Match di 0 o più sottoregole entity
    RCRLY                 // Match di 1 token RCRLY
    EOF
    ;

entity:
    ENTITY name=ID LCRLY
        features+=feature*
    RCRLY
    ;

feature:
    name=ID COLON type=type_spec SEMI
    ;

type_spec  // Regola definita per casi
    : INTEGER   #integer_type
    | BOOLEAN   #boolean_type
    | STRING    #string_type
    | target=ID #entity_type
    ;