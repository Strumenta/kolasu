package com.strumenta.kolasu.antlr.model

import com.strumenta.kolasu.antlr4j.parsing.ParseTreeOrigin
import com.strumenta.kolasu.antlr4j.parsing.toRange
import com.strumenta.kolasu.model.Node
import com.strumenta.kolasu.model.Point
import com.strumenta.kolasu.model.Range
import com.strumenta.simplelang.SimpleLangLexer
import com.strumenta.simplelang.SimpleLangParser
import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CommonTokenStream
import org.antlr.v4.runtime.tree.ParseTree
import kotlin.test.assertEquals
import org.junit.Test as test

data class MySetStatement(
    val specifiedRange: Range? = null,
) : Node(specifiedRange)

class RangeTest {
    @test
    fun parserRuleContextRange() {
        val code = "set foo = 123"
        val lexer = SimpleLangLexer(CharStreams.fromString(code))
        val parser = SimpleLangParser(CommonTokenStream(lexer))
        val cu = parser.compilationUnit()
        val setStmt = cu.statement(0) as SimpleLangParser.SetStmtContext
        val pos = setStmt.toRange()
        assertEquals(Range(Point(1, 0), Point(1, 13)), pos)
    }

    @test
    fun rangeDerivedFromParseTreeNode() {
        val code = "set foo = 123"
        val lexer = SimpleLangLexer(CharStreams.fromString(code))
        val parser = SimpleLangParser(CommonTokenStream(lexer))
        val cu = parser.compilationUnit()
        val setStmt = cu.statement(0) as SimpleLangParser.SetStmtContext
        val mySetStatement = MySetStatement()
        mySetStatement.origin = ParseTreeOrigin(setStmt)
        assertEquals(Range(Point(1, 0), Point(1, 13)), mySetStatement.range)
    }

    @test
    fun parserTreeRange() {
        val code = "set foo = 123"
        val lexer = SimpleLangLexer(CharStreams.fromString(code))
        val parser = SimpleLangParser(CommonTokenStream(lexer))
        val cu: ParseTree = parser.compilationUnit()
        val pos = cu.toRange()
        assertEquals(Range(Point(1, 0), Point(1, 13)), pos)
    }
}
