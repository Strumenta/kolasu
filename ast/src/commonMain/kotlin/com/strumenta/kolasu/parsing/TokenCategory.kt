package com.strumenta.kolasu.parsing

data class TokenCategory(
    val type: String,
) {
    companion object {
        val COMMENT = TokenCategory("Comment")
        val KEYWORD = TokenCategory("Keyword")
        val NUMERIC_LITERAL = TokenCategory("Numeric literal")
        val STRING_LITERAL = TokenCategory("String literal")
        val PLAIN_TEXT = TokenCategory("Plain text")
        val commonCategories = listOf(COMMENT, KEYWORD, NUMERIC_LITERAL, STRING_LITERAL, PLAIN_TEXT)
    }
}
