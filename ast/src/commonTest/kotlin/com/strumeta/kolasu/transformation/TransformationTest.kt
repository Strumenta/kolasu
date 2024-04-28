package com.strumeta.kolasu.transformation

import com.strumenta.kolasu.language.Concept
import com.strumenta.kolasu.language.StarLasuLanguage
import com.strumenta.kolasu.model.MPNode
import com.strumenta.kolasu.transformation.MPASTTransformer
import com.strumenta.kolasu.transformation.translateCasted
import kotlin.reflect.KClass
import kotlin.test.Test
import kotlin.test.assertEquals

object MyLanguage : StarLasuLanguage("my.foo.language")

sealed class FExpression : MPNode()

data class FIntLiteral(
    val value: Int,
) : FExpression() {
    override fun calculateConcept(): Concept {
        return FIntLiteral::class.concept
    }
}

data class FSumExpr(
    val left: FExpression,
    val right: FExpression,
) : FExpression()

val KClass<FIntLiteral>.concept: Concept
    get() {
        val c = Concept(MyLanguage, "FIntLiteral")
        c.explicitlySetKotlinClass = FIntLiteral::class
        return c
    }

sealed class ParseTreeExpr

class ParseTreeSumExpr(
    val left: ParseTreeExpr,
    val right: ParseTreeExpr,
) : ParseTreeExpr()

class ParseTreeIntLiteral(
    val value: Int,
) : ParseTreeExpr()

class TransformationTest {
    @Test
    fun transformerThatDoubleValue() {
        val original = FIntLiteral(3)
        val transformer = MPASTTransformer()
        transformer.registerNodeTransformer<FIntLiteral, FIntLiteral>(FIntLiteral::class.concept) { a, b, c ->
            FIntLiteral(a.value * 2)
        }

        assertEquals(FIntLiteral(6), transformer.transform(original))
    }

    @Test
    fun transformerIntInIntLiteral() {
        val original = 3
        val transformer = MPASTTransformer()
        transformer.registerNodeTransformer<Int, FIntLiteral>(Int::class) { a, b, c ->
            FIntLiteral(a * 2)
        }

        assertEquals(FIntLiteral(6), transformer.transform(original))
    }

    @Test
    fun transformerParseTreeIntoAstWithSpecificTransformes() {
        val original = ParseTreeSumExpr(ParseTreeIntLiteral(1), ParseTreeIntLiteral(2))
        val transformer = MPASTTransformer()
        transformer.registerNodeTransformer<ParseTreeIntLiteral, FIntLiteral>(
            ParseTreeIntLiteral::class,
        ) { a, b, c ->
            FIntLiteral(a.value)
        }
        transformer.registerNodeTransformer<ParseTreeSumExpr, FSumExpr>(ParseTreeSumExpr::class) { a, b, c ->
            FSumExpr(b.translateCasted(a.left), b.translateCasted(a.right))
        }

        assertEquals(FSumExpr(FIntLiteral(1), FIntLiteral(2)), transformer.transform(original))
    }

    @Test
    fun transformerParseTreeIntoAstWithDefaultTransformer() {
        val original = ParseTreeSumExpr(ParseTreeIntLiteral(1), ParseTreeIntLiteral(2))
        val transformer = MPASTTransformer()

        transformer.registerDefaultNodeTransformer { source, astTransformer ->
            // Here we should use some logic that consumes the entity model
            // we do not have that, so we do something simpler
            when (source) {
                is ParseTreeSumExpr ->
                    listOf(
                        FSumExpr(
                            astTransformer.translateCasted(source.left),
                            astTransformer.translateCasted(source.right),
                        ),
                    )
                is ParseTreeIntLiteral -> listOf(FIntLiteral(source.value))
                else -> TODO()
            }
        }

        assertEquals(FSumExpr(FIntLiteral(1), FIntLiteral(2)), transformer.transform(original))
    }
}
