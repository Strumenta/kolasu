package com.strumenta.kolasu.generation

import com.strumenta.kolasu.model.Node
import com.strumenta.kolasu.validation.Error
import com.strumenta.kolasu.validation.ErrorType
import kotlin.test.assertEquals
import org.junit.Test

class JsonGenerationTest {


    @Test
    fun generateJsonOfResultWithErrors() {
        val result : com.strumenta.kolasu.Result<Node> = com.strumenta.kolasu.Result(
                listOf(com.strumenta.kolasu.validation.Error(ErrorType.SYNTACTIC, "An error"),
                        com.strumenta.kolasu.validation.Error(ErrorType.SYNTACTIC, "Another error")),
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
}""", json)
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
  "type": "MyRoot",
  "mainSection": {
    "type": "Section",
    "contents": [
      {
        "type": "Content",
        "id": 1
      },
      {
        "type": "Content",
        "annidatedContent": {
          "type": "Content",
          "annidatedContent": {
            "type": "Content",
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
}
