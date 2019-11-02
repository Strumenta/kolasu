parser grammar SimpleLangParser;

options { tokenVocab = SimpleLangLexer; }

compilationUnit:
    statement+;

statement:
      DISPLAY expression
    | SET ID EQUAL expression
    | INPUT ID IS type
    ;

expression:
      INT_LIT
    | DEC_LIT
    | STRING_LIT
    | BOOLEAN_LIT
    | expression PLUS expression
    | expression MINUS expression
    | expression MULT expression
    | expression DIV expression
    ;

type:
       INT
     | DEC
     | STRING
     | BOOLEAN
     ;
