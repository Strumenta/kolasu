package com.strumenta.kolasu.lionweb

import com.strumenta.kolasu.model.Named
import com.strumenta.kolasu.model.Node
import com.strumenta.kolasu.model.ReferenceByName
import kotlin.test.Test
import kotlin.test.assertEquals

data class Root(val id: Int, val children : MutableList<NodeA>) : Node()

data class NodeA(override val name: String, val ref: ReferenceByName<NodeA>, val child: NodeB?) : Named, Node()

data class NodeB(val value: String) : Node()

class KolasuLanguageTest {

    @Test
    fun allASTClassesAreFound() {
        val kolasuLanguage = KolasuLanguage()
        assertEquals(0, kolasuLanguage.astClasses.size)
        kolasuLanguage.addClass(Root::class)
        assertEquals(3, kolasuLanguage.astClasses.size)
    }
}