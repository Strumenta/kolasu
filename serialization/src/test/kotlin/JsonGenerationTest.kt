package com.strumenta.kolasu.serialization

import com.google.gson.stream.JsonWriter
import com.strumenta.kolasu.ast.FeatureDescription
import com.strumenta.kolasu.ast.FeatureType
import com.strumenta.kolasu.ast.Multiplicity
import com.strumenta.kolasu.ast.NodeLike
import com.strumenta.kolasu.ast.Point
import com.strumenta.kolasu.ast.Range
import com.strumenta.kolasu.model.Named
import com.strumenta.kolasu.model.Node
import com.strumenta.kolasu.model.PossiblyNamed
import com.strumenta.kolasu.model.ReferenceByName
import com.strumenta.kolasu.model.range
import com.strumenta.kolasu.validation.Issue
import com.strumenta.kolasu.validation.IssueSeverity
import com.strumenta.kolasu.validation.IssueType
import com.strumenta.kolasu.validation.Result
import org.junit.Test
import java.io.StringWriter
import kotlin.test.assertEquals

data class NodeOverridingName(
    override var name: String,
) : Node(),
    Named

open class BaseNode(
    open var attr1: Int,
) : Node()

data class ExtNode(
    override var attr1: Int,
) : BaseNode(attr1)

data class NodeWithReference(
    override val name: String? = null,
    val reference: ReferenceByName<NodeWithReference>? = null,
    val children: MutableList<NodeLike> = mutableListOf(),
) : Node(),
    PossiblyNamed

class JsonGenerationTest {
    @Test
    fun generateJsonOfResultWithIssues() {
        val result: Result<NodeLike> =
            Result(
                listOf(
                    Issue(IssueType.SYNTACTIC, "An error", range = range(1, 2, 3, 4)),
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
  "#type": "com.strumenta.kolasu.serialization.MyRoot",
  "mainSection": {
    "#type": "com.strumenta.kolasu.serialization.Section",
    "contents": [
      {
        "#type": "com.strumenta.kolasu.serialization.Content",
        "id": 1
      },
      {
        "#type": "com.strumenta.kolasu.serialization.Content",
        "annidatedContent": {
          "#type": "com.strumenta.kolasu.serialization.Content",
          "annidatedContent": {
            "#type": "com.strumenta.kolasu.serialization.Content",
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
            """{"#type":"com.strumenta.kolasu.serialization.MyRoot",
                |"mainSection":{"#type":"com.strumenta.kolasu.serialization.Section","contents":
                |[{"#type":"com.strumenta.kolasu.serialization.Content","id":1},
                |{"#type":"com.strumenta.kolasu.serialization.Content","annidatedContent":{"#type":"com.strumenta.kolasu.serialization.Content",
                |"annidatedContent":{"#type":"com.strumenta.kolasu.serialization.Content","id":4},"id":3},"id":2}],
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
        val node = NodeWithReference(name = "nodeWithReference", reference = ReferenceByName(name = "self"))
        val writer = StringWriter()
        JsonGenerator().generateJSONWithStreaming(node, JsonWriter(writer))
        val json = writer.toString()
        assertEquals(
            """{
            |"#type":"com.strumenta.kolasu.serialization.NodeWithReference",
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
        val node = NodeWithReference(name = "nodeWithReference", reference = ReferenceByName(name = "self"))
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
            """{"#type":"com.strumenta.kolasu.serialization.NodeOverridingName","name":"foo"}
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
  "#type": "com.strumenta.kolasu.serialization.NodeOverridingName",
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
  "#type": "com.strumenta.kolasu.serialization.ExtNode",
  "attr1": 123
}""",
            json,
        )
    }

    @Test
    fun nodeWithUnresolvedReferenceByName() {
        val node = NodeWithReference(name = "nodeWithReference", reference = ReferenceByName(name = "self"))
        val json = JsonGenerator().generateString(node, withIds = node.computeIdsForReferencedNodes())
        assertEquals(
            """
            {
              "#type": "com.strumenta.kolasu.serialization.NodeWithReference",
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
                reference = ReferenceByName(name = "self"),
            ).apply { reference!!.referred = this }
        val json = JsonGenerator().generateString(node, withIds = node.computeIdsForReferencedNodes())
        assertEquals(
            """
            {
              "#type": "com.strumenta.kolasu.serialization.NodeWithReference",
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
        val node =
            DynamicNode(
                "com.strumenta.kolasu.test.Node",
                listOf(
                    FeatureDescription(
                        "someAttr",
                        false,
                        Multiplicity.SINGULAR,
                        123,
                        FeatureType.ATTRIBUTE,
                    ),
                    FeatureDescription(
                        "someListAttr",
                        false,
                        Multiplicity.MANY,
                        listOf("a", "b"),
                        FeatureType.ATTRIBUTE,
                    ),
                    FeatureDescription(
                        "someChild",
                        true,
                        Multiplicity.SINGULAR,
                        BaseNode(456),
                        FeatureType.CONTAINMENT,
                    ),
                    FeatureDescription(
                        "someChildren",
                        true,
                        Multiplicity.MANY,
                        listOf(BaseNode(78), BaseNode(90)),
                        FeatureType.CONTAINMENT,
                    ),
                ),
            )
        val json = JsonGenerator().generateString(node, withIds = node.computeIdsForReferencedNodes())
        assertEquals(
            """
            {
              "#type": "com.strumenta.kolasu.test.Node",
              "someAttr": 123,
              "someListAttr": [
                "a",
                "b"
              ],
              "someChild": {
                "#type": "com.strumenta.kolasu.serialization.BaseNode",
                "attr1": 456
              },
              "someChildren": [
                {
                  "#type": "com.strumenta.kolasu.serialization.BaseNode",
                  "attr1": 78
                },
                {
                  "#type": "com.strumenta.kolasu.serialization.BaseNode",
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
    override val nodeType: String,
    override val properties: List<FeatureDescription>,
) : Node()
