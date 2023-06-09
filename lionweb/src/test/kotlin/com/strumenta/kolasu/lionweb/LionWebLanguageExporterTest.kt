package com.strumenta.kolasu.lionweb

import com.strumenta.kolasu.model.Named
import com.strumenta.kolasu.model.Node
import com.strumenta.kolasu.model.ReferenceByName
import kotlin.test.Test
import kotlin.test.assertEquals

data class SimpleRoot(val id: Int, val children : MutableList<SimpleDecl>) : Node()

sealed class SimpleDecl: Node()

data class SimpleNodeA(override val name: String, val ref: ReferenceByName<SimpleNodeA>, val child: SimpleNodeB?) : Named, SimpleDecl()

data class SimpleNodeB(val value: String) : SimpleDecl()

class LionWebLanguageExporterTest {

    @Test
    fun exportSimpleLanguage() {
        val kLanguage = KolasuLanguage().apply {
            addClass(SimpleRoot::class)
        }
        assertEquals(4, kLanguage.astClasses.size)
        val lwLanguage = LionWebLanguageExporter().export(kLanguage)
        assertEquals(4, lwLanguage.elements.size)

        val simpleRoot = lwLanguage.getConceptByName("SimpleRoot")!!
        assertEquals("SimpleRoot", simpleRoot.name)
        assertEquals(null, simpleRoot.extendedConcept)
        assertEquals(3, simpleRoot.allFeatures().size)

    }
}