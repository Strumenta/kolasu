package com.strumenta.kolasu.generation

import com.google.gson.stream.JsonWriter
import com.strumenta.kolasu.model.Node
import com.strumenta.kolasu.validation.Issue
import com.strumenta.kolasu.validation.IssueType
import com.strumenta.kolasu.validation.Result
import java.io.StringWriter
import kotlin.test.assertEquals
import org.junit.Test

class JsonGenerationTest {

    @Test
    fun generateJsonOfResultWithErrors() {
        val result: Result<Node> = Result(
            listOf(
                Issue(IssueType.SYNTACTIC, "An error"),
                Issue(IssueType.SYNTACTIC, "Another error")
            ),
            null
        )
        val json = JsonGenerator().generateString(result)
        assertEquals(
            """{
  "errors": [
    {
      "type": "SYNTACTIC",
      "message": "An error"
    },
    {
      "type": "SYNTACTIC",
      "message": "Another error"
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
  "#type": "com.strumenta.kolasu.generation.MyRoot",
  "mainSection": {
    "#type": "com.strumenta.kolasu.generation.Section",
    "contents": [
      {
        "#type": "com.strumenta.kolasu.generation.Content",
        "id": 1
      },
      {
        "#type": "com.strumenta.kolasu.generation.Content",
        "annidatedContent": {
          "#type": "com.strumenta.kolasu.generation.Content",
          "annidatedContent": {
            "#type": "com.strumenta.kolasu.generation.Content",
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
            """{"#type":"com.strumenta.kolasu.generation.MyRoot","mainSection":{"#type":"com.strumenta.kolasu.generation.Section","contents":[{"#type":"com.strumenta.kolasu.generation.Content","annidatedContent":null,"id":1},{"#type":"com.strumenta.kolasu.generation.Content","annidatedContent":{"#type":"com.strumenta.kolasu.generation.Content","annidatedContent":{"#type":"com.strumenta.kolasu.generation.Content","annidatedContent":null,"id":4},"id":3},"id":2}],"name":"Section1"},"otherSections":[]}""",
            json
        )
    }
}
