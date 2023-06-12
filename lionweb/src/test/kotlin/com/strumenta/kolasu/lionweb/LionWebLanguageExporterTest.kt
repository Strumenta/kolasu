package com.strumenta.kolasu.lionweb

import com.strumenta.kolasu.model.Named
import com.strumenta.kolasu.model.Node
import com.strumenta.kolasu.model.ReferenceByName
import io.lionweb.lioncore.java.language.LionCoreBuiltins
import kotlin.test.Test
import kotlin.test.assertEquals

data class SimpleRoot(val id: Int, val children: MutableList<SimpleDecl>) : Node()

sealed class SimpleDecl : Node()

data class SimpleNodeA(
    override val name: String,
    val ref: ReferenceByName<SimpleNodeA>,
    val child: SimpleNodeB?
) : Named, SimpleDecl()

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
        val simpleNodeA = lwLanguage.getConceptByName("SimpleNodeA")!!
        val simpleNodeB = lwLanguage.getConceptByName("SimpleNodeB")!!

        assertEquals("SimpleRoot", simpleRoot.name)
        assertEquals(StarLasuLWLanguage.ASTNode, simpleRoot.extendedConcept)
        assertEquals(emptyList(), simpleRoot.implemented)
        assertEquals(false, simpleRoot.isAbstract)
        assertEquals(2, simpleRoot.features.size)
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
        assertEquals(StarLasuLWLanguage.ASTNode, simpleDecl.extendedConcept)
        assertEquals(emptyList(), simpleRoot.implemented)
        assertEquals(true, simpleDecl.isAbstract)

        assertEquals("SimpleNodeA", simpleNodeA.name)
        assertEquals(simpleDecl, simpleNodeA.extendedConcept)
        assertEquals(listOf(StarLasuLWLanguage.Named), simpleNodeA.implemented)
        assertEquals(false, simpleNodeA.isAbstract)
        assertEquals(2, simpleNodeA.features.size)
        assertEquals(3, simpleNodeA.allFeatures().size)

        assertEquals(true, StarLasuLWLanguage.Named.getPropertyByName("name") in simpleNodeA.allFeatures())

        val simpleNodeARef = simpleNodeA.getReferenceByName("ref")!!
        assertEquals("ref", simpleNodeARef.name)
        assertEquals(false, simpleNodeARef.isOptional)
        assertEquals(false, simpleNodeARef.isMultiple)
        assertEquals(simpleNodeA, simpleNodeARef.type)

        val simpleNodeAChild = simpleNodeA.getContainmentByName("child")!!
        assertEquals("child", simpleNodeAChild.name)
        assertEquals(true, simpleNodeAChild.isOptional)
        assertEquals(false, simpleNodeAChild.isMultiple)
        assertEquals(simpleNodeB, simpleNodeAChild.type)

        assertEquals("SimpleNodeB", simpleNodeB.name)
        assertEquals(simpleDecl, simpleNodeB.extendedConcept)
        assertEquals(false, simpleNodeB.isAbstract)
        assertEquals(1, simpleNodeB.features.size)
        assertEquals(1, simpleNodeB.allFeatures().size)

        val simpleNodeBValue = simpleNodeB.getPropertyByName("value")!!
        assertEquals("value", simpleNodeBValue.name)
        assertEquals(false, simpleNodeBValue.isOptional)
        assertEquals(LionCoreBuiltins.getString(), simpleNodeBValue.type)
    }
}
