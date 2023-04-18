parser grammar AntlrScriptParser;

options {
    tokenVocab=AntlrScriptLexer; // Riferimento al lexer
}

script:
    (statements+=statement)*
    EOF
    ;

statement:
      CREATE entity=ID (AS var_name=ID)? #create_statement
    | SET feature=ID OF instance=expression TO value=expression #set_statement
    | PRINT message=expression #print_statement
    ;

expression:
      name=ID #reference_expression
    | entity=ID HASH id=expression #entity_by_id_expression
    | MINUS expression #minus_expression
    | INT_VALUE #int_literal_expression
    | STR_VALUE #string_literal_expression
    | left=expression op=(DIV|MULT) right=expression #div_mult_expression
    | left=expression op=(PLUS|MINUS) right=expression #sum_sub_expression
    | LPAREN expression RPAREN #parens_expression
    | feature=ID OF instance=expression #feature_access_expression
    | CONCAT left=expression AND right=expression #concat_expression
    ;


type_spec  // Regola definita per casi
    : INTEGER   #integer_type
    | BOOLEAN   #boolean_type
    | STRING    #string_type
    | target=ID #entity_type
    ;