package com.strumenta.kolasu.lionweb

import com.strumenta.kolasu.language.KolasuLanguage
import com.strumenta.kolasu.model.Named
import com.strumenta.kolasu.model.Node
import com.strumenta.kolasu.model.NodeType
import com.strumenta.kolasu.model.ReferenceByName
import io.lionweb.utils.LanguageValidator
import kotlin.test.Test
import kotlin.test.assertEquals

data class Root(val _id: Int, val childrez: MutableList<NodeA>) : Node()

data class NodeA(override val name: String, val ref: ReferenceByName<NodeA>, val child: NodeB?) : Named, Node()

data class NodeB(val value: String) : Node(), FooMyRelevantInterface, FooMyIrrelevantInterface

interface FooMyIrrelevantInterface

@NodeType
interface FooMyRelevantInterface

class KolasuLanguageTest {
    @Test
    fun allASTClassesAreFound() {
        val kolasuLanguage = KolasuLanguage("com.strumenta.TestLanguage1")
        assertEquals(emptySet(), kolasuLanguage.astClasses.toSet())
        kolasuLanguage.addClass(Root::class)
        assertEquals(
            setOf(Root::class, NodeA::class, NodeB::class, FooMyRelevantInterface::class),
            kolasuLanguage.astClasses.toSet(),
        )
    }

    @Test
    fun validateStarLasuLanguage() {
        LanguageValidator.ensureIsValid(StarLasuLWLanguage)
    }
}
