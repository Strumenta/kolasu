package com.strumenta.kolasu.parsing

data class TokenCategory(
    val type: String,
) {
    companion object {
        val COMMENT = TokenCategory("Comment")
        val KEYWORD = TokenCategory("Keyword")
        val NUMERIC_LITERAL = TokenCategory("Numeric literal")
        val STRING_LITERAL = TokenCategory("String literal")
        val OTHER_LITERAL = TokenCategory("Other literal")
        val PLAIN_TEXT = TokenCategory("Plain text")
        val WHITESPACE = TokenCategory("Whitespace")
        val IDENTIFIER = TokenCategory("Identifier")
        val PUNCTUATION = TokenCategory("Punctuation")
        val OPERATOR = TokenCategory("Operator")
        val commonCategories =
            listOf(
                COMMENT, KEYWORD, NUMERIC_LITERAL, STRING_LITERAL, PLAIN_TEXT, WHITESPACE,
                IDENTIFIER, PUNCTUATION, OTHER_LITERAL, OTHER_LITERAL, OPERATOR,
            )
    }
}
