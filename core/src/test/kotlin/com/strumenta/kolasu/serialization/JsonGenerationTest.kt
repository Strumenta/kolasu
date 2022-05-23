package com.strumenta.kolasu.serialization

import com.google.gson.stream.JsonWriter
import com.strumenta.kolasu.model.Node
import com.strumenta.kolasu.validation.Issue
import com.strumenta.kolasu.validation.IssueSeverity
import com.strumenta.kolasu.validation.IssueType
import com.strumenta.kolasu.validation.Result
import org.junit.Test
import java.io.StringWriter
import kotlin.test.assertEquals

class JsonGenerationTest {

    @Test
    fun generateJsonOfResultWithIssues() {
        val result: Result<Node> = Result(
            listOf(
                Issue(IssueType.SYNTACTIC, "An error"),
                Issue(IssueType.LEXICAL, "A warning", severity = IssueSeverity.WARNING),
                Issue(IssueType.SEMANTIC, "An info", severity = IssueSeverity.INFO)
            ),
            null
        )
        val json = JsonGenerator().generateString(result)
        assertEquals(
            """{
  "errors": [
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
            json
        )
    }

    @Test
    fun generateJson() {
        val myRoot = MyRoot(
            mainSection = Section(
                "Section1",
                listOf(
                    Content(1, null),
                    Content(2, Content(3, Content(4, null)))
                )
            ),
            otherSections = listOf()
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
            json
        )
    }

    @Test
    fun generateJsonWithStreaming() {
        val myRoot = MyRoot(
            mainSection = Section(
                "Section1",
                listOf(
                    Content(1, null),
                    Content(2, Content(3, Content(4, null)))
                )
            ),
            otherSections = listOf()
        )
        val writer = StringWriter()
        JsonGenerator().generateJSONWithStreaming(myRoot, JsonWriter(writer))
        val json = writer.toString()
        assertEquals(
            """{"#type":"com.strumenta.kolasu.serialization.MyRoot",
                |"mainSection":{"#type":"com.strumenta.kolasu.serialization.Section","contents":
                |[{"#type":"com.strumenta.kolasu.serialization.Content","annidatedContent":null,"id":1},
                |{"#type":"com.strumenta.kolasu.serialization.Content","annidatedContent":
                |{"#type":"com.strumenta.kolasu.serialization.Content","annidatedContent":
                |{"#type":"com.strumenta.kolasu.serialization.Content","annidatedContent":null,"id":4},"id":3},"id":2}],
                |"name":"Section1"},"otherSections":[]}""".trimMargin().replace("\n", ""),
            json
        )
    }
}
