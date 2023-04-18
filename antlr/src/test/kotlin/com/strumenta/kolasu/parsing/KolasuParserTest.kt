package com.strumenta.kolasu.parsing

import com.strumenta.kolasu.model.Node
import com.strumenta.kolasu.validation.Issue
import com.strumenta.simplelang.SimpleLangLexer
import com.strumenta.simplelang.SimpleLangParser
import org.antlr.v4.runtime.CharStream
import org.antlr.v4.runtime.Lexer
import org.antlr.v4.runtime.TokenStream
import org.antlr.v4.runtime.tree.TerminalNode
import org.junit.Test
import java.io.InputStream
import java.nio.charset.Charset
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class SimpleLangKolasuParser : KolasuANTLRParser<Node, SimpleLangParser, SimpleLangParser.CompilationUnitContext,
    KolasuANTLRToken>() {
    override fun createANTLRLexer(charStream: CharStream): Lexer {
        return SimpleLangLexer(charStream)
    }

    override fun lex(
        inputStream: InputStream,
        charset: Charset,
        onlyFromDefaultChannel: Boolean
    ): LexingResult<KolasuANTLRToken> {
        TODO("Not yet implemented")
    }

    override fun createANTLRParser(tokenStream: TokenStream): SimpleLangParser {
        return SimpleLangParser(tokenStream)
    }

    override fun convertToken(terminalNode: TerminalNode): KolasuANTLRToken {
        return KolasuANTLRToken(TokenCategory.PLAIN_TEXT, terminalNode.symbol)
    }

    override fun parseTreeToAst(
        parseTreeRoot: SimpleLangParser.CompilationUnitContext,
        considerPosition: Boolean,
        issues: MutableList<Issue>
    ): Node? = null
}

class KolasuParserTest {

    @Test
    fun testLexing() {
        val parser = SimpleLangKolasuParser()
        val result = parser.parse(
            """set a = 10
            |set b = ""
            |display c
        """.trimMargin()
        )
        assertNotNull(result)
        val lexingResult = parser.extractTokens(result)
        assertNotNull(lexingResult)
        assertEquals(11, lexingResult.tokens.size)
        val text = lexingResult.tokens.map { it.text }
        assertEquals(listOf("set", "a", "=", "10", "set", "b", "=", "\"\"", "display", "c", "<EOF>"), text)
    }
}
