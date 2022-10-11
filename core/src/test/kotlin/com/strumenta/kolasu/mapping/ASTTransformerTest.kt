package com.strumenta.kolasu.mapping

import com.strumenta.kolasu.model.Node
import com.strumenta.kolasu.model.hasValidParents
import com.strumenta.kolasu.testing.assertASTsAreEqual
import com.strumenta.kolasu.transformation.ASTTransformer
import org.junit.Test
import kotlin.reflect.KClass
import kotlin.test.assertEquals
import kotlin.test.assertTrue

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
}
