package com.strumenta.kolasu.lionweb

import com.strumenta.kolasu.language.KolasuLanguage
import com.strumenta.kolasu.model.ASTRoot
import com.strumenta.kolasu.model.Named
import com.strumenta.kolasu.model.Node
import com.strumenta.kolasu.model.Point
import com.strumenta.kolasu.model.Position
import com.strumenta.kolasu.model.ReferenceByName
import com.strumenta.kolasu.model.SyntheticSource
import com.strumenta.kolasu.model.assignParents
import com.strumenta.kolasu.model.withPosition
import com.strumenta.kolasu.testing.assertASTsAreEqual
import io.lionweb.lioncore.java.language.Concept
import io.lionweb.lioncore.java.model.impl.EnumerationValue
import io.lionweb.lioncore.java.serialization.JsonSerialization
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

enum class AnEnum {
    FOO,
    BAR,
    ZUM
}

@ASTRoot(canBeNotRoot = true)
data class NodeWithEnum(
    override val name: String,
    val e: AnEnum?
) : Named, Node()

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
      "id": "synthetic_foo-bar-source",
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
        },
        {
          "property": {
            "language": "com_strumenta_starlasu",
            "version": "1",
            "key": "com_strumenta_starlasu-ASTNode-position-key"
          },
          "value": null
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
            "synthetic_foo-bar-source_childrez",
            "synthetic_foo-bar-source_childrez_1",
            "synthetic_foo-bar-source_childrez_2"
          ]
        }
      ],
      "references": [],
      "annotations": [],
      "parent": null
    },
    {
      "id": "synthetic_foo-bar-source_childrez",
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
        },
        {
          "property": {
            "language": "com_strumenta_starlasu",
            "version": "1",
            "key": "com_strumenta_starlasu-ASTNode-position-key"
          },
          "value": null
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
              "reference": "synthetic_foo-bar-source_childrez"
            }
          ]
        }
      ],
      "annotations": [],
      "parent": "synthetic_foo-bar-source"
    },
    {
      "id": "synthetic_foo-bar-source_childrez_1",
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
        },
        {
          "property": {
            "language": "com_strumenta_starlasu",
            "version": "1",
            "key": "com_strumenta_starlasu-ASTNode-position-key"
          },
          "value": null
        }
      ],
      "containments": [],
      "references": [],
      "annotations": [],
      "parent": "synthetic_foo-bar-source"
    },
    {
      "id": "synthetic_foo-bar-source_childrez_2",
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
        },
        {
          "property": {
            "language": "com_strumenta_starlasu",
            "version": "1",
            "key": "com_strumenta_starlasu-ASTNode-position-key"
          },
          "value": null
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
            "synthetic_foo-bar-source_childrez_2_child"
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
              "reference": "synthetic_foo-bar-source_childrez"
            }
          ]
        }
      ],
      "annotations": [],
      "parent": "synthetic_foo-bar-source"
    },
    {
      "id": "synthetic_foo-bar-source_childrez_2_child",
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
        },
        {
          "property": {
            "language": "com_strumenta_starlasu",
            "version": "1",
            "key": "com_strumenta_starlasu-ASTNode-position-key"
          },
          "value": null
        }
      ],
      "containments": [],
      "references": [],
      "annotations": [],
      "parent": "synthetic_foo-bar-source_childrez_2"
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
        val kAST = mConverter.importModelFromLionWeb(lwAST) as KNode

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

    @Test
    fun kolasuNodesExtendsLionWebASTNode() {
        val mConverter = LionWebModelConverter()
        val kLanguage = KolasuLanguage("com.strumenta.SimpleLang").apply {
            addClass(SimpleRoot::class)
        }
        val lwLanguage = mConverter.exportLanguageToLionWeb(kLanguage)
        assertEquals(4, lwLanguage.elements.filterIsInstance<Concept>().size)
        lwLanguage.elements.filterIsInstance<Concept>().forEach { concept ->
            assertEquals(true, concept.allAncestors().contains(StarLasuLWLanguage.ASTNode))
        }
    }

    @Test
    fun serializeAndDeserializePosition() {
        val mConverter = LionWebModelConverter()
        val kLanguage = KolasuLanguage("com.strumenta.SimpleLang").apply {
            addClass(SimpleRoot::class)
        }
        mConverter.exportLanguageToLionWeb(kLanguage)

        val a1 = SimpleNodeA("A1", ReferenceByName("A1"), null)
            .withPosition(Position(Point(1, 1), Point(1, 10)))
        a1.ref.referred = a1
        val b2 = SimpleNodeB("some magic value")
            .withPosition(Position(Point(2, 1), Point(2, 10)))
        val b3_1 = SimpleNodeB("some other value")
            .withPosition(Position(Point(2, 21), Point(2, 30)))
        val a3 = SimpleNodeA("A3", ReferenceByName("A1", a1), b3_1)
            .withPosition(Position(Point(3, 4), Point(3, 12)))
        val initialAst = SimpleRoot(
            12345,
            mutableListOf(
                a1,
                b2,
                a3
            )
        )
        initialAst.assignParents()
        initialAst.source = SyntheticSource("ss1")

        val lwAST = mConverter.exportModelToLionWeb(initialAst)
        assertEquals(null, lwAST.getPropertyValueByName("position"))
        assertEquals(3, lwAST.children.size)
        val lwASTChild0 = lwAST.children[0]
        assertEquals(Position(Point(1, 1), Point(1, 10)), lwASTChild0.getPropertyValueByName("position"))
        val lwASTChild1 = lwAST.children[1]
        assertEquals(Position(Point(2, 1), Point(2, 10)), lwASTChild1.getPropertyValueByName("position"))
        val lwASTChild2 = lwAST.children[2]
        assertEquals(Position(Point(3, 4), Point(3, 12)), lwASTChild2.getPropertyValueByName("position"))

        val deserializedAST = mConverter.importModelFromLionWeb(lwAST) as KNode

        assertASTsAreEqual(initialAst, deserializedAST, considerPosition = true)
    }

    @Test
    fun exportParent() {
        val b2 = SimpleNodeB("some magic value")
        val a1 = SimpleNodeA("A1", ReferenceByName("A1"), b2)
        a1.assignParents()
        a1.source = SyntheticSource("ss1")

        // if we store b2, child of a1, we expect the parent to be set
        val converter = LionWebModelConverter()
        converter.exportLanguageToLionWeb(
            KolasuLanguage("myLanguage").apply {
                addClass(SimpleNodeA::class)
                addClass(SimpleNodeB::class)
            }
        )
        var exported = converter.exportModelToLionWeb(b2, considerParent = false)
        assertNull(exported.parent)

        exported = converter.exportModelToLionWeb(b2, considerParent = true)
        assertNotNull(exported.parent)
        assertEquals(converter.nodeIdProvider.id(a1), exported.parent.id)
    }

    @Test
    fun exportEnum() {
        val n1 = NodeWithEnum("n1", AnEnum.BAR)
        val n2 = NodeWithEnum("n2", AnEnum.FOO)
        val n3 = NodeWithEnum("n3", AnEnum.ZUM)
        val n4 = NodeWithEnum("n4", null)
        n1.source = SyntheticSource("someSource")
        n2.source = SyntheticSource("someSource")
        n3.source = SyntheticSource("someSource")
        n4.source = SyntheticSource("someSource")

        val converter = LionWebModelConverter()
        converter.exportLanguageToLionWeb(
            KolasuLanguage("myLanguage").apply {
                addClass(NodeWithEnum::class)
            }
        )
        val exportedN1 = converter.exportModelToLionWeb(n1)
        val exportedN2 = converter.exportModelToLionWeb(n2)
        val exportedN3 = converter.exportModelToLionWeb(n3)
        val exportedN4 = converter.exportModelToLionWeb(n4)

        assertTrue(exportedN1.getPropertyValueByName("e") is EnumerationValue)
        assertEquals("BAR", (exportedN1.getPropertyValueByName("e") as EnumerationValue).enumerationLiteral.name)
        assertTrue(exportedN2.getPropertyValueByName("e") is EnumerationValue)
        assertEquals("FOO", (exportedN2.getPropertyValueByName("e") as EnumerationValue).enumerationLiteral.name)
        assertTrue(exportedN3.getPropertyValueByName("e") is EnumerationValue)
        assertEquals("ZUM", (exportedN3.getPropertyValueByName("e") as EnumerationValue).enumerationLiteral.name)
        assertEquals(null, exportedN4.getPropertyValueByName("e"))

        val jsonSerialization = JsonSerialization.getStandardSerialization()
        converter.prepareJsonSerialization(jsonSerialization)
        jsonSerialization.serializeTreesToJsonString(exportedN1)
    }

    @Test
    fun importEnum() {
        val n1 = NodeWithEnum("n1", AnEnum.BAR)
        val n2 = NodeWithEnum("n2", AnEnum.FOO)
        val n3 = NodeWithEnum("n3", AnEnum.ZUM)
        val n4 = NodeWithEnum("n4", null)
        n1.source = SyntheticSource("someSource")
        n2.source = SyntheticSource("someSource")
        n3.source = SyntheticSource("someSource")
        n4.source = SyntheticSource("someSource")

        val converter = LionWebModelConverter()
        converter.exportLanguageToLionWeb(
            KolasuLanguage("myLanguage").apply {
                addClass(NodeWithEnum::class)
            }
        )

        val reimportedN1 = converter.importModelFromLionWeb(converter.exportModelToLionWeb(n1)) as NodeWithEnum
        val reimportedN2 = converter.importModelFromLionWeb(converter.exportModelToLionWeb(n2)) as NodeWithEnum
        val reimportedN3 = converter.importModelFromLionWeb(converter.exportModelToLionWeb(n3)) as NodeWithEnum
        val reimportedN4 = converter.importModelFromLionWeb(converter.exportModelToLionWeb(n4)) as NodeWithEnum

        assertASTsAreEqual(n1, reimportedN1)
        assertASTsAreEqual(n2, reimportedN2)
        assertASTsAreEqual(n3, reimportedN3)
        assertASTsAreEqual(n4, reimportedN4)
    }

    // This verifies Issue #324
    @Test
    fun whenImportingConsiderAlsoPropertiesNotInConstructor() {
        val kl = KolasuLanguage("my.language").apply {
            addClass(NodeWithPropertiesNotInConstructor::class)
        }
        val mc = LionWebModelConverter()
        mc.exportLanguageToLionWeb(kl)
        val n1 = NodeWithPropertiesNotInConstructor("N1", "foo")
        n1.b = 10
        val n2 = NodeWithPropertiesNotInConstructor("N2", "bar")
        n2.b = 11
        val n3 = NodeWithPropertiesNotInConstructor("N3", "zum")
        n3.b = 12
        n1.c.add(n2)
        n1.c.add(n3)
        n2.r.referred = n3
        n1.source = SyntheticSource("JustForTest")
        n1.assignParents()
        val lwNode = mc.exportModelToLionWeb(n1)

        val n1deserialized = mc.importModelFromLionWeb(lwNode) as NodeWithPropertiesNotInConstructor
        assertEquals("N1", n1deserialized.name)
        assertEquals("foo", n1deserialized.a)
        assertEquals(10, n1deserialized.b)
        assertEquals(listOf("N2", "N3"), n1deserialized.c.map { it.name })
        assertEquals(null, n1deserialized.r.referred)

        val n2deserialized = n1deserialized.c[0]
        val n3deserialized = n1deserialized.c[1]

        assertEquals("N2", n2deserialized.name)
        assertEquals("bar", n2deserialized.a)
        assertEquals(11, n2deserialized.b)
        assertEquals(listOf(), n2deserialized.c.map { it.name })
        assertEquals(n3deserialized, n2deserialized.r.referred)

        assertEquals("N3", n3deserialized.name)
        assertEquals("zum", n3deserialized.a)
        assertEquals(12, n3deserialized.b)
        assertEquals(listOf(), n3deserialized.c.map { it.name })
        assertEquals(null, n3deserialized.r.referred)
    }

    @Test
    fun whenImportingConsiderAlsoPropertiesNotInConstructorWhichAreMutable() {
        val kl = KolasuLanguage("my.language").apply {
            addClass(NodeWithPropertiesNotInConstructorMutableProps::class)
        }
        val mc = LionWebModelConverter()
        mc.exportLanguageToLionWeb(kl)
        val n1 = NodeWithPropertiesNotInConstructorMutableProps("N1", "foo")
        n1.b = 10
        val n2 = NodeWithPropertiesNotInConstructorMutableProps("N2", "bar")
        n2.b = 11
        val n3 = NodeWithPropertiesNotInConstructorMutableProps("N3", "zum")
        n3.b = 12
        n1.c.add(n2)
        n1.c.add(n3)
        n2.r.referred = n3
        n1.source = SyntheticSource("JustForTest")
        n1.assignParents()
        val lwNode = mc.exportModelToLionWeb(n1)

        val n1deserialized = mc.importModelFromLionWeb(lwNode) as NodeWithPropertiesNotInConstructorMutableProps
        assertEquals("N1", n1deserialized.name)
        assertEquals("foo", n1deserialized.a)
        assertEquals(10, n1deserialized.b)
        assertEquals(listOf("N2", "N3"), n1deserialized.c.map { it.name })
        assertEquals(null, n1deserialized.r.referred)

        val n2deserialized = n1deserialized.c[0]
        val n3deserialized = n1deserialized.c[1]

        assertEquals("N2", n2deserialized.name)
        assertEquals("bar", n2deserialized.a)
        assertEquals(11, n2deserialized.b)
        assertEquals(listOf(), n2deserialized.c.map { it.name })
        assertEquals(n3deserialized, n2deserialized.r.referred)

        assertEquals("N3", n3deserialized.name)
        assertEquals("zum", n3deserialized.a)
        assertEquals(12, n3deserialized.b)
        assertEquals(listOf(), n3deserialized.c.map { it.name })
        assertEquals(null, n3deserialized.r.referred)
    }

    @Test
    fun nodesAreNotProducedForPositionsAndPoints() {
        val kl = KolasuLanguage("my.language").apply {
            addClass(NodeWithEnum::class)
        }
        val mc = LionWebModelConverter()
        mc.exportLanguageToLionWeb(kl)
        val n1 = NodeWithEnum("foo", AnEnum.FOO)
            .withPosition(Position(Point(3, 5), Point(27, 200)))
            .setSourceForTree(LionWebSource("MySource"))
        val lwNodes = mc.exportModelToLionWeb(n1)
        assertEquals(emptyList(), lwNodes.children)
    }

    @Test
    fun canSerializePosition() {
        val kl = KolasuLanguage("my.language").apply {
            addClass(NodeWithEnum::class)
        }
        val mc = LionWebModelConverter()
        mc.exportLanguageToLionWeb(kl)
        val n1 = NodeWithEnum("foo", AnEnum.FOO)
            .withPosition(Position(Point(3, 5), Point(27, 200)))
            .setSourceForTree(LionWebSource("MySource"))
        val lwNode = mc.exportModelToLionWeb(n1)
        val jsonSerialization = JsonSerialization.getStandardSerialization()
        mc.prepareJsonSerialization(jsonSerialization)
        val serializationBlock = jsonSerialization.serializeNodesToSerializationBlock(lwNode)
        assertEquals(
            "L3:5 to L27:200",
            serializationBlock.classifierInstancesByID["MySource"]!!
                .getPropertyValue("com_strumenta_starlasu-ASTNode-position-key")
        )
    }

    @Test
    fun canDeserializePosition() {
        val kl = KolasuLanguage("my.language").apply {
            addClass(NodeWithEnum::class)
        }
        val mc = LionWebModelConverter()
        mc.exportLanguageToLionWeb(kl)
        val n1 = NodeWithEnum("foo", AnEnum.FOO)
            .withPosition(Position(Point(3, 5), Point(27, 200)))
            .setSourceForTree(LionWebSource("MySource"))
        val lwNode = mc.exportModelToLionWeb(n1)
        val jsonSerialization = JsonSerialization.getStandardSerialization()
        jsonSerialization.enableDynamicNodes()
        mc.prepareJsonSerialization(jsonSerialization)
        val json = jsonSerialization.serializeNodesToJsonString(lwNode)
        val deserializeLWNode = jsonSerialization.deserializeToNodes(json).first()
        val deserializeN1 = mc.importModelFromLionWeb(deserializeLWNode) as NodeWithEnum
        assertEquals(Position(Point(3, 5), Point(27, 200)), deserializeN1.position)
    }
}

@ASTRoot(canBeNotRoot = true)
data class NodeWithPropertiesNotInConstructor(override val name: String, var a: String) : Node(), Named {
    var b: Int = 0
    val c = mutableListOf<NodeWithPropertiesNotInConstructor>()
    val r = ReferenceByName<NodeWithPropertiesNotInConstructor>("")
}

@ASTRoot(canBeNotRoot = true)
data class NodeWithPropertiesNotInConstructorMutableProps(override val name: String, var a: String) : Node(), Named {
    var b: Int = 0
    var c = mutableListOf<NodeWithPropertiesNotInConstructorMutableProps>()
    var r = ReferenceByName<NodeWithPropertiesNotInConstructorMutableProps>("")
}
