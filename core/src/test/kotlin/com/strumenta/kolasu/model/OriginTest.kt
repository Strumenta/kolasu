package com.strumenta.kolasu.model

import com.strumenta.kolasu.parsing.ParseTreeOrigin
import com.strumenta.simplelang.SimpleLangLexer
import com.strumenta.simplelang.SimpleLangParser
import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CommonTokenStream
import kotlin.test.assertEquals
import kotlin.test.assertNull
import org.junit.Test as test

class OriginTest {

    @test fun parseTreeOriginPosition() {
        val code = """set a = 1 + 2
            |input c is string
            |display 2 * 3""".trimMargin()
        val lexer = SimpleLangLexer(CharStreams.fromString(code))
        val parser = SimpleLangParser(CommonTokenStream(lexer))
        val parseTreeRoot = parser.compilationUnit()
        println(parseTreeRoot)
        val rootOrigin = ParseTreeOrigin(parseTreeRoot)
        assertEquals(Range(Point(1, 0), Point(3, 13)), rootOrigin.range)

        val inputStatement = ParseTreeOrigin(parseTreeRoot.statement(1))
        assertEquals(Range(Point(2, 0), Point(2, 17)), inputStatement.range)

        var node = Node().withOrigin(rootOrigin)
        assertEquals(rootOrigin.range, node.range)
        node.detach()
        assertEquals(rootOrigin.range, node.range)
        node = Node().withOrigin(rootOrigin)
        assertEquals(rootOrigin.range, node.range)
        node.detach(keepPosition = true)
        assertEquals(rootOrigin.range, node.range)
        node = Node().withOrigin(rootOrigin)
        assertEquals(rootOrigin.range, node.range)
        node.detach(keepPosition = false)
        assertNull(node.origin)
        assertNull(node.range)
        node = Node().withOrigin(rootOrigin).withPosition(pos(1, 2, 3, 4))
        assertEquals(pos(1, 2, 3, 4), node.range)
        node.detach(keepPosition = false)
        assertNull(node.origin)
        assertEquals(pos(1, 2, 3, 4), node.range)
    }

    @test fun parseTreeOriginsSourceText() {
        val code = """set a = 1 + 2
            |input c is string
            |display 2 * 3""".trimMargin()
        val lexer = SimpleLangLexer(CharStreams.fromString(code))
        val parser = SimpleLangParser(CommonTokenStream(lexer))
        val parseTreeRoot = parser.compilationUnit()
        println(parseTreeRoot)
        val rootOrigin = ParseTreeOrigin(parseTreeRoot)
        assertEquals(code, rootOrigin.sourceText)

        val inputStatement = ParseTreeOrigin(parseTreeRoot.statement(1))
        val text = "input c is string"
        assertEquals(text, inputStatement.sourceText)

        var node = Node().withOrigin(rootOrigin)
        assertEquals(rootOrigin.sourceText, node.sourceText)
        node.detach()
        assertNull(node.sourceText)
        node = Node().withOrigin(rootOrigin)
        assertEquals(rootOrigin.sourceText, node.sourceText)
        node.detach(keepSourceText = true)
        assertEquals(rootOrigin.sourceText, node.sourceText)
        node = Node().withOrigin(rootOrigin)
        assertEquals(rootOrigin.sourceText, node.sourceText)
        node.detach(keepSourceText = false)
        assertNull(node.sourceText)
    }
}
