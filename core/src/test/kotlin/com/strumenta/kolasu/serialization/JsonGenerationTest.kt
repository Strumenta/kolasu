package com.strumenta.kolasu.serialization

import com.google.gson.stream.JsonWriter
import com.strumenta.kolasu.model.*
import com.strumenta.kolasu.validation.Issue
import com.strumenta.kolasu.validation.IssueSeverity
import com.strumenta.kolasu.validation.IssueType
import com.strumenta.kolasu.validation.Result
import org.junit.Test
import java.io.StringWriter
import kotlin.test.assertEquals

data class NodeWithReference(
    override val name: String? = null,
    val reference: ReferenceByName<NodeWithReference>? = null,
    val children: MutableList<Node> = mutableListOf(),
) : Node(), PossiblyNamed

class JsonGenerationTest {

    @Test
    fun generateJsonOfResultWithIssues() {
        val result: Result<Node> = Result(
            listOf(
                Issue(IssueType.SYNTACTIC, "An error"),
                Issue(IssueType.LEXICAL, "A warning", severity = IssueSeverity.WARNING),
                Issue(IssueType.SEMANTIC, "An info", severity = IssueSeverity.INFO),
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
      "severity": "ERROR"
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
    }
  ]
}""",
            json,
        )
    }

    @Test
    fun generateJson() {
        val myRoot = MyRoot(
            mainSection = Section(
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
        val myRoot = MyRoot(
            mainSection = Section(
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
    fun duplicatePropertiesInheritedByInterface() {
        val json = JsonGenerator().generateString(NodeOverridingName("foo"))
        assertEquals(
            """{
  "#type": "com.strumenta.kolasu.model.NodeOverridingName",
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
  "#type": "com.strumenta.kolasu.model.ExtNode",
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
        val node = NodeWithReference(
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
        val node = DynamicNode(
            "com.strumenta.kolasu.test.Node",
            listOf(
                PropertyDescription(
                    "someAttr", false, Multiplicity.SINGULAR, 123, PropertyType.ATTRIBUTE
                ),
                PropertyDescription(
                    "someListAttr", false, Multiplicity.MANY, listOf("a", "b"), PropertyType.ATTRIBUTE
                ),
                PropertyDescription(
                    "someChild", true, Multiplicity.SINGULAR, BaseNode(456), PropertyType.CONTAINMENT
                ),
                PropertyDescription(
                    "someChildren", true, Multiplicity.MANY,
                    listOf(BaseNode(78), BaseNode(90)), PropertyType.CONTAINMENT
                ),
            )
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
                "#type": "com.strumenta.kolasu.model.BaseNode",
                "attr1": 456
              },
              "someChildren": [
                {
                  "#type": "com.strumenta.kolasu.model.BaseNode",
                  "attr1": 78
                },
                {
                  "#type": "com.strumenta.kolasu.model.BaseNode",
                  "attr1": 90
                }
              ]
            }
            """.trimIndent(),
            json,
        )
    }
}

data class DynamicNode(override val nodeType: String, override val properties: List<PropertyDescription>) : Node()
