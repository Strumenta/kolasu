package com.strumenta.kolasu.antlr.parsing

import com.strumenta.kolasu.ast.Source
import com.strumenta.kolasu.model.NodeLike
import com.strumenta.kolasu.model.Point
import com.strumenta.kolasu.model.Range
import com.strumenta.kolasu.validation.Issue
import com.strumenta.simplelang.SimpleLangLexer
import com.strumenta.simplelang.SimpleLangParser
import org.antlr.v4.runtime.CharStream
import org.antlr.v4.runtime.Lexer
import org.antlr.v4.runtime.TokenStream
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class SimpleLangKolasuParser :
    KolasuANTLRParser<
        NodeLike,
        SimpleLangParser,
        SimpleLangParser.CompilationUnitContext,
        KolasuANTLRToken,
        >(ANTLRTokenFactory()) {
    override fun createANTLRLexer(charStream: CharStream): Lexer = SimpleLangLexer(charStream)

    override fun createANTLRParser(tokenStream: TokenStream): SimpleLangParser = SimpleLangParser(tokenStream)

    override fun parseTreeToAst(
        parseTreeRoot: SimpleLangParser.CompilationUnitContext,
        considerRange: Boolean,
        issues: MutableList<Issue>,
        source: Source?,
    ): NodeLike? = null

    override fun clearCaches() {
        super.clearCaches()
        cachesCounter++
    }

    public var cachesCounter = 0
}

class KolasuParserTest {
    @Test
    fun testLexing() {
        val parser = SimpleLangKolasuParser()
        val result =
            parser.parseFirstStage(
                """set a = 10
            |set b = ""
            |display c
                """.trimMargin(),
            )
        assertNotNull(result)
        val lexingResult = parser.tokenFactory.extractTokens(result)
        assertNotNull(lexingResult)
        assertEquals(11, lexingResult.tokens.size)
        val text = lexingResult.tokens.map { it.text }
        assertEquals(listOf("set", "a", "=", "10", "set", "b", "=", "\"\"", "display", "c", "<EOF>"), text)
    }

    @Test
    fun clearCache() {
        val parser = SimpleLangKolasuParser()
        parser.parse(
            """set a = 10
            |set b = ""
            |display c
            """.trimMargin(),
        )
        parser.executionsToNextCacheClean = 0
        assertEquals(0, parser.cachesCounter)
        parser.parse(
            """set a = 10
            |set b = ""
            |display c
            """.trimMargin(),
        )
        assertEquals(1, parser.cachesCounter)
    }

    @Test
    fun issuesAreCapitalized() {
        val parser = SimpleLangKolasuParser()
        val result =
            parser.parse(
                """set set a = 10
            |display c
                """.trimMargin(),
            )
        assert(result.issues.isNotEmpty())
        assertNotNull(result.issues.find { it.message.startsWith("Extraneous input 'set'") })
        assertNotNull(result.issues.find { it.message.startsWith("Mismatched input 'c'") })
    }

    @Test
    fun issuesHaveNotFlatPosition() {
        val parser = SimpleLangKolasuParser()
        val result =
            parser.parse(
                """set set a = 10
            |display c
                """.trimMargin(),
            )
        assert(result.issues.isNotEmpty())
        assert(result.issues.none { it.range?.isFlat ?: false })
        val extraneousInput = result.issues.find { it.message.startsWith("Extraneous input 'set'") }!!
        assertEquals(Range(Point(1, 4), Point(1, 7)), extraneousInput.range)
        val mismatchedInput = result.issues.find { it.message.startsWith("Mismatched input 'c'") }!!
        assertEquals(Range(Point(2, 8), Point(2, 9)), mismatchedInput.range)
    }
}
