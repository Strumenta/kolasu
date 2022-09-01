package com.strumenta.kolasu.model

import com.strumenta.kolasu.parsing.ParseTreeOrigin
import com.strumenta.kolasu.parsing.toPosition
import com.strumenta.simplelang.SimpleLangLexer
import com.strumenta.simplelang.SimpleLangParser
import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CommonTokenStream
import org.antlr.v4.runtime.tree.ParseTree
import org.junit.Test
import kotlin.test.assertEquals

data class MySetStatement(val specifiedPosition: Position? = null) : Node(specifiedPosition)

class PositionTest {

    @Test
    fun parserTreePosition() {
        val code = "set foo = 123"
        val lexer = SimpleLangLexer(CharStreams.fromString(code))
        val parser = SimpleLangParser(CommonTokenStream(lexer))
        val cu: ParseTree = parser.compilationUnit()
        val pos = cu.toPosition()
        assertEquals(Position(Point(1, 0), Point(1, 13)), pos)
    }

    @Test
    fun parserRuleContextPosition() {
        val code = "set foo = 123"
        val lexer = SimpleLangLexer(CharStreams.fromString(code))
        val parser = SimpleLangParser(CommonTokenStream(lexer))
        val cu = parser.compilationUnit()
        val setStmt = cu.statement(0) as SimpleLangParser.SetStmtContext
        val pos = setStmt.toPosition()
        assertEquals(Position(Point(1, 0), Point(1, 13)), pos)
    }

    @Test
    fun positionDerivedFromParseTreeNode() {
        val code = "set foo = 123"
        val lexer = SimpleLangLexer(CharStreams.fromString(code))
        val parser = SimpleLangParser(CommonTokenStream(lexer))
        val cu = parser.compilationUnit()
        val setStmt = cu.statement(0) as SimpleLangParser.SetStmtContext
        val mySetStatement = MySetStatement()
        mySetStatement.origin = ParseTreeOrigin(setStmt)
        assertEquals(Position(Point(1, 0), Point(1, 13)), mySetStatement.position)
    }
}
