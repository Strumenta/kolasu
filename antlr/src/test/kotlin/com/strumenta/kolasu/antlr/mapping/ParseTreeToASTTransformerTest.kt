package com.strumenta.kolasu.antlr.mapping

import com.strumenta.kolasu.antlr.parsing.withParseTreeNode
import com.strumenta.kolasu.model.GenericErrorNode
import com.strumenta.kolasu.model.ASTNode
import com.strumenta.kolasu.model.Range
import com.strumenta.kolasu.model.hasValidParents
import com.strumenta.kolasu.model.invalidRanges
import com.strumenta.kolasu.model.lionweb.ReflectionBasedMetamodel
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

val mm by lazy {
}

object Metamodel : ReflectionBasedMetamodel(
    CU::class, SetStatement::class, DisplayIntStatement::class,
    EEntity::class, EModule::class,
    SStatement::class, SScript::class
)

data class CU(
    val specifiedRange: Range? = null,
    var statements: List<ASTNode> =
        listOf()
) : ASTNode(specifiedRange)
data class DisplayIntStatement(val specifiedRange: Range? = null, val value: Int) : ASTNode(specifiedRange)
data class SetStatement(val specifiedRange: Range? = null, var variable: String = "", val value: Int = 0) :
    ASTNode(specifiedRange)

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
        assertASTsAreEqual(cu, transformedCU, considerRange = true)
        assertTrue { transformedCU.hasValidParents() }
        assertNull(transformedCU.invalidRanges().firstOrNull())
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
                GenericErrorNode(message = "Exception java.lang.IllegalStateException: Parse error")
                    .withParseTreeNode(pt.statement(0)),
                GenericErrorNode(message = "Exception java.lang.IllegalStateException: Parse error")
                    .withParseTreeNode(pt.statement(1))
            )
        ).withParseTreeNode(pt)
        val transformedCU = transformer.transform(pt)!! as CU
        assertASTsAreEqual(cu, transformedCU, considerRange = true)
        assertTrue { transformedCU.hasValidParents() }
        assertNull(transformedCU.invalidRanges().firstOrNull())
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
        assertASTsAreEqual(cu, transformedCU, considerRange = true)
        assertTrue { transformedCU.hasValidParents() }
    }

    private fun configure(transformer: ASTTransformer) {
        transformer.registerNodeTransformer(SimpleLangParser.CompilationUnitContext::class, CU::class)
            .withChild(CU::statements, SimpleLangParser.CompilationUnitContext::statement)
        transformer.registerNodeTransformer(SimpleLangParser.DisplayStmtContext::class) { ctx ->
            if (ctx.exception != null || ctx.expression().exception != null) {
                // We throw a custom error so that we can check that it's recorded in the AST
                throw IllegalStateException("Parse error")
            }
            DisplayIntStatement(value = ctx.expression().INT_LIT().text.toInt())
        }
        transformer.registerNodeTransformer(SimpleLangParser.SetStmtContext::class) { ctx ->
            if (ctx.exception != null || ctx.expression().exception != null) {
                // We throw a custom error so that we can check that it's recorded in the AST
                throw IllegalStateException("Parse error")
            }
            SetStatement(variable = ctx.ID().text, value = ctx.expression().INT_LIT().text.toInt())
        }
    }
}
