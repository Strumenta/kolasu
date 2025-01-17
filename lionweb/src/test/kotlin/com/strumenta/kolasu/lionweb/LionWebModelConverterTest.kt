package com.strumenta.kolasu.lionweb

import com.strumenta.kolasu.language.KolasuLanguage
import com.strumenta.kolasu.language.StarLasuLanguage
import com.strumenta.kolasu.language.explore
import com.strumenta.kolasu.model.ASTRoot
import com.strumenta.kolasu.model.FileSource
import com.strumenta.kolasu.model.LanguageAssociation
import com.strumenta.kolasu.model.Named
import com.strumenta.kolasu.model.Node
import com.strumenta.kolasu.model.NodeDestination
import com.strumenta.kolasu.model.NodeOrigin
import com.strumenta.kolasu.model.Point
import com.strumenta.kolasu.model.Range
import com.strumenta.kolasu.model.ReferenceValue
import com.strumenta.kolasu.model.SyntheticSource
import com.strumenta.kolasu.model.assignParents
import com.strumenta.kolasu.model.range
import com.strumenta.kolasu.model.withRange
import com.strumenta.kolasu.parsing.KolasuToken
import com.strumenta.kolasu.parsing.ParsingResult
import com.strumenta.kolasu.parsing.TokenCategory
import com.strumenta.kolasu.testing.assertASTsAreEqual
import com.strumenta.kolasu.transformation.FailingASTTransformation
import com.strumenta.kolasu.transformation.MissingASTTransformation
import com.strumenta.kolasu.transformation.dummyInstance
import com.strumenta.kolasu.validation.Issue
import com.strumenta.kolasu.validation.IssueSeverity
import com.strumenta.kolasu.validation.IssueType
import io.lionweb.lioncore.java.language.Concept
import io.lionweb.lioncore.java.model.impl.EnumerationValue
import io.lionweb.lioncore.java.serialization.SerializationProvider
import io.lionweb.lioncore.kotlin.children
import io.lionweb.lioncore.kotlin.getChildrenByContainmentName
import io.lionweb.lioncore.kotlin.getPropertyValueByName
import io.lionweb.lioncore.kotlin.getReferenceValueByName
import org.mkfl3x.jsondelta.JsonDelta
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

enum class AnEnum {
    FOO,
    BAR,
    ZUM,
}

@ASTRoot(canBeNotRoot = true)
data class NodeWithEnum(
    override val name: String,
    val e: AnEnum?,
) : Node(),
    Named

class LionWebModelConverterTest {
    val serialized = """{
  "serializationFormatVersion": "2024.1",
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
      "version": "2024.1"
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
            "key": "com_strumenta_starlasu-ASTNode-range-key"
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
      "references": [
        {
          "reference": {
            "language": "com_strumenta_starlasu",
            "version": "1",
            "key": "com_strumenta_starlasu-ASTNode-originalNode-key"
          },
          "targets": []
        },
        {
          "reference": {
            "language": "com_strumenta_starlasu",
            "version": "1",
            "key": "com_strumenta_starlasu-ASTNode-transpiledNodes-key"
          },
          "targets": []
        }
      ],
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
            "version": "2024.1",
            "key": "LionCore-builtins-INamed-name"
          },
          "value": "A1"
        },
        {
          "property": {
            "language": "com_strumenta_starlasu",
            "version": "1",
            "key": "com_strumenta_starlasu-ASTNode-range-key"
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
        },
        {
          "reference": {
            "language": "com_strumenta_starlasu",
            "version": "1",
            "key": "com_strumenta_starlasu-ASTNode-originalNode-key"
          },
          "targets": []
        },
        {
          "reference": {
            "language": "com_strumenta_starlasu",
            "version": "1",
            "key": "com_strumenta_starlasu-ASTNode-transpiledNodes-key"
          },
          "targets": []
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
            "key": "com_strumenta_starlasu-ASTNode-range-key"
          },
          "value": null
        }
      ],
      "containments": [],
      "references": [
        {
          "reference": {
            "language": "com_strumenta_starlasu",
            "version": "1",
            "key": "com_strumenta_starlasu-ASTNode-originalNode-key"
          },
          "targets": []
        },
        {
          "reference": {
            "language": "com_strumenta_starlasu",
            "version": "1",
            "key": "com_strumenta_starlasu-ASTNode-transpiledNodes-key"
          },
          "targets": []
        }
      ],
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
            "version": "2024.1",
            "key": "LionCore-builtins-INamed-name"
          },
          "value": "A3"
        },
        {
          "property": {
            "language": "com_strumenta_starlasu",
            "version": "1",
            "key": "com_strumenta_starlasu-ASTNode-range-key"
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
        },
        {
          "reference": {
            "language": "com_strumenta_starlasu",
            "version": "1",
            "key": "com_strumenta_starlasu-ASTNode-originalNode-key"
          },
          "targets": []
        },
        {
          "reference": {
            "language": "com_strumenta_starlasu",
            "version": "1",
            "key": "com_strumenta_starlasu-ASTNode-transpiledNodes-key"
          },
          "targets": []
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
            "key": "com_strumenta_starlasu-ASTNode-range-key"
          },
          "value": null
        }
      ],
      "containments": [],
      "references": [
        {
          "reference": {
            "language": "com_strumenta_starlasu",
            "version": "1",
            "key": "com_strumenta_starlasu-ASTNode-originalNode-key"
          },
          "targets": []
        },
        {
          "reference": {
            "language": "com_strumenta_starlasu",
            "version": "1",
            "key": "com_strumenta_starlasu-ASTNode-transpiledNodes-key"
          },
          "targets": []
        }
      ],
      "annotations": [],
      "parent": "synthetic_foo-bar-source_childrez_2"
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

        assertSame(simpleRoot, lwAST.classifier)
        assertEquals(12345, lwAST.getPropertyValueByName("id"))
        assertEquals(3, lwAST.getChildrenByContainmentName("childrez").size)

        val child1 = lwAST.getChildrenByContainmentName("childrez")[0]
        assertSame(simpleNodeA, child1.classifier)
        assertEquals("A1", child1.getPropertyValueByName("name"))
        val refValue1 = child1.getReferenceValueByName("ref")
        assertEquals(1, refValue1.size)
        assertEquals("A1", refValue1[0].resolveInfo)
        assertSame(child1, refValue1[0].referred)

        val child2 = lwAST.getChildrenByContainmentName("childrez")[1]
        assertSame(simpleNodeB, child2.classifier)
        assertEquals("some magic value", child2.getPropertyValueByName("value"))

        val child3 = lwAST.getChildrenByContainmentName("childrez")[2]
        assertSame(simpleNodeA, child3.classifier)
        assertEquals("A3", child3.getPropertyValueByName("name"))
        val refValue3 = child3.getReferenceValueByName("ref")
        assertEquals(1, refValue3.size)
        assertEquals("A1", refValue3[0].resolveInfo)
        assertSame(child1, refValue3[0].referred)

        val js = SerializationProvider.getStandardJsonSerialization()
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
        assertEquals(null, lwAST.getPropertyValueByName("range"))
        assertEquals(3, lwAST.children.size)
        val lwASTChild0 = lwAST.children[0]
        assertEquals(Range(Point(1, 1), Point(1, 10)), lwASTChild0.getPropertyValueByName("range"))
        val lwASTChild1 = lwAST.children[1]
        assertEquals(Range(Point(2, 1), Point(2, 10)), lwASTChild1.getPropertyValueByName("range"))
        val lwASTChild2 = lwAST.children[2]
        assertEquals(Range(Point(3, 4), Point(3, 12)), lwASTChild2.getPropertyValueByName("range"))

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
            },
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

        val jsonSerialization = SerializationProvider.getStandardJsonSerialization()
        converter.prepareSerialization(jsonSerialization)
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
            },
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
        val kl =
            KolasuLanguage("my.language").apply {
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
        val kl =
            KolasuLanguage("my.language").apply {
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
    fun nodesAreNotProducedForRangesAndPoints() {
        val kl =
            KolasuLanguage("my.language").apply {
                addClass(NodeWithEnum::class)
            }
        val mc = LionWebModelConverter()
        mc.exportLanguageToLionWeb(kl)
        val n1 =
            NodeWithEnum("foo", AnEnum.FOO)
                .withRange(Range(Point(3, 5), Point(27, 200)))
                .setSourceForTree(LionWebSource("MySource"))
        val lwNodes = mc.exportModelToLionWeb(n1)
        assertEquals(emptyList(), lwNodes.children)
    }

    @Test
    fun canSerializeRange() {
        val kl =
            KolasuLanguage("my.language").apply {
                addClass(NodeWithEnum::class)
            }
        val mc = LionWebModelConverter()
        mc.exportLanguageToLionWeb(kl)
        val n1 =
            NodeWithEnum("foo", AnEnum.FOO)
                .withRange(Range(Point(3, 5), Point(27, 200)))
                .setSourceForTree(LionWebSource("MySource"))
        val lwNode = mc.exportModelToLionWeb(n1)
        val jsonSerialization = SerializationProvider.getStandardJsonSerialization()
        mc.prepareSerialization(jsonSerialization)
        val serializationBlock = jsonSerialization.serializeNodesToSerializationBlock(lwNode)
        assertEquals(
            "L3:5-L27:200",
            serializationBlock
                .classifierInstancesByID["MySource"]!!
                .getPropertyValue("com_strumenta_starlasu-ASTNode-range-key"),
        )
    }

    @Test
    fun canDeserializeRange() {
        // This has the side effect on ensuring that the serializers and deserializers for primitive types in the
        // StarLasu Language are registered in the MetamodelRegistry
        StarLasuLWLanguage
        val kl =
            KolasuLanguage("my.language").apply {
                addClass(NodeWithEnum::class)
            }
        val mc = LionWebModelConverter()
        mc.exportLanguageToLionWeb(kl)
        val n1 =
            NodeWithEnum("foo", AnEnum.FOO)
                .withRange(Range(Point(3, 5), Point(27, 200)))
                .setSourceForTree(LionWebSource("MySource"))
        val lwNode = mc.exportModelToLionWeb(n1)
        val jsonSerialization = SerializationProvider.getStandardJsonSerialization()
        jsonSerialization.enableDynamicNodes()
        mc.prepareSerialization(jsonSerialization)
        val json = jsonSerialization.serializeNodesToJsonString(lwNode)
        val deserializeLWNode = jsonSerialization.deserializeToNodes(json).first()
        val deserializeN1 = mc.importModelFromLionWeb(deserializeLWNode) as NodeWithEnum
        assertEquals(Range(Point(3, 5), Point(27, 200)), deserializeN1.range)
    }

    @Test
    fun canSerializeAndDeserializeTheOriginalNodeLink() {
        val kl =
            KolasuLanguage("my.language").apply {
                addClass(SimpleRoot::class)
            }
        val mc = LionWebModelConverter()
        mc.exportLanguageToLionWeb(kl)

        val node1 =
            SimpleRoot(1, mutableListOf())
                .apply { range = Range(Point(10, 20), Point(30, 40)) }
                .setSourceForTree(LionWebSource("MySource1"))
        assertEquals(Range(Point(10, 20), Point(30, 40)), node1.range)
        val node2 = SimpleRoot(2, mutableListOf()).setSourceForTree(LionWebSource("MySource2"))

        mc.externalNodeResolver =
            object : NodeResolver {
                override fun resolve(nodeID: String): KNode? =
                    if (nodeID == "MySource2") {
                        node2
                    } else {
                        null
                    }
            }

        node1.origin = NodeOrigin(node2)
        assertEquals(NodeOrigin(node2), node1.origin)

        // We verify the data is correct before exporting
        assertEquals(LionWebSource("MySource1"), node1.source)
        assertEquals(NodeOrigin(node2), node1.origin)
        assertEquals(Range(Point(10, 20), Point(30, 40)), node1.range)
        val lwNode1 = mc.exportModelToLionWeb(node1)

        // We verify the exported data is correct
        val lwNode1Origins = lwNode1.getReferenceValueByName("originalNode")
        assertEquals(1, lwNode1Origins.size)
        assertEquals("MySource2", lwNode1Origins.first().referredID)

        // We verify the re-imported data is correct
        val deserializedNode1 = mc.importModelFromLionWeb(lwNode1) as SimpleRoot
        assertEquals(1, deserializedNode1.id)
        val deserializedNode1NodeOrigin = (deserializedNode1.origin as NodeOrigin).node
        assert(deserializedNode1NodeOrigin is SimpleRoot)
        assertEquals(2, (deserializedNode1NodeOrigin as SimpleRoot).id)
        assertEquals(Range(Point(10, 20), Point(30, 40)), deserializedNode1.range)
    }

    @Test
    fun canSerializeAndDeserializeTheTranspiledNodesLinkSingleCase() {
        val kl =
            KolasuLanguage("my.language").apply {
                addClass(SimpleRoot::class)
            }
        val mc = LionWebModelConverter()
        mc.exportLanguageToLionWeb(kl)

        val node1 = SimpleRoot(1, mutableListOf()).setSourceForTree(LionWebSource("MySource1"))
        val node3 =
            SimpleRoot(3, mutableListOf())
                .setSourceForTree(LionWebSource("MySource3"))
                .apply { range = Range(Point(1, 2), Point(3, 4)) }

        node3.destinations.add(NodeDestination(node1))

        mc.externalNodeResolver =
            object : NodeResolver {
                override fun resolve(nodeID: String): KNode? =
                    if (nodeID == "MySource1") {
                        node1
                    } else {
                        null
                    }
            }

        val lwNode3 = mc.exportModelToLionWeb(node3)
        val deserializedNode3 = mc.importModelFromLionWeb(lwNode3) as SimpleRoot
        assertEquals(3, deserializedNode3.id)
        assertEquals(1, deserializedNode3.destinations.size)
        val deserializedNode3Destination = (deserializedNode3.destinations.first() as NodeDestination).node
        assert(deserializedNode3Destination is SimpleRoot)
        assertEquals(1, (deserializedNode3Destination as SimpleRoot).id)
        assertEquals(Range(Point(1, 2), Point(3, 4)), deserializedNode3.range)
    }

    @Test
    fun canSerializeAndDeserializeTheTranspiledNodesLinkMultipleCase() {
        val kl =
            KolasuLanguage("my.language").apply {
                addClass(SimpleRoot::class)
            }
        val mc = LionWebModelConverter()
        mc.exportLanguageToLionWeb(kl)

        val node1 = SimpleRoot(1, mutableListOf()).setSourceForTree(LionWebSource("MySource1"))
        val node2 = SimpleRoot(2, mutableListOf()).setSourceForTree(LionWebSource("MySource2"))
        val node3 =
            SimpleRoot(3, mutableListOf())
                .apply { range = Range(Point(1, 2), Point(3, 4)) }
                .setSourceForTree(LionWebSource("MySource3"))

        node3.destinations.add(NodeDestination(node1))
        node3.destinations.add(NodeDestination(node2))

        mc.externalNodeResolver =
            object : NodeResolver {
                override fun resolve(nodeID: String): KNode? =
                    when (nodeID) {
                        "MySource1" -> {
                            node1
                        }
                        "MySource2" -> {
                            node2
                        }
                        else -> {
                            null
                        }
                    }
            }

        val lwNode3 = mc.exportModelToLionWeb(node3)
        val deserializedNode3 = mc.importModelFromLionWeb(lwNode3) as SimpleRoot
        assertEquals(3, deserializedNode3.id)
        assertEquals(2, deserializedNode3.destinations.size)
        val deserializedNode3Destination0 = (deserializedNode3.destinations[0] as NodeDestination).node
        val deserializedNode3Destination1 = (deserializedNode3.destinations[1] as NodeDestination).node
        assertEquals(1, (deserializedNode3Destination0 as SimpleRoot).id)
        assertEquals(2, (deserializedNode3Destination1 as SimpleRoot).id)
        assertEquals(Range(Point(1, 2), Point(3, 4)), deserializedNode3.range)
    }

    @Test
    fun canSerializeAndDeserializePlaceholderNodes() {
        val kl =
            KolasuLanguage("my.language").apply {
                addClass(SimpleRoot::class)
            }
        val mc = LionWebModelConverter()
        mc.exportLanguageToLionWeb(kl)

        val node1 = SimpleRoot(1, mutableListOf()).setSourceForTree(LionWebSource("MySource1"))
        val node2 = SimpleRoot(2, mutableListOf()).setSourceForTree(LionWebSource("MySource2"))
        node1.origin = MissingASTTransformation(node2)
        val lwNode1 = mc.exportModelToLionWeb(node1)
        // We verify the exported data is correct
        val lwNode1Origins = lwNode1.getReferenceValueByName("originalNode")
        assertEquals(1, lwNode1Origins.size)
        assertEquals(1, lwNode1.annotations.size)
        // We verify the re-imported data is correct
        mc.externalNodeResolver =
            object : NodeResolver {
                override fun resolve(nodeID: String): KNode? =
                    when (nodeID) {
                        "MySource1" -> {
                            node1
                        }
                        "MySource2" -> {
                            node2
                        }
                        else -> {
                            null
                        }
                    }
            }
        val deserializedNode1 = mc.importModelFromLionWeb(lwNode1) as SimpleRoot
        assertIs<MissingASTTransformation>(deserializedNode1.origin)
        assertIs<SimpleRoot>(((deserializedNode1.origin as MissingASTTransformation).origin as NodeOrigin).node)
        assertEquals(
            2,
            (((deserializedNode1.origin as MissingASTTransformation).origin as NodeOrigin).node as SimpleRoot).id,
        )
    }

    @Test
    fun exportImportIssue() {
        val i1 = Issue(IssueType.LEXICAL, "An issue")
        val i2 = Issue(IssueType.SYNTACTIC, "Another issue", IssueSeverity.WARNING)
        val i3 = Issue(IssueType.SEMANTIC, "Yet another issue", IssueSeverity.INFO, range(1, 2, 3, 4))

        val converter = LionWebModelConverter()
        val exportedI1 = converter.exportIssueToLionweb(i1)
        assertNull(exportedI1.id)
        assertEquals("LEXICAL", exportedI1.type?.enumerationLiteral?.name)
        assertEquals("An issue", exportedI1.message)
        assertEquals("ERROR", exportedI1.severity?.enumerationLiteral?.name)
        assertNull(exportedI1.range)
        val reimportedI1 = converter.importModelFromLionWeb(exportedI1) as Issue
        val reimportedI2 = converter.importModelFromLionWeb(converter.exportIssueToLionweb(i2)) as Issue
        val exportedI3 = converter.exportIssueToLionweb(i3)
        assertEquals(exportedI3.range, range(1, 2, 3, 4))
        val reimportedI3 = converter.importModelFromLionWeb(exportedI3) as Issue

        assertEquals(i1, reimportedI1)
        assertEquals(i2, reimportedI2)
        assertEquals(i3, reimportedI3)
    }

    @Test
    fun exportImportParsingResult() {
        val source = FileSource(File(""))
        val i1 = Issue(IssueType.LEXICAL, "An issue")
        val i2 = Issue(IssueType.SYNTACTIC, "Another issue", IssueSeverity.WARNING)
        val i3 = Issue(IssueType.SEMANTIC, "Yet another issue", IssueSeverity.INFO, range(1, 2, 3, 4))
        val a1 = SimpleNodeA("A1", ReferenceValue("A1"), null)
        a1.ref.referred = a1
        val b2 = SimpleNodeB("some magic value")
        val b3bis = SimpleNodeB("some other value")
        val a3 = SimpleNodeA("A3", ReferenceValue("A1", a1), b3bis)
        val root =
            SimpleRoot(
                12345,
                mutableListOf(
                    a1,
                    b2,
                    a3,
                ),
            ).withRange(Range(Point(1, 2), Point(3, 4)))
        root.source = source
        root.assignParents()
        val parsingResult = ParsingResult(listOf(i1, i2, i3), root, "bla bla", source = source)

        val kLanguage =
            StarLasuLanguage("com.strumenta.SimpleLang").apply {
                explore(SimpleRoot::class)
            }
        val converter = LionWebModelConverter()
        converter.exportLanguageToLionWeb(kLanguage)
        val exported =
            converter.exportParsingResultToLionweb(
                parsingResult,
                listOf(
                    KolasuToken(TokenCategory.STRING_LITERAL, range(1, 2, 3, 4), null),
                    KolasuToken(TokenCategory.PLAIN_TEXT, range(3, 5, 6, 7), null),
                ),
            )
        val reimported = converter.importModelFromLionWeb(exported) as ParsingResultWithTokens<*>
        assertASTsAreEqual(parsingResult.root!!, reimported.root!!)
        assertEquals(3, parsingResult.issues.size)
        assertEquals("bla bla", parsingResult.code)
        assertEquals(
            listOf(
                TokenCategory.STRING_LITERAL.type,
                TokenCategory.PLAIN_TEXT.type,
            ),
            reimported.tokens.map { it.category.type },
        )
        assertEquals(
            listOf(
                range(1, 2, 3, 4),
                range(3, 5, 6, 7),
            ),
            reimported.tokens.map { it.range },
        )
    }

    @Test
    fun serializeAndDeserializeNodeWithMissingTransformation() {
        val myOriginalSource = LionWebSource("Some_dummy_original_source")
        val myMigratedSource = LionWebSource("Some_dummy_migrated_source")
        val myOriginalNode = NodeWithEnum::class.dummyInstance()
        val myTransformedNode = NodeWithEnum::class.dummyInstance()
        myOriginalNode.source = myOriginalSource
        myTransformedNode.source = myMigratedSource
        myTransformedNode.origin = MissingASTTransformation(myOriginalNode)

        val converter = LionWebModelConverter()
        converter.exportLanguageToLionWeb(
            KolasuLanguage("myLanguage").apply {
                addClass(NodeWithEnum::class)
            },
        )
        converter.exportModelToLionWeb(myOriginalNode)
        val lwNode = converter.exportModelToLionWeb(myTransformedNode)
        val reimportedNode = converter.importModelFromLionWeb(lwNode) as Node
        assert(reimportedNode.origin is MissingASTTransformation)
    }

    @Test
    fun serializeAndDeserializeNodeWithFailingTransformation() {
        val myOriginalSource = LionWebSource("Some_dummy_original_source")
        val myMigratedSource = LionWebSource("Some_dummy_migrated_source")
        val myOriginalNode = NodeWithEnum::class.dummyInstance()
        val myTransformedNode = NodeWithEnum::class.dummyInstance()
        myOriginalNode.source = myOriginalSource
        myTransformedNode.source = myMigratedSource
        myTransformedNode.origin = FailingASTTransformation(NodeOrigin(myOriginalNode), "failing because...")

        val converter = LionWebModelConverter()
        converter.exportLanguageToLionWeb(
            KolasuLanguage("myLanguage").apply {
                addClass(NodeWithEnum::class)
            },
        )
        converter.exportModelToLionWeb(myOriginalNode)
        val lwNode = converter.exportModelToLionWeb(myTransformedNode)
        val reimportedNode = converter.importModelFromLionWeb(lwNode) as Node
        assert(reimportedNode.origin is FailingASTTransformation)
        assertEquals("failing because...", (reimportedNode.origin as FailingASTTransformation).message)
    }
}

@ASTRoot(canBeNotRoot = true)
@LanguageAssociation(StarLasuLanguageInstance::class)
data class NodeWithPropertiesNotInConstructor(
    override val name: String,
    var a: String,
) : Node(),
    Named {
    var b: Int = 0
    val c = mutableListOf<NodeWithPropertiesNotInConstructor>()
    val r = ReferenceValue<NodeWithPropertiesNotInConstructor>("")
}

@ASTRoot(canBeNotRoot = true)
@LanguageAssociation(StarLasuLanguageInstance::class)
data class NodeWithPropertiesNotInConstructorMutableProps(
    override val name: String,
    var a: String,
) : Node(),
    Named {
    var b: Int = 0
    var c = mutableListOf<NodeWithPropertiesNotInConstructorMutableProps>()
    var r = ReferenceValue<NodeWithPropertiesNotInConstructorMutableProps>("")
}
