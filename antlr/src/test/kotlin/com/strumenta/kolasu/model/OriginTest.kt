package com.strumenta.kolasu.model

import com.strumenta.kolasu.parsing.ParseTreeOrigin
import com.strumenta.simplelang.SimpleLangLexer
import com.strumenta.simplelang.SimpleLangParser
import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CommonTokenStream
import kotlin.test.assertEquals
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
        assertEquals(Position(Point(1, 0), Point(3, 13)), rootOrigin.position)

        val inputStatement = ParseTreeOrigin(parseTreeRoot.statement(1))
        assertEquals(Position(Point(2, 0), Point(2, 17)), inputStatement.position)
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
        assertEquals("input c is string", inputStatement.sourceText)
    }
}
