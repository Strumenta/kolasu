package com.strumenta.kolasu.model

import com.strumenta.simplelang.SimpleLangLexer
import com.strumenta.simplelang.SimpleLangParser
import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CommonTokenStream
import kotlin.test.assertEquals
import org.junit.Test as test

class OriginTest {

    @test fun parseTreeNodeOriginPosition() {
        val code = """set a = 1 + 2
            |input c is string
            |display 2 * 3""".trimMargin()
        val lexer = SimpleLangLexer(CharStreams.fromString(code))
        val parser = SimpleLangParser(CommonTokenStream(lexer))
        val parseTreeRoot = parser.compilationUnit()
        println(parseTreeRoot)
        val rootOrigin = ParseTreeNodeOrigin(parseTreeRoot)
        assertEquals(Position(Point(1,0), Point(3, 13)), rootOrigin.position)
        assertEquals(code, rootOrigin.sourceText)

        val inputStatement = ParseTreeNodeOrigin(parseTreeRoot.statement(1))
        assertEquals(Position(Point(2,0), Point(2, 17)), inputStatement.position)
        assertEquals("input c is string", inputStatement.sourceText)
    }
}
