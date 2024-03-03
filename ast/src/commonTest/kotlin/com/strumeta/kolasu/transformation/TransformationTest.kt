package com.strumeta.kolasu.transformation

import com.strumenta.kolasu.model.MPNode
import com.strumenta.kolasu.transformation.MPASTTransformer
import kotlin.test.Test
import kotlin.test.assertEquals

sealed class FExpression : MPNode()

data class FIntLiteral(val value: Int) : FExpression()


class TransformationTest {

    @Test
    fun transformerThatDoubleValue() {
        val original = FIntLiteral(3)
        val transformer = MPASTTransformer()
        assertEquals(FIntLiteral(6), transformer.transform(original))
    }

}