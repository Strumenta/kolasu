package com.strumenta.kolasu.symbolresolution

import com.strumenta.kolasu.model.Node
import com.strumenta.kolasu.model.PossiblyNamed
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

data class TestNode(override val name: String?) : Node(), PossiblyNamed

class ScopingTest {
    @Test
    fun testScopeWithIgnoreCase() {
        val node = TestNode(name = "TestNode")
        val scope = Scope(ignoreCase = true).apply { define(node) }
        assertEquals(node, scope.resolve(name = "TestNode"))
        assertEquals(node, scope.resolve(name = "testnode"))
        assertEquals(node, scope.resolve(name = "testNode"))
        assertEquals(node, scope.resolve(name = "Testnode"))
    }

    @Test
    fun testScopeWithoutIgnoreCase() {
        val node = TestNode(name = "TestNode")
        val scope = Scope(ignoreCase = false).apply { define(node) }
        assertEquals(node, scope.resolve(name = "TestNode"))
        assertNull(scope.resolve(name = "testnode"))
        assertNull(scope.resolve(name = "testNode"))
        assertNull(scope.resolve(name = "Testnode"))
    }
}
