package com.strumenta.kolasu.generation

import com.strumenta.kolasu.model.Point
import com.strumenta.kolasu.model.Position
import com.strumenta.kolasu.validation.Issue
import com.strumenta.kolasu.validation.IssueType
import com.strumenta.kolasu.validation.Result
import kotlin.test.assertEquals
import org.junit.Test

class JsonDeserializerTest {

    @Test
    fun deserializeNodeFromJson() {
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
        val deserialized = JsonDeserializer().deserialize(MyRoot::class.java, json)
        assertEquals(myRoot, deserialized)
    }

    @Test
    fun deserializePositiveResultFromJson() {
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
        val originalResult = Result(emptyList(), myRoot)
        val json = JsonGenerator().generateString(originalResult)
        val deserialized = JsonDeserializer().deserializeResult(MyRoot::class.java, json)
        assertEquals(originalResult, deserialized)
    }

    @Test
    fun deserializeNegativeResultFromJson() {
        val originalResult = Result(listOf(Issue(IssueType.LEXICAL, "foo", Position(Point(1, 10), Point(4, 540)))), null)
        val json = JsonGenerator().generateString(originalResult)
        val deserialized = JsonDeserializer().deserializeResult(MyRoot::class.java, json)
        assertEquals(originalResult, deserialized)
    }
}
