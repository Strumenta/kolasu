package com.strumenta.kolasu.lionweb

import com.strumenta.kolasu.language.KolasuLanguage
import com.strumenta.kolasu.model.Point
import com.strumenta.kolasu.model.Range
import com.strumenta.kolasu.model.ReferenceValue
import com.strumenta.kolasu.model.SyntheticSource
import com.strumenta.kolasu.model.assignParents
import com.strumenta.kolasu.model.withRange
import com.strumenta.kolasu.testing.assertASTsAreEqual
import io.lionweb.lioncore.java.language.Concept
import io.lionweb.lioncore.java.serialization.JsonSerialization
import org.mkfl3x.jsondelta.JsonDelta
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class LionWebModelConverterTest {
    val serialized = """{
  "serializationFormatVersion": "2023.1",
  "languages": [
    {
      "key": "com-strumenta-SimpleLang",
      "version": "1"
    },
    {
      "key": "com_strumenta_starlasu",
      "version": "1"
    },
    {
      "key": "LionCore-builtins",
      "version": "2023.1"
    }
  ],
  "nodes": [
    {
      "id": "synthetic_foo-bar-source_root",
      "classifier": {
        "language": "com-strumenta-SimpleLang",
        "version": "1",
        "key": "com-strumenta-SimpleLang_SimpleRoot"
      },
      "properties": [
        {
          "property": {
            "language": "com-strumenta-SimpleLang",
            "version": "1",
            "key": "com-strumenta-SimpleLang_SimpleRoot_id"
          },
          "value": "12345"
        }
      ],
      "containments": [
        {
          "containment": {
            "language": "com-strumenta-SimpleLang",
            "version": "1",
            "key": "com-strumenta-SimpleLang_SimpleRoot_childrez"
          },
          "children": [
            "synthetic_foo-bar-source_root_childrez",
            "synthetic_foo-bar-source_root_childrez_1",
            "synthetic_foo-bar-source_root_childrez_2"
          ]
        },
        {
          "containment": {
            "language": "com_strumenta_starlasu",
            "version": "1",
            "key": "com_strumenta_starlasu-ASTNode-range-key"
          },
          "children": []
        }
      ],
      "references": [],
      "annotations": [],
      "parent": null
    },
    {
      "id": "synthetic_foo-bar-source_root_childrez",
      "classifier": {
        "language": "com-strumenta-SimpleLang",
        "version": "1",
        "key": "com-strumenta-SimpleLang_SimpleNodeA"
      },
      "properties": [
        {
          "property": {
            "language": "LionCore-builtins",
            "version": "2023.1",
            "key": "LionCore-builtins-INamed-name"
          },
          "value": "A1"
        }
      ],
      "containments": [
        {
          "containment": {
            "language": "com-strumenta-SimpleLang",
            "version": "1",
            "key": "com-strumenta-SimpleLang_SimpleNodeA_child"
          },
          "children": []
        },
        {
          "containment": {
            "language": "com_strumenta_starlasu",
            "version": "1",
            "key": "com_strumenta_starlasu-ASTNode-range-key"
          },
          "children": []
        }
      ],
      "references": [
        {
          "reference": {
            "language": "com-strumenta-SimpleLang",
            "version": "1",
            "key": "com-strumenta-SimpleLang_SimpleNodeA_ref"
          },
          "targets": [
            {
              "resolveInfo": "A1",
              "reference": "synthetic_foo-bar-source_root_childrez"
            }
          ]
        }
      ],
      "annotations": [],
      "parent": "synthetic_foo-bar-source_root"
    },
    {
      "id": "synthetic_foo-bar-source_root_childrez_1",
      "classifier": {
        "language": "com-strumenta-SimpleLang",
        "version": "1",
        "key": "com-strumenta-SimpleLang_SimpleNodeB"
      },
      "properties": [
        {
          "property": {
            "language": "com-strumenta-SimpleLang",
            "version": "1",
            "key": "com-strumenta-SimpleLang_SimpleNodeB_value"
          },
          "value": "some magic value"
        }
      ],
      "containments": [
        {
          "containment": {
            "language": "com_strumenta_starlasu",
            "version": "1",
            "key": "com_strumenta_starlasu-ASTNode-range-key"
          },
          "children": []
        }
      ],
      "references": [],
      "annotations": [],
      "parent": "synthetic_foo-bar-source_root"
    },
    {
      "id": "synthetic_foo-bar-source_root_childrez_2",
      "classifier": {
        "language": "com-strumenta-SimpleLang",
        "version": "1",
        "key": "com-strumenta-SimpleLang_SimpleNodeA"
      },
      "properties": [
        {
          "property": {
            "language": "LionCore-builtins",
            "version": "2023.1",
            "key": "LionCore-builtins-INamed-name"
          },
          "value": "A3"
        }
      ],
      "containments": [
        {
          "containment": {
            "language": "com-strumenta-SimpleLang",
            "version": "1",
            "key": "com-strumenta-SimpleLang_SimpleNodeA_child"
          },
          "children": [
            "synthetic_foo-bar-source_root_childrez_2_child"
          ]
        },
        {
          "containment": {
            "language": "com_strumenta_starlasu",
            "version": "1",
            "key": "com_strumenta_starlasu-ASTNode-range-key"
          },
          "children": []
        }
      ],
      "references": [
        {
          "reference": {
            "language": "com-strumenta-SimpleLang",
            "version": "1",
            "key": "com-strumenta-SimpleLang_SimpleNodeA_ref"
          },
          "targets": [
            {
              "resolveInfo": "A1",
              "reference": "synthetic_foo-bar-source_root_childrez"
            }
          ]
        }
      ],
      "annotations": [],
      "parent": "synthetic_foo-bar-source_root"
    },
    {
      "id": "synthetic_foo-bar-source_root_childrez_2_child",
      "classifier": {
        "language": "com-strumenta-SimpleLang",
        "version": "1",
        "key": "com-strumenta-SimpleLang_SimpleNodeB"
      },
      "properties": [
        {
          "property": {
            "language": "com-strumenta-SimpleLang",
            "version": "1",
            "key": "com-strumenta-SimpleLang_SimpleNodeB_value"
          },
          "value": "some other value"
        }
      ],
      "containments": [
        {
          "containment": {
            "language": "com_strumenta_starlasu",
            "version": "1",
            "key": "com_strumenta_starlasu-ASTNode-range-key"
          },
          "children": []
        }
      ],
      "references": [],
      "annotations": [],
      "parent": "synthetic_foo-bar-source_root_childrez_2"
    }
  ]
}"""

    @Test
    fun exportSimpleModel() {
        val kLanguage =
            KolasuLanguage("com.strumenta.SimpleLang").apply {
                addClass(SimpleRoot::class)
            }
        val a1 = SimpleNodeA("A1", ReferenceValue("A1"), null)
        a1.ref.referred = a1
        val b2 = SimpleNodeB("some magic value")
        val b31 = SimpleNodeB("some other value")
        val a3 = SimpleNodeA("A3", ReferenceValue("A1", a1), b31)
        val ast =
            SimpleRoot(
                12345,
                mutableListOf(
                    a1,
                    b2,
                    a3,
                ),
            )
        ast.source = SyntheticSource("foo-bar-source")
        ast.assignParents()

        val exporter = LionWebModelConverter()
        exporter.exportLanguageToLionWeb(kLanguage)
        val lwAST = exporter.exportModelToLionWeb(ast)

        val lwLanguage = exporter.correspondingLanguage(kLanguage)

        val simpleRoot = lwLanguage.getConceptByName("SimpleRoot")!!
        val simpleNodeA = lwLanguage.getConceptByName("SimpleNodeA")!!
        val simpleNodeB = lwLanguage.getConceptByName("SimpleNodeB")!!

        assertSame(simpleRoot, lwAST.concept)
        assertEquals(12345, lwAST.getPropertyValueByName("id"))
        assertEquals(3, lwAST.getChildrenByContainmentName("childrez").size)

        val child1 = lwAST.getChildrenByContainmentName("childrez")[0]
        assertSame(simpleNodeA, child1.concept)
        assertEquals("A1", child1.getPropertyValueByName("name"))
        val refValue1 = child1.getReferenceValueByName("ref")
        assertEquals(1, refValue1.size)
        assertEquals("A1", refValue1[0].resolveInfo)
        assertSame(child1, refValue1[0].referred)

        val child2 = lwAST.getChildrenByContainmentName("childrez")[1]
        assertSame(simpleNodeB, child2.concept)
        assertEquals("some magic value", child2.getPropertyValueByName("value"))

        val child3 = lwAST.getChildrenByContainmentName("childrez")[2]
        assertSame(simpleNodeA, child3.concept)
        assertEquals("A3", child3.getPropertyValueByName("name"))
        val refValue3 = child3.getReferenceValueByName("ref")
        assertEquals(1, refValue3.size)
        assertEquals("A1", refValue3[0].resolveInfo)
        assertSame(child1, refValue3[0].referred)

        val js = JsonSerialization.getStandardSerialization()
        val report = JsonDelta().compare(serialized, js.serializeTreeToJsonString(lwAST))
        assertTrue(report.success, message = "Mismatches: ${report.mismatches}")
    }

    @Test
    fun importSimpleModel() {
        val mConverter = LionWebModelConverter()
        val kLanguage =
            KolasuLanguage("com.strumenta.SimpleLang").apply {
                addClass(SimpleRoot::class)
            }
        mConverter.exportLanguageToLionWeb(kLanguage)
        val lwAST = mConverter.deserializeToNodes(serialized).first()
        val kAST = mConverter.importModelFromLionWeb(lwAST) as KNode

        val a1 = SimpleNodeA("A1", ReferenceValue("A1"), null)
        a1.ref.referred = a1
        val b2 = SimpleNodeB("some magic value")
        val b31 = SimpleNodeB("some other value")
        val a3 = SimpleNodeA("A3", ReferenceValue("A1", a1), b31)
        val expectedAST =
            SimpleRoot(
                12345,
                mutableListOf(
                    a1,
                    b2,
                    a3,
                ),
            )
        expectedAST.assignParents()

        assertASTsAreEqual(expectedAST, kAST)
    }

    @Test
    fun kolasuNodesExtendsLionWebASTNode() {
        val mConverter = LionWebModelConverter()
        val kLanguage =
            KolasuLanguage("com.strumenta.SimpleLang").apply {
                addClass(SimpleRoot::class)
            }
        val lwLanguage = mConverter.exportLanguageToLionWeb(kLanguage)
        assertEquals(4, lwLanguage.elements.filterIsInstance<Concept>().size)
        lwLanguage.elements.filterIsInstance<Concept>().forEach { concept ->
            assertEquals(true, concept.allAncestors().contains(StarLasuLWLanguage.ASTNode))
        }
    }

    @Test
    fun serializeAndDeserializeRange() {
        val mConverter = LionWebModelConverter()
        val kLanguage =
            KolasuLanguage("com.strumenta.SimpleLang").apply {
                addClass(SimpleRoot::class)
            }
        mConverter.exportLanguageToLionWeb(kLanguage)

        val a1 =
            SimpleNodeA("A1", ReferenceValue("A1"), null)
                .withRange(Range(Point(1, 1), Point(1, 10)))
        a1.ref.referred = a1
        val b2 =
            SimpleNodeB("some magic value")
                .withRange(Range(Point(2, 1), Point(2, 10)))
        val b3s1 =
            SimpleNodeB("some other value")
                .withRange(Range(Point(2, 21), Point(2, 30)))
        val a3 =
            SimpleNodeA("A3", ReferenceValue("A1", a1), b3s1)
                .withRange(Range(Point(3, 4), Point(3, 12)))
        val initialAst =
            SimpleRoot(
                12345,
                mutableListOf(
                    a1,
                    b2,
                    a3,
                ),
            )
        initialAst.assignParents()
        initialAst.source = SyntheticSource("ss1")

        val lwAST = mConverter.exportModelToLionWeb(initialAst)
        assertEquals(0, lwAST.getChildrenByContainmentName("range").size)
        assertEquals(3, lwAST.children.size)
        val lwASTChild0 = lwAST.children[0]
        assertEquals(1, lwASTChild0.getChildrenByContainmentName("range").size)
        val lwASTChild1 = lwAST.children[1]
        assertEquals(1, lwASTChild1.getChildrenByContainmentName("range").size)
        val lwASTChild2 = lwAST.children[2]
        assertEquals(1, lwASTChild2.getChildrenByContainmentName("range").size)

        val deserializedAST = mConverter.importModelFromLionWeb(lwAST) as KNode

        assertASTsAreEqual(initialAst, deserializedAST, considerRange = true)
    }

    @Test
    fun exportParent() {
        val b2 = SimpleNodeB("some magic value")
        val a1 = SimpleNodeA("A1", ReferenceValue("A1"), b2)
        a1.assignParents()
        a1.source = SyntheticSource("ss1")

        // if we store b2, child of a1, we expect the parent to be set
        val converter = LionWebModelConverter()
        converter.exportLanguageToLionWeb(
            KolasuLanguage("myLanguage").apply {
                addClass(SimpleNodeA::class)
                addClass(SimpleNodeB::class)
            },
        )
        var exported = converter.exportModelToLionWeb(b2, considerParent = false)
        assertNull(exported.parent)

        exported = converter.exportModelToLionWeb(b2, considerParent = true)
        assertNotNull(exported.parent)
        assertEquals(converter.nodeIdProvider.id(a1), exported.parent.id)
    }
}
