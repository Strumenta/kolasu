package com.strumenta.kolasu.antlr.model

import com.strumenta.kolasu.antlr.parsing.ParseTreeOrigin
import com.strumenta.kolasu.antlr.parsing.toPosition
import com.strumenta.kolasu.model.ASTNode
import com.strumenta.kolasu.model.Point
import com.strumenta.kolasu.model.Position
import com.strumenta.simplelang.SimpleLangLexer
import com.strumenta.simplelang.SimpleLangParser
import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CommonTokenStream
import org.antlr.v4.runtime.tree.ParseTree
import kotlin.test.assertEquals
import org.junit.Test as test

data class MySetStatement(val specifiedPosition: Position? = null) : ASTNode(specifiedPosition)

class PositionTest {

    @test
    fun parserRuleContextPosition() {
        val code = "set foo = 123"
        val lexer = SimpleLangLexer(CharStreams.fromString(code))
        val parser = SimpleLangParser(CommonTokenStream(lexer))
        val cu = parser.compilationUnit()
        val setStmt = cu.statement(0) as SimpleLangParser.SetStmtContext
        val pos = setStmt.toPosition()
        assertEquals(Position(Point(1, 0), Point(1, 13)), pos)
    }

    @test
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

    @test fun parserTreePosition() {
        val code = "set foo = 123"
        val lexer = SimpleLangLexer(CharStreams.fromString(code))
        val parser = SimpleLangParser(CommonTokenStream(lexer))
        val cu: ParseTree = parser.compilationUnit()
        val pos = cu.toPosition()
        assertEquals(Position(Point(1, 0), Point(1, 13)), pos)
    }
}
