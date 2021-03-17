package com.strumenta.kolasu.generation

import org.junit.Test
import kotlin.test.assertEquals

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
}