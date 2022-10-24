package com.strumenta.kolasu.transformation

import com.strumenta.kolasu.model.Node
import com.strumenta.kolasu.model.Position
import com.strumenta.kolasu.model.hasValidParents
import com.strumenta.kolasu.testing.assertASTsAreEqual
import org.junit.Test
import kotlin.reflect.KClass
import kotlin.test.assertEquals
import kotlin.test.assertTrue

data class CU(val specifiedPosition: Position? = null, var statements: List<Node> = listOf()) : Node(specifiedPosition)
data class DisplayIntStatement(val specifiedPosition: Position? = null, val value: Int) : Node(specifiedPosition)
data class SetStatement(val specifiedPosition: Position? = null, var variable: String = "", val value: Int = 0) :
    Node(specifiedPosition)


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


class ASTTransformerTest {

    @Test
    fun testIdentitiyTransformer() {
        val transformer = ASTTransformer()
        transformer.registerNodeFactory(CU::class, CU::class)
            .withChild(CU::statements, CU::statements)
        registerIdentityTransformation(transformer, DisplayIntStatement::class)
        registerIdentityTransformation(transformer, SetStatement::class)

        val cu = CU(
            statements = listOf(
                SetStatement(variable = "foo", value = 123),
                DisplayIntStatement(value = 456)
            )
        )
        val transformedCU = transformer.transform(cu)!!
        assertASTsAreEqual(cu, transformedCU, considerPosition = true)
        assertTrue { transformedCU.hasValidParents() }
        assertEquals(transformedCU.origin, cu)
    }

    fun <T : Node> registerIdentityTransformation(transformer: ASTTransformer, nodeClass: KClass<T>) =
        transformer.registerNodeFactory(nodeClass) { node -> node }

    /**
     * Example of transformation to perform a refactoring within the same language.
     */
    @Test
    fun translateBinaryExpression() {
        val myTransformer = ASTTransformer(allowGenericNode = false).apply {
            registerNodeFactory(GenericBinaryExpression::class) { source: GenericBinaryExpression ->
                when (source.operator) {
                    Operator.MULT -> Mult(transform(source.left) as Expression, transform(source.right) as Expression)
                    Operator.PLUS -> Sum(transform(source.left) as Expression, transform(source.right) as Expression)
                }
            }
            // This may benefit of specific support: for example a NodeFactory that returns the same element
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

    /**
     * Example of transformation to perform a translation to another language.
     */
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
