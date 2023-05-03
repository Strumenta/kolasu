package com.strumenta.kolasu.antlr.model

import com.strumenta.kolasu.antlr.parsing.ParseTreeOrigin
import com.strumenta.kolasu.model.ASTNode
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

    @test fun parseTreeOriginRange() {
        val code = """set a = 1 + 2
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

        var node = ASTNode().withOrigin(rootOrigin)
        assertEquals(rootOrigin.range, node.range)
        node.detach()
        assertEquals(rootOrigin.range, node.range)
        node = ASTNode().withOrigin(rootOrigin)
        assertEquals(rootOrigin.range, node.range)
        node.detach(keepRange = true)
        assertEquals(rootOrigin.range, node.range)
        node = ASTNode().withOrigin(rootOrigin)
        assertEquals(rootOrigin.range, node.range)
        node.detach(keepRange = false)
        assertNull(node.origin)
        assertNull(node.range)
        node = ASTNode().withOrigin(rootOrigin).withRange(range(1, 2, 3, 4))
        assertEquals(range(1, 2, 3, 4), node.range)
        node.detach(keepRange = false)
        assertNull(node.origin)
        assertEquals(range(1, 2, 3, 4), node.range)
    }

    @test fun parseTreeOriginsSourceText() {
        val code = """set a = 1 + 2
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

        var node = ASTNode().withOrigin(rootOrigin)
        assertEquals(rootOrigin.sourceText, node.sourceText)
        node.detach()
        assertNull(node.sourceText)
        node = ASTNode().withOrigin(rootOrigin)
        assertEquals(rootOrigin.sourceText, node.sourceText)
        node.detach(keepSourceText = true)
        assertEquals(rootOrigin.sourceText, node.sourceText)
        node = ASTNode().withOrigin(rootOrigin)
        assertEquals(rootOrigin.sourceText, node.sourceText)
        node.detach(keepSourceText = false)
        assertNull(node.sourceText)
    }
}
