package com.strumenta.kolasu.transformation

import com.strumenta.kolasu.model.Node
import com.strumenta.kolasu.testing.assertASTsAreEqual
import org.junit.Test

enum class Operator {
    PLUS, MULT
}
sealed class Expression : Node()
data class IntLiteral(val value: Int) : Expression()
data class GenericBinaryExpression(val operator: Operator, val left: Expression, val right: Expression) : Node()
data class Mult(val left: Expression, val right: Expression) : Node()
data class Sum(val left: Expression, val right: Expression) : Node()

sealed class ALangExpression : Node()
data class ALangIntLiteral(val value: Int) : ALangExpression()
data class ALangSum(val left: ALangExpression, val right: ALangExpression) : ALangExpression()
data class ALangMult(val left: ALangExpression, val right: ALangExpression) : ALangExpression()

sealed class BLangExpression : Node()
data class BLangIntLiteral(val value: Int) : BLangExpression()
data class BLangSum(val left: BLangExpression, val right: BLangExpression) : BLangExpression()
data class BLangMult(val left: BLangExpression, val right: BLangExpression) : BLangExpression()

class MyASTTransformerTest {

    @Test
    fun translateBinaryExpression() {
        val myTransformer = ASTTransformer(allowGenericNode = false).apply {
            registerNodeFactory(GenericBinaryExpression::class) { source: GenericBinaryExpression ->
                when (source.operator) {
                    Operator.MULT -> Mult(transform(source.left) as Expression, transform(source.right) as Expression)
                    Operator.PLUS -> Sum(transform(source.left) as Expression, transform(source.right) as Expression)
                }
            }
            registerNodeFactory(IntLiteral::class) { source: IntLiteral -> source }
        }
        assertASTsAreEqual(
            Mult(IntLiteral(7), IntLiteral(8)),
            myTransformer.transform(GenericBinaryExpression(Operator.MULT, IntLiteral(7), IntLiteral(8)))!!
        )
        assertASTsAreEqual(
            Sum(IntLiteral(7), IntLiteral(8)),
            myTransformer.transform(GenericBinaryExpression(Operator.PLUS, IntLiteral(7), IntLiteral(8)))!!
        )
    }

    @Test
    fun translateAcrossLanguages() {
        val myTransformer = ASTTransformer(allowGenericNode = false).apply {
            registerNodeFactory(ALangIntLiteral::class) { source: ALangIntLiteral -> BLangIntLiteral(source.value) }
            registerNodeFactory(ALangSum::class) { source: ALangSum ->
                BLangSum(transform(source.left) as BLangExpression, transform(source.right) as BLangExpression)
            }
            registerNodeFactory(ALangMult::class) { source: ALangMult ->
                BLangMult(transform(source.left) as BLangExpression, transform(source.right) as BLangExpression)
            }
        }
        assertASTsAreEqual(
            BLangMult(
                BLangSum(
                    BLangIntLiteral(1),
                    BLangMult(BLangIntLiteral(2), BLangIntLiteral(3))
                ),
                BLangIntLiteral(4)
            ),
            myTransformer.transform(
                ALangMult(
                    ALangSum(
                        ALangIntLiteral(1),
                        ALangMult(ALangIntLiteral(2), ALangIntLiteral(3))
                    ),
                    ALangIntLiteral(4)
                )
            )!!
        )
    }
}
