package com.strumenta.kolasu.serialization

import com.google.gson.stream.JsonWriter
import com.strumenta.kolasu.language.Concept
import com.strumenta.kolasu.language.Containment
import com.strumenta.kolasu.language.Feature
import com.strumenta.kolasu.language.Property
import com.strumenta.kolasu.language.StarLasuLanguage
import com.strumenta.kolasu.language.explore
import com.strumenta.kolasu.language.intType
import com.strumenta.kolasu.model.LanguageAssociation
import com.strumenta.kolasu.model.Multiplicity
import com.strumenta.kolasu.model.Named
import com.strumenta.kolasu.model.Node
import com.strumenta.kolasu.model.NodeLike
import com.strumenta.kolasu.model.Point
import com.strumenta.kolasu.model.PossiblyNamed
import com.strumenta.kolasu.model.Range
import com.strumenta.kolasu.model.ReferenceValue
import com.strumenta.kolasu.model.asConceptLike
import com.strumenta.kolasu.validation.Issue
import com.strumenta.kolasu.validation.IssueSeverity
import com.strumenta.kolasu.validation.IssueType
import com.strumenta.kolasu.validation.Result
import org.junit.Test
import java.io.StringWriter
import kotlin.test.assertEquals

object MyOtherLanguageForSerialization : StarLasuLanguage("MyOtherLanguageForSerialization") {
    init {
        explore(NodeOverridingName::class, BaseNode::class, ExtNode::class, NodeWithReference::class)
    }
}

@LanguageAssociation(MyOtherLanguageForSerialization::class)
data class NodeOverridingName(
    override var name: String,
) : Node(),
    Named

@LanguageAssociation(MyOtherLanguageForSerialization::class)
open class BaseNode(
    open var attr1: Int,
) : Node()

@LanguageAssociation(MyOtherLanguageForSerialization::class)
data class ExtNode(
    override var attr1: Int,
) : BaseNode(attr1)

@LanguageAssociation(MyOtherLanguageForSerialization::class)
data class NodeWithReference(
    override val name: String? = null,
    val reference: ReferenceValue<NodeWithReference>? = null,
    val children: MutableList<NodeLike> = mutableListOf(),
) : Node(),
    PossiblyNamed

class JsonGenerationTest {
    @Test
    fun generateJsonOfResultWithIssues() {
        val result: Result<NodeLike> =
            Result(
                listOf(
                    Issue(IssueType.SYNTACTIC, "An error", range = Range(1, 2, 3, 4)),
                    Issue(IssueType.LEXICAL, "A warning", severity = IssueSeverity.WARNING),
                    Issue(IssueType.SEMANTIC, "An info", severity = IssueSeverity.INFO),
                    Issue(IssueType.TRANSLATION, "Translation issue"),
                ),
                null,
            )
        val json = JsonGenerator().generateString(result)
        assertEquals(
            """{
  "issues": [
    {
      "type": "SYNTACTIC",
      "message": "An error",
      "severity": "ERROR",
      "range": {
        "description": "Range(start\u003dLine 1, Column 2, end\u003dLine 3, Column 4)",
        "start": {
          "line": 1,
          "column": 2
        },
        "end": {
          "line": 3,
          "column": 4
        }
      }
    },
    {
      "type": "LEXICAL",
      "message": "A warning",
      "severity": "WARNING"
    },
    {
      "type": "SEMANTIC",
      "message": "An info",
      "severity": "INFO"
    },
    {
      "type": "TRANSLATION",
      "message": "Translation issue",
      "severity": "ERROR"
    }
  ]
}""",
            json,
        )
    }

    @Test
    fun generateJson() {
        val myRoot =
            MyRoot(
                mainSection =
                    Section(
                        "Section1",
                        listOf(
                            Content(1, null),
                            Content(2, Content(3, Content(4, null))),
                        ),
                    ),
                otherSections = listOf(),
            )
        val json = JsonGenerator().generateString(myRoot)
        assertEquals(
            """{
  "#type": "YetAnotherLanguageForSerialization.MyRoot",
  "mainSection": {
    "#type": "YetAnotherLanguageForSerialization.Section",
    "contents": [
      {
        "#type": "YetAnotherLanguageForSerialization.Content",
        "id": 1
      },
      {
        "#type": "YetAnotherLanguageForSerialization.Content",
        "annidatedContent": {
          "#type": "YetAnotherLanguageForSerialization.Content",
          "annidatedContent": {
            "#type": "YetAnotherLanguageForSerialization.Content",
            "id": 4
          },
          "id": 3
        },
        "id": 2
      }
    ],
    "name": "Section1"
  },
  "otherSections": []
}""",
            json,
        )
    }

    @Test
    fun generateJsonWithStreaming() {
        val myRoot =
            MyRoot(
                mainSection =
                    Section(
                        "Section1",
                        listOf(
                            Content(1, null),
                            Content(2, Content(3, Content(4, null))),
                        ),
                    ),
                otherSections = listOf(),
            )
        val writer = StringWriter()
        JsonGenerator().generateJSONWithStreaming(myRoot, JsonWriter(writer))
        val json = writer.toString()
        assertEquals(
            """{"#type":"YetAnotherLanguageForSerialization.MyRoot",
                |"mainSection":{"#type":"YetAnotherLanguageForSerialization.Section","contents":
                |[{"#type":"YetAnotherLanguageForSerialization.Content","id":1},
                |{"#type":"YetAnotherLanguageForSerialization.Content","annidatedContent":{"#type":"YetAnotherLanguageForSerialization.Content",
                |"annidatedContent":{"#type":"YetAnotherLanguageForSerialization.Content","id":4},"id":3},"id":2}],
                |"name":"Section1"},"otherSections":[]}
            """.trimMargin().replace("\n", ""),
            json,
        )
    }

    @Test
    fun generateJsonWithStreamingShortNames() {
        val myRoot =
            MyRoot(
                mainSection =
                    Section(
                        "Section1",
                        listOf(
                            Content(1, null),
                            Content(2, Content(3, Content(4, null))),
                        ),
                    ),
                otherSections = listOf(),
            )
        val writer = StringWriter()
        JsonGenerator().generateJSONWithStreaming(root = myRoot, writer = JsonWriter(writer), shortClassNames = true)
        val json = writer.toString()
        assertEquals(
            """{"#type":"MyRoot",
                |"mainSection":{"#type":"Section","contents":
                |[{"#type":"Content","id":1},
                |{"#type":"Content","annidatedContent":{"#type":"Content",
                |"annidatedContent":{"#type":"Content","id":4},"id":3},"id":2}],
                |"name":"Section1"},"otherSections":[]}
            """.trimMargin().replace("\n", ""),
            json,
        )
    }

    @Test
    fun nodeWithReferenceStreaming() {
        val node = NodeWithReference(name = "nodeWithReference", reference = ReferenceValue(name = "self"))
        val writer = StringWriter()
        JsonGenerator().generateJSONWithStreaming(node, JsonWriter(writer))
        val json = writer.toString()
        assertEquals(
            """{
            |"#type":"MyOtherLanguageForSerialization.NodeWithReference",
            |"children":[],
            |"name":"nodeWithReference",
            |"reference":{
            |"name":"self"
            |}
            |}
            """.trimMargin().replace("\n", ""),
            json,
        )
    }

    @Test
    fun nodeWithReferenceStreamingShortNames() {
        val node = NodeWithReference(name = "nodeWithReference", reference = ReferenceValue(name = "self"))
        val writer = StringWriter()
        JsonGenerator().generateJSONWithStreaming(root = node, writer = JsonWriter(writer), shortClassNames = true)
        val json = writer.toString()
        assertEquals(
            """{
            |"#type":"NodeWithReference",
            |"children":[],
            |"name":"nodeWithReference",
            |"reference":{
            |"name":"self"
            |}
            |}
            """.trimMargin().replace("\n", ""),
            json,
        )
    }

    @Test
    fun duplicatePropertiesStreaming() {
        val node = NodeOverridingName("foo")
        val writer = StringWriter()
        JsonGenerator().generateJSONWithStreaming(root = node, writer = JsonWriter(writer))
        val json = writer.toString()
        assertEquals(
            """{"#type":"MyOtherLanguageForSerialization.NodeOverridingName","name":"foo"}
            """.trimMargin().replace("\n", ""),
            json,
        )
    }

    @Test
    fun duplicatePropertiesStreamingShortNames() {
        val node = NodeOverridingName("foo")
        val writer = StringWriter()
        JsonGenerator().generateJSONWithStreaming(root = node, writer = JsonWriter(writer), shortClassNames = true)
        val json = writer.toString()
        assertEquals(
            """{"#type":"NodeOverridingName","name":"foo"}
            """.trimMargin().replace("\n", ""),
            json,
        )
    }

    @Test
    fun streamingANullRoot() {
        val originalResult: Result<MyRoot> =
            Result(
                listOf(
                    Issue(
                        IssueType.LEXICAL,
                        "foo",
                        range = Range(Point(1, 10), Point(4, 540)),
                    ),
                ),
                null,
            )
        val writer = StringWriter()
        JsonGenerator().generateJSONWithStreaming(
            result = originalResult,
            writer = JsonWriter(writer),
            shortClassNames = true,
        )
        val json = writer.toString()
        assertEquals(
            """{"issues":[{"type":"LEXICAL","message":"foo","range":{"description":"Range(start=Line 1, Column 10, end=Line 4, Column 540)","start":{"line":1,"column":10},"end":{"line":4,"column":540}},"severity":"ERROR"}]}
            """.trimMargin()
                .replace("\n", ""),
            json,
        )
    }

    @Test
    fun duplicatePropertiesInheritedByInterface() {
        val json = JsonGenerator().generateString(NodeOverridingName("foo"))
        assertEquals(
            """{
  "#type": "MyOtherLanguageForSerialization.NodeOverridingName",
  "name": "foo"
}""",
            json,
        )
    }

    @Test
    fun duplicatePropertiesInheritedByClass() {
        val json = JsonGenerator().generateString(ExtNode(123))
        assertEquals(
            """{
  "#type": "MyOtherLanguageForSerialization.ExtNode",
  "attr1": 123
}""",
            json,
        )
    }

    @Test
    fun nodeWithUnresolvedReferenceByName() {
        val node = NodeWithReference(name = "nodeWithReference", reference = ReferenceValue(name = "self"))
        val json = JsonGenerator().generateString(node, withIds = node.computeIdsForReferencedNodes())
        assertEquals(
            """
            {
              "#type": "MyOtherLanguageForSerialization.NodeWithReference",
              "children": [],
              "name": "nodeWithReference",
              "reference": {
                "name": "self"
              }
            }
            """.trimIndent(),
            json,
        )
    }

    @Test
    fun nodeWithResolvedReferencedByName() {
        val node =
            NodeWithReference(
                name = "nodeWithReference",
                reference = ReferenceValue(name = "self"),
            ).apply { reference!!.referred = this }
        val json = JsonGenerator().generateString(node, withIds = node.computeIdsForReferencedNodes())
        assertEquals(
            """
            {
              "#type": "MyOtherLanguageForSerialization.NodeWithReference",
              "#id": "0",
              "children": [],
              "name": "nodeWithReference",
              "reference": {
                "name": "self",
                "referred": "0"
              }
            }
            """.trimIndent(),
            json,
        )
    }

    @Test
    fun dynamicNode() {
        val myLanguage = StarLasuLanguage("MyLanguage")
        val myConcept = Concept(myLanguage, "MyNode")
        myConcept.declaredFeatures.add(
            Property(
                "someAttr",
                false,
                intType,
                { (it as DynamicNode).values[it.concept.feature("someAttr")] },
                false,
            ),
        )
        myConcept.declaredFeatures.add(
            Containment(
                "someChild",
                Multiplicity.SINGULAR,
                BaseNode::class.asConceptLike(),
                {
                    (it as DynamicNode).values[it.concept.feature("someChild")]
                },
                false,
            ),
        )
        myConcept.declaredFeatures.add(
            Containment(
                "someChildren",
                Multiplicity.MANY,
                BaseNode::class.asConceptLike(),
                {
                    (it as DynamicNode).values[it.concept.feature("someChildren")]
                },
                false,
            ),
        )
        myLanguage.ensureIsRegistered()
        val node =
            DynamicNode(
                myConcept,
                mutableMapOf(
                    myConcept.requireProperty("someAttr") to 123,
                    myConcept.requireContainment("someChild") to BaseNode(456),
                    myConcept.requireContainment("someChildren") to listOf(BaseNode(78), BaseNode(90)),
                ),
            )
        val json = JsonGenerator().generateString(node, withIds = node.computeIdsForReferencedNodes())
        assertEquals(
            """
            {
              "#type": "MyLanguage.MyNode",
              "someAttr": 123,
              "someChild": {
                "#type": "MyOtherLanguageForSerialization.BaseNode",
                "attr1": 456
              },
              "someChildren": [
                {
                  "#type": "MyOtherLanguageForSerialization.BaseNode",
                  "attr1": 78
                },
                {
                  "#type": "MyOtherLanguageForSerialization.BaseNode",
                  "attr1": 90
                }
              ]
            }
            """.trimIndent(),
            json,
        )
    }
}

data class DynamicNode(
    override val concept: Concept,
    val values: MutableMap<Feature, Any?>,
) : Node()
