package com.strumenta.kolasu.lionweb

import com.strumenta.kolasu.model.ReferenceByName
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame

class LionWebModelExporterTest {

    @Test
    fun exportSimpleModel() {
        val kLanguage = KolasuLanguage().apply {
            addClass(SimpleRoot::class)
        }
        val a1 = SimpleNodeA("A1", ReferenceByName("A1"), null)
        a1.ref.referred = a1
        val b2 = SimpleNodeB("some magic value")
        val a3 = SimpleNodeA("A3", ReferenceByName("A1", a1), b2)
        val ast = SimpleRoot(
            12345,
            mutableListOf(
                a1, b2, a3
            )
        )

        val exporter = LionWebModelExporter()
        exporter.recordLanguage(kLanguage)
        val lwAST = exporter.export(ast)

        val lwLanguage = exporter.correspondingLanguage(kLanguage)

        val simpleRoot = lwLanguage.getConceptByName("SimpleRoot")!!
        val simpleNodeA = lwLanguage.getConceptByName("SimpleNodeA")!!
        val simpleNodeB = lwLanguage.getConceptByName("SimpleNodeB")!!

        assertSame(simpleRoot, lwAST.concept)
        assertEquals(12345, lwAST.getPropertyValueByName("id"))
        assertEquals(3, lwAST.getChildrenByContainmentName("children").size)

        val child1 = lwAST.getChildrenByContainmentName("children")[0]
        assertSame(simpleNodeA, child1.concept)
        assertEquals("A1", child1.getPropertyValueByName("name"))
        val lwReference1 = child1.concept.getReferenceByName("ref")!!
        val refValue1 = child1.getReferenceValues(lwReference1)
        assertEquals(1, refValue1.size)
        assertEquals("A1", refValue1[0].resolveInfo)
        assertSame(child1, refValue1[0].referred)

        val child2 = lwAST.getChildrenByContainmentName("children")[1]
        assertSame(simpleNodeB, child2.concept)
        assertEquals("some magic value", child2.getPropertyValueByName("value"))

        val child3 = lwAST.getChildrenByContainmentName("children")[2]
        assertSame(simpleNodeA, child3.concept)
        assertEquals("A3", child3.getPropertyValueByName("name"))
        val lwReference3 = child3.concept.getReferenceByName("ref")!!
        val refValue3 = child3.getReferenceValues(lwReference3)
        assertEquals(1, refValue3.size)
        assertEquals("A1", refValue3[0].resolveInfo)
        assertSame(child1, refValue3[0].referred)
    }
}
