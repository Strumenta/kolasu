package com.strumenta.kolasu.generation

import com.strumenta.kolasu.model.Node
import kotlin.test.assertEquals
import org.junit.Test

data class MyRoot(val mainSection: Section, val otherSections: List<Section>) : Node()
data class Section(val name: String, val contents: List<Content>) : Node()
data class Content(val id: Int, val annidatedContent: Content?) : Node()

class GenerationTest {

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
