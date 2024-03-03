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

val KClass<FIntLiteral>.concept : Concept
    get() {
    val c = Concept("FIntLiteral")
    return c
}


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

}