package com.strumenta.kolasu.antlr.parsing

import com.strumenta.kolasu.model.Node
import com.strumenta.kolasu.model.Source
import com.strumenta.kolasu.parsing.ParsingResult
import com.strumenta.kolasu.validation.Issue
import com.strumenta.simplelang.SimpleLangLexer
import com.strumenta.simplelang.SimpleLangParser
import org.antlr.v4.runtime.CharStream
import org.antlr.v4.runtime.Lexer
import org.antlr.v4.runtime.TokenStream
import org.junit.Test
import java.io.File
import java.nio.charset.Charset
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class SimpleLangKolasuParser : KolasuANTLRParser<Node, SimpleLangParser, SimpleLangParser.CompilationUnitContext,
    KolasuANTLRToken>(ANTLRTokenFactory()) {
    override fun createANTLRLexer(charStream: CharStream): Lexer {
        return SimpleLangLexer(charStream)
    }

    override fun createANTLRParser(tokenStream: TokenStream): SimpleLangParser {
        return SimpleLangParser(tokenStream)
    }

    override fun parseTreeToAst(
        parseTreeRoot: SimpleLangParser.CompilationUnitContext,
        considerRange: Boolean,
        issues: MutableList<Issue>,
        source: Source?
    ): Node? = null
}

class KolasuParserTest {

    @Test
    fun testLexing() {
        val parser = SimpleLangKolasuParser()
        val result = parser.parseFirstStage(
            """set a = 10
            |set b = ""
            |display c
            """.trimMargin()
        )
        assertNotNull(result)
        val lexingResult = parser.tokenFactory.extractTokens(result)
        assertNotNull(lexingResult)
        assertEquals(11, lexingResult.tokens.size)
        val text = lexingResult.tokens.map { it.text }
        assertEquals(listOf("set", "a", "=", "10", "set", "b", "=", "\"\"", "display", "c", "<EOF>"), text)
    }
}
