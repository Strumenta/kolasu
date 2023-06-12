package com.strumenta.kolasu.lionweb

import com.strumenta.kolasu.model.Named
import com.strumenta.kolasu.model.Node
import com.strumenta.kolasu.model.ReferenceByName
import kotlin.test.Test
import kotlin.test.assertEquals

data class Root(val id: Int, val children: MutableList<NodeA>) : Node()

data class NodeA(override val name: String, val ref: ReferenceByName<NodeA>, val child: NodeB?) : Named, Node()

data class NodeB(val value: String) : Node()

class KolasuLanguageTest {

    @Test
    fun allASTClassesAreFound() {
        val kolasuLanguage = KolasuLanguage("com.strumenta.TestLanguage1")
        assertEquals(emptySet(), kolasuLanguage.astClasses.toSet())
        kolasuLanguage.addClass(Root::class)
        assertEquals(setOf(Root::class, NodeA::class, NodeB::class), kolasuLanguage.astClasses.toSet())
    }
}
