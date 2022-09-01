package com.strumenta.kolasu.language

import com.strumenta.kolasu.model.Node
import com.strumenta.kolasu.model.Position
import com.strumenta.kolasu.parsing.withParseTreeNode
import com.strumenta.simplelang.SimpleLangLexer
import com.strumenta.simplelang.SimpleLangParser
import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CommonTokenStream
import org.junit.Test
import kotlin.test.assertEquals

data class CU(val specifiedPosition: Position? = null, var statements: List<Node> = listOf()) : Node(specifiedPosition)
data class DisplayIntStatement(val specifiedPosition: Position? = null, val value: Int) : Node(specifiedPosition)
data class SetStatement(val specifiedPosition: Position? = null, var variable: String = "", val value: Int = 0) :
    Node(specifiedPosition)

class UnparserTest {

    @Test
    fun parserRuleContextPosition() {
        val code = "set foo = 123\ndisplay 456"
        val lexer = SimpleLangLexer(CharStreams.fromString(code))
        val parser = SimpleLangParser(CommonTokenStream(lexer))
        val pt = parser.compilationUnit()
        val cu = CU(
            statements = listOf(
                DisplayIntStatement(value = 456).withParseTreeNode(pt.statement(1)),
                SetStatement(variable = "foo", value = 123).withParseTreeNode(pt.statement(0))
            )
        ).withParseTreeNode(pt)
        assertEquals(code, Unparser().unparse(cu))
    }
}
