package com.strumenta.kolasu.lionweb

import com.strumenta.kolasu.language.KolasuLanguage
import com.strumenta.kolasu.model.ReferenceByName
import com.strumenta.kolasu.model.assignParents
import com.strumenta.kolasu.testing.assertASTsAreEqual
import io.lionweb.lioncore.java.serialization.JsonSerialization
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame

class LionWebModelConverterTest {

    val serialized = """{
  "serializationFormatVersion": "2023.1",
  "languages": [
    {
      "version": "1",
      "key": "com-strumenta-SimpleLang"
    }
  ],
  "nodes": [
    {
      "id": "UNKNOWN_SOURCE_root",
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
            "UNKNOWN_SOURCE_root_childrez_0",
            "UNKNOWN_SOURCE_root_childrez_1",
            "UNKNOWN_SOURCE_root_childrez_2"
          ]
        }
      ],
      "references": [],
      "annotations": [],
      "parent": null
    },
    {
      "id": "UNKNOWN_SOURCE_root_childrez_0",
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
              "reference": "UNKNOWN_SOURCE_root_childrez_0"
            }
          ]
        }
      ],
      "annotations": [],
      "parent": "UNKNOWN_SOURCE_root"
    },
    {
      "id": "UNKNOWN_SOURCE_root_childrez_1",
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
      "containments": [],
      "references": [],
      "annotations": [],
      "parent": "UNKNOWN_SOURCE_root"
    },
    {
      "id": "UNKNOWN_SOURCE_root_childrez_2",
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
            "UNKNOWN_SOURCE_root_childrez_2_child"
          ]
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
              "reference": "UNKNOWN_SOURCE_root_childrez_0"
            }
          ]
        }
      ],
      "annotations": [],
      "parent": "UNKNOWN_SOURCE_root"
    },
    {
      "id": "UNKNOWN_SOURCE_root_childrez_2_child",
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
      "containments": [],
      "references": [],
      "annotations": [],
      "parent": "UNKNOWN_SOURCE_root_childrez_2"
    }
  ]
}"""

    @Test
    fun exportSimpleModel() {
        val kLanguage = KolasuLanguage("com.strumenta.SimpleLang").apply {
            addClass(SimpleRoot::class)
        }
        val a1 = SimpleNodeA("A1", ReferenceByName("A1"), null)
        a1.ref.referred = a1
        val b2 = SimpleNodeB("some magic value")
        val b3_1 = SimpleNodeB("some other value")
        val a3 = SimpleNodeA("A3", ReferenceByName("A1", a1), b3_1)
        val ast = SimpleRoot(
            12345,
            mutableListOf(
                a1,
                b2,
                a3
            )
        )
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
        assertJSONsAreEqual(serialized, js.serializeTreeToJsonString(lwAST))
    }

    @Test
    fun importSimpleModel() {
        val mConverter = LionWebModelConverter()
        val kLanguage = KolasuLanguage("com.strumenta.SimpleLang").apply {
            addClass(SimpleRoot::class)
        }
        mConverter.exportLanguageToLionWeb(kLanguage)
        val lwAST = mConverter.deserializeToNodes(serialized).first()
        val kAST = mConverter.importModelFromLionWeb(lwAST)

        val a1 = SimpleNodeA("A1", ReferenceByName("A1"), null)
        a1.ref.referred = a1
        val b2 = SimpleNodeB("some magic value")
        val b3_1 = SimpleNodeB("some other value")
        val a3 = SimpleNodeA("A3", ReferenceByName("A1", a1), b3_1)
        val expectedAST = SimpleRoot(
            12345,
            mutableListOf(
                a1,
                b2,
                a3
            )
        )
        expectedAST.assignParents()

        assertASTsAreEqual(expectedAST, kAST)
    }
}
