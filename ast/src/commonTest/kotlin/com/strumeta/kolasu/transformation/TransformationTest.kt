package com.strumeta.kolasu.transformation

import com.strumenta.kolasu.language.Concept
import com.strumenta.kolasu.model.MPNode
import com.strumenta.kolasu.transformation.MPASTTransformer
import kotlin.reflect.KClass
import kotlin.test.Test
import kotlin.test.assertEquals

sealed class FExpression : MPNode()

data class FIntLiteral(val value: Int) : FExpression() {
    override fun calculateConcept(): Concept {
        return FIntLiteral::class.concept
    }
}

data class FSumExpr(val left: FExpression, val right: FExpression) : FExpression()

val KClass<FIntLiteral>.concept : Concept
    get() {
    val c = Concept("FIntLiteral")
    return c
}

sealed class ParseTreeExpr
class ParseTreeSumExpr(val left: ParseTreeExpr, val right: ParseTreeExpr) : ParseTreeExpr()
class ParseTreeIntLiteral(val value: Int): ParseTreeExpr()



class TransformationTest {

    @Test
    fun transformerThatDoubleValue() {
        val original = FIntLiteral(3)
        val transformer = MPASTTransformer()
        transformer.registerNodeTransformer<FIntLiteral, FIntLiteral>(FIntLiteral::class) { a, b, c ->
            FIntLiteral(a.value * 2)
        }

        assertEquals(FIntLiteral(6), transformer.transform(original))
    }

    @Test
    fun transformerIntInIntLiteral() {
        val original = 3
        val transformer = MPASTTransformer()
        transformer.registerNodeTransformer<Int, FIntLiteral>(FIntLiteral::class) { a, b, c ->
            FIntLiteral(a * 2)
        }

        assertEquals(FIntLiteral(6), transformer.transform(original))
    }

    @Test
    fun transformerParseTreeIntoAstWithSpecificTransformes() {
        val original = ParseTreeSumExpr(ParseTreeIntLiteral(1), ParseTreeIntLiteral(2))
        val transformer = MPASTTransformer()
        TODO("register specific transformers")
//        transformer.registerNodeTransformer<Int, FIntLiteral>(FIntLiteral::class.concept) { a, b, c ->
//            FIntLiteral(a * 2)
//        }

        assertEquals(FSumExpr(FIntLiteral(1), FIntLiteral(2)), transformer.transform(original))
    }

    @Test
    fun transformerParseTreeIntoAstWithDefaultTransformer() {
        val original = ParseTreeSumExpr(ParseTreeIntLiteral(1), ParseTreeIntLiteral(2))
        val transformer = MPASTTransformer()
        TODO("register default transformer")
//        transformer.registerNodeTransformer<Int, FIntLiteral>(FIntLiteral::class.concept) { a, b, c ->
//            FIntLiteral(a * 2)
//        }

        assertEquals(FSumExpr(FIntLiteral(1), FIntLiteral(2)), transformer.transform(original))
    }

}