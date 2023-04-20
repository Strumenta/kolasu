package com.strumenta.kolasu.serialization

import com.strumenta.kolasu.model.pos
import com.strumenta.kolasu.validation.Issue
import com.strumenta.kolasu.validation.IssueSeverity
import com.strumenta.kolasu.validation.IssueType
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import kotlin.test.Test
import kotlin.test.assertEquals

class JavaSerializationTest {
    @Test
    fun issueSerializable() {
        val baos = ByteArrayOutputStream()
        ObjectOutputStream(baos).use {
            it.writeObject(
                Issue.syntactic("issue", position = pos(1, 2, 3, 4)))
        }
        ObjectInputStream(ByteArrayInputStream(baos.toByteArray())).use {
            val obj = it.readObject() as Issue
            assertEquals("issue", obj.message)
            assertEquals(pos(1, 2, 3, 4), obj.position)
            assertEquals(IssueSeverity.ERROR, obj.severity)
            assertEquals(IssueType.SYNTACTIC, obj.type)
        }
    }
}
