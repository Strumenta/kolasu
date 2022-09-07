package com.strumenta.kolasu.mapping

import com.strumenta.kolasu.model.*
import com.strumenta.kolasu.parsing.withParseTreeNode
import com.strumenta.kolasu.testing.assertASTsAreEqual
import com.strumenta.kolasu.transformation.ASTTransformer
import com.strumenta.kolasu.transformation.GenericNode
import com.strumenta.simplelang.SimpleLangLexer
import com.strumenta.simplelang.SimpleLangParser
import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CommonTokenStream
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

data class CU(val specifiedPosition: Position? = null, var statements: List<Node> = listOf()) : Node(specifiedPosition)
data class DisplayIntStatement(val specifiedPosition: Position? = null, val value: Int) : Node(specifiedPosition)
data class SetStatement(val specifiedPosition: Position? = null, var variable: String = "", val value: Int = 0) :
    Node(specifiedPosition)

class ParseTreeToASTTransformerTest {

    @Test
    fun testParseTreeTransformer() {
        val code = "set foo = 123\ndisplay 456"
        val lexer = SimpleLangLexer(CharStreams.fromString(code))
        val parser = SimpleLangParser(CommonTokenStream(lexer))
        val pt = parser.compilationUnit()

        val transformer = ParseTreeToASTTransformer()
        configure(transformer)

        val cu = CU(
            statements = listOf(
                SetStatement(variable = "foo", value = 123).withParseTreeNode(pt.statement(0)),
                DisplayIntStatement(value = 456).withParseTreeNode(pt.statement(1))
            )
        ).withParseTreeNode(pt)
        val transformedCU = transformer.transform(pt)!!
        assertASTsAreEqual(cu, transformedCU, considerPosition = true)
        assertTrue { transformedCU.hasValidParents() }
        assertNull(transformedCU.invalidPositions().firstOrNull())
    }

    @Test
    fun testTransformationWithErrors() {
        val code = "set foo = \ndisplay @@@"
        val lexer = SimpleLangLexer(CharStreams.fromString(code))
        val parser = SimpleLangParser(CommonTokenStream(lexer))
        val pt = parser.compilationUnit()
        assertEquals(2, parser.numberOfSyntaxErrors)

        val transformer = ParseTreeToASTTransformer()
        configure(transformer)

        val cu = CU(
            statements = listOf(
                GenericErrorNode(message = "Exception java.lang.NullPointerException")
                    .withParseTreeNode(pt.statement(0)),
                GenericErrorNode(message = "Exception java.lang.IllegalStateException: Parse error")
                    .withParseTreeNode(pt.statement(1))
            )
        ).withParseTreeNode(pt)
        val transformedCU = transformer.transform(pt)!! as CU
        assertASTsAreEqual(cu, transformedCU, considerPosition = true)
        assertTrue { transformedCU.hasValidParents() }
        assertNull(transformedCU.invalidPositions().firstOrNull())
    }

    @Test
    fun testGenericNode() {
        val code = "set foo = 123\ndisplay 456"
        val lexer = SimpleLangLexer(CharStreams.fromString(code))
        val parser = SimpleLangParser(CommonTokenStream(lexer))
        val pt = parser.compilationUnit()

        val transformer = ParseTreeToASTTransformer()
        assertASTsAreEqual(GenericNode(), transformer.transform(pt)!!)
    }

    @Test
    fun testGenericASTTransformer() {
        val code = "set foo = 123\ndisplay 456"
        val lexer = SimpleLangLexer(CharStreams.fromString(code))
        val parser = SimpleLangParser(CommonTokenStream(lexer))
        val pt = parser.compilationUnit()

        val transformer = ASTTransformer()
        configure(transformer)

        // Compared to ParseTreeToASTTransformer, the base class ASTTransformer does not assign a parse tree node
        // to each AST node
        val cu = CU(
            statements = listOf(
                SetStatement(variable = "foo", value = 123),
                DisplayIntStatement(value = 456)
            )
        )
        val transformedCU = transformer.transform(pt)!!
        assertASTsAreEqual(cu, transformedCU, considerPosition = true)
        assertTrue { transformedCU.hasValidParents() }
    }

    private fun configure(transformer: ASTTransformer) {
        transformer.registerNodeFactory(SimpleLangParser.CompilationUnitContext::class, CU::class)
            .withChild(SimpleLangParser.CompilationUnitContext::statement, CU::statements)
        transformer.registerNodeFactory(SimpleLangParser.DisplayStmtContext::class) { ctx ->
            if (ctx.exception != null || ctx.expression().exception != null) {
                // We throw a custom error so that we can check that it's recorded in the AST
                throw IllegalStateException("Parse error")
            }
            DisplayIntStatement(value = ctx.expression().INT_LIT().text.toInt())
        }
        transformer.registerNodeFactory(SimpleLangParser.SetStmtContext::class) { ctx ->
            SetStatement(variable = ctx.ID().text, value = ctx.expression().INT_LIT().text.toInt())
        }
    }
}
