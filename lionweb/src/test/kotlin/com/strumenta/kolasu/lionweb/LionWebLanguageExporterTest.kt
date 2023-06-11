package com.strumenta.kolasu.lionweb

import com.strumenta.kolasu.model.Named
import com.strumenta.kolasu.model.Node
import com.strumenta.kolasu.model.ReferenceByName
import io.lionweb.lioncore.java.language.LionCoreBuiltins
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
        val simpleDecl = lwLanguage.getConceptByName("SimpleDecl")!!

        // TODO verify that all of them extend ASTConcept

        assertEquals("SimpleRoot", simpleRoot.name)
        assertEquals(false, simpleRoot.isAbstract)
        assertEquals(null, simpleRoot.extendedConcept)
        assertEquals(2, simpleRoot.allFeatures().size)

        val simpleRootID = simpleRoot.getPropertyByName("id")!!
        assertEquals("id", simpleRootID.name)
        assertEquals(false, simpleRootID.isOptional)
        assertEquals(LionCoreBuiltins.getInteger(), simpleRootID.type)

        val simpleRootChildren = simpleRoot.getContainmentByName("children")!!
        assertEquals("children", simpleRootChildren.name)
        assertEquals(true, simpleRootChildren.isOptional)
        assertEquals(true, simpleRootChildren.isMultiple)
        assertEquals(simpleDecl, simpleRootChildren.type)

        assertEquals("SimpleDecl", simpleDecl.name)
        assertEquals(true, simpleDecl.isAbstract)

    }
}