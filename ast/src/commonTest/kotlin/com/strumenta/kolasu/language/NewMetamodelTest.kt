package com.strumenta.kolasu.language

import com.strumenta.kolasu.model.MPNode
import kotlin.test.Test
import kotlin.test.assertEquals

data class LWRoot(
    val id: Int,
) : MPNode()

class NewMetamodelTest {
    @Test
    fun classSimpleName() {
        assertEquals("LWRoot", LWRoot::class.simpleName)
    }
}
