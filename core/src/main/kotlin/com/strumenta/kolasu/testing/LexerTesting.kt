package com.strumenta.kolasu.testing

import com.strumenta.kolasu.model.START_POINT
import com.strumenta.kolasu.model.codeAtPosition
import com.strumenta.kolasu.parsing.KolasuLexer
import com.strumenta.kolasu.parsing.KolasuToken
import java.io.File
import kotlin.test.assertEquals

fun<T : KolasuToken> checkFileTokenization(file: File, lexer: KolasuLexer<T>) {
    require(file.exists())
    require(file.isFile())
    require(file.canRead())
    val code = file.readText()
    val lexingResult = lexer.lex(file)
    require(lexingResult.issues.isEmpty())
    checkTokensAreCoveringText(code, lexingResult.tokens)
}

fun<T : KolasuToken> checkTokensAreCoveringText(code: String, tokens: List<T>) {
    require(code.isEmpty() == tokens.isEmpty())
    if (code.isEmpty()) {
        return
    }

    // Tokens should be in order and they should cover without gaps or overlaps
    // the text from the very start to the very end of the code

    var prevToken: KolasuToken? = null
    tokens.forEach { token ->
        if (prevToken == null) {
            // This is the first token, so we should start at the very beginning
            assertEquals(token.position.start, START_POINT)
        } else {
            assertEquals(
                token.position.start,
                prevToken!!.position.end,
                "Token $token does not immediately follow $prevToken"
            )
        }

        // The text specified in tokens should be as long as the position indicated
        assertEquals(
            token.position.start + token.text,
            token.position.end,
            "We have a token with position ${token.position} and text '${token.text}'"
        )

        // The text specified in the tokens should correspond to the corresponding code
        val expectedText = code.codeAtPosition(token.position)
        assertEquals(
            expectedText,
            token.text,
            "At position ${token.position} we found '${token.text}' while we expected '$expectedText'"
        )

        prevToken = token
    }

    val codeEnd = START_POINT + code
    assertEquals(prevToken!!.position.end, codeEnd)
}
