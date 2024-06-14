package com.strumenta.kolasu.antlr.model

import com.strumenta.kolasu.antlr4j.detachFromParseTree
import com.strumenta.kolasu.antlr4j.parsing.ParseTreeOrigin
import com.strumenta.kolasu.model.Node
import com.strumenta.kolasu.model.Point
import com.strumenta.kolasu.model.Range
import com.strumenta.kolasu.model.range
import com.strumenta.kolasu.model.withOrigin
import com.strumenta.kolasu.model.withRange
import com.strumenta.simplelang.SimpleLangLexer
import com.strumenta.simplelang.SimpleLangParser
import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CommonTokenStream
import kotlin.test.assertEquals
import kotlin.test.assertNull
import org.junit.Test as test

class OriginTest {
    @test
    fun parseTreeOriginRange() {
        val code =
            """set a = 1 + 2
            |input c is string
            |display 2 * 3
            """.trimMargin()
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
        node.detachFromParseTree()
        assertEquals(rootOrigin.range, node.range)
        node = Node().withOrigin(rootOrigin)
        assertEquals(rootOrigin.range, node.range)
        node.detachFromParseTree(keepRange = true)
        assertEquals(rootOrigin.range, node.range)
        node = Node().withOrigin(rootOrigin)
        assertEquals(rootOrigin.range, node.range)
        node.detachFromParseTree(keepRange = false)
        assertNull(node.origin)
        assertNull(node.range)
        node = Node().withOrigin(rootOrigin).withRange(Range(1, 2, 3, 4))
        assertEquals(Range(1, 2, 3, 4), node.range)
        node.detachFromParseTree(keepRange = false)
        assertNull(node.origin)
        assertEquals(Range(1, 2, 3, 4), node.range)
    }

    @test
    fun parseTreeOriginsSourceText() {
        val code =
            """set a = 1 + 2
            |input c is string
            |display 2 * 3
            """.trimMargin()
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
        node.detachFromParseTree()
        assertNull(node.sourceText)
        node = Node().withOrigin(rootOrigin)
        assertEquals(rootOrigin.sourceText, node.sourceText)
        node.detachFromParseTree(keepSourceText = true)
        assertEquals(rootOrigin.sourceText, node.sourceText)
        node = Node().withOrigin(rootOrigin)
        assertEquals(rootOrigin.sourceText, node.sourceText)
        node.detachFromParseTree(keepSourceText = false)
        assertNull(node.sourceText)
    }
}
