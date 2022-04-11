package com.strumenta.kolasu.model

import com.strumenta.kolasu.mapping.toPosition
import com.strumenta.kolasu.parsing.ParseTreeOrigin
import com.strumenta.simplelang.SimpleLangLexer
import com.strumenta.simplelang.SimpleLangParser
import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CommonTokenStream
import org.antlr.v4.runtime.tree.ParseTree
import kotlin.test.assertEquals
import kotlin.test.assertFails
import org.junit.Test as test

data class MySetStatement(val specifiedPosition: Position? = null) : Node(specifiedPosition)

class PositionTest {

    @test fun offset() {
        val code =
            """this is some code
                     |second line
                     |third line""".trimMargin("|")
        assertEquals(0, START_POINT.offset(code))
        assertEquals(5, Point(1, 5).offset(code))
        assertEquals(17, Point(1, 17).offset(code))
        assertEquals(18, Point(2, 0).offset(code))
        assertFails { Point(1, 18).offset(code) }
        assertFails { Point(4, 0).offset(code) }
    }

    @test fun pointCompare() {
        val p0 = START_POINT
        val p1 = Point(1, 1)
        val p2 = Point(1, 100)
        val p3 = Point(2, 90)

        assertEquals(false, p0 < p0)
        assertEquals(true, p0 <= p0)
        assertEquals(true, p0 >= p0)
        assertEquals(false, p0 > p0)

        assertEquals(true, p0 < p1)
        assertEquals(true, p0 <= p1)
        assertEquals(false, p0 >= p1)
        assertEquals(false, p0 > p1)

        assertEquals(true, p0 < p2)
        assertEquals(true, p0 <= p2)
        assertEquals(false, p0 >= p2)
        assertEquals(false, p0 > p2)

        assertEquals(true, p0 < p3)
        assertEquals(true, p0 <= p3)
        assertEquals(false, p0 >= p3)
        assertEquals(false, p0 > p3)

        assertEquals(true, p1 < p2)
        assertEquals(true, p1 <= p2)
        assertEquals(false, p1 >= p2)
        assertEquals(false, p1 > p2)

        assertEquals(true, p1 < p3)
        assertEquals(true, p1 <= p3)
        assertEquals(false, p1 >= p3)
        assertEquals(false, p1 > p3)
    }

    @test fun isBefore() {
        val p0 = START_POINT
        val p1 = Point(1, 1)
        val p2 = Point(1, 100)
        val p3 = Point(2, 90)

        assertEquals(false, p0.isBefore(p0))
        assertEquals(true, p0.isBefore(p1))
        assertEquals(true, p0.isBefore(p2))
        assertEquals(true, p0.isBefore(p3))

        assertEquals(false, p1.isBefore(p0))
        assertEquals(false, p1.isBefore(p1))
        assertEquals(true, p1.isBefore(p2))
        assertEquals(true, p1.isBefore(p3))

        assertEquals(false, p2.isBefore(p0))
        assertEquals(false, p2.isBefore(p1))
        assertEquals(false, p2.isBefore(p2))
        assertEquals(true, p2.isBefore(p3))

        assertEquals(false, p3.isBefore(p0))
        assertEquals(false, p3.isBefore(p1))
        assertEquals(false, p3.isBefore(p2))
        assertEquals(false, p3.isBefore(p3))
    }

    @test fun text() {
        val code =
            """this is some code
                     |second line
                     |third line""".trimMargin("|")
        assertEquals("", Position(START_POINT, START_POINT).text(code))
        assertEquals("t", Position(START_POINT, Point(1, 1)).text(code))
        assertEquals("this is some cod", Position(START_POINT, Point(1, 16)).text(code))
        assertEquals("this is some code", Position(START_POINT, Point(1, 17)).text(code))
        assertEquals("this is some code\n", Position(START_POINT, Point(2, 0)).text(code))
        assertEquals("this is some code\ns", Position(START_POINT, Point(2, 1)).text(code))
    }

    @test fun parserRuleContextPosition() {
        val code = "set foo = 123"
        val lexer = SimpleLangLexer(CharStreams.fromString(code))
        val parser = SimpleLangParser(CommonTokenStream(lexer))
        val cu = parser.compilationUnit()
        val setStmt = cu.statement(0) as SimpleLangParser.SetStmtContext
        val pos = setStmt.toPosition()
        assertEquals(Position(Point(1, 0), Point(1, 13)), pos)
    }

    @test fun positionDerivedFromParseTreeNode() {
        val code = "set foo = 123"
        val lexer = SimpleLangLexer(CharStreams.fromString(code))
        val parser = SimpleLangParser(CommonTokenStream(lexer))
        val cu = parser.compilationUnit()
        val setStmt = cu.statement(0) as SimpleLangParser.SetStmtContext
        val mySetStatement = MySetStatement()
        mySetStatement.origin = ParseTreeOrigin(setStmt)
        assertEquals(Position(Point(1, 0), Point(1, 13)), mySetStatement.position)
    }

    @test fun illegalPositionAccepted() {
        Position(Point(10, 1), Point(5, 2), validate = false)
    }

    @test(expected = Exception::class)
    fun illegalPositionNotAccepted() {
        Position(Point(10, 1), Point(5, 2), validate = true)
    }

    @test fun parserTreePosition() {
        val code = "set foo = 123"
        val lexer = SimpleLangLexer(CharStreams.fromString(code))
        val parser = SimpleLangParser(CommonTokenStream(lexer))
        val cu: ParseTree = parser.compilationUnit()
        val pos = cu.toPosition()
        assertEquals(Position(Point(1, 0), Point(1, 13)), pos)
    }

//    @test(expected = Exception::class) fun illegalPositionNotAcceptedByDefault() {
//        val p = Position(Point(10, 1), Point(5, 2))
//    }
}
