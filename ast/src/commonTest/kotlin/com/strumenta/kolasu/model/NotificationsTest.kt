package com.strumenta.kolasu.model

import com.strumenta.kolasu.language.Attribute
import com.strumenta.kolasu.language.stringType
import kotlin.test.Test
import kotlin.test.assertEquals

class SimpleNode(
    var foo: String,
) : MPNode()

class NotificationsTest {
    @Test
    fun attributeChange() {
        val n1 = SimpleNode("x")
        val attr = Attribute("foo", false, stringType, { TODO() })
        val notif1 = AttributeChangedNotification(n1, attr, "x", "y")
        assertEquals(n1, notif1.node)
        assertEquals(attr, notif1.attribute)
        assertEquals("x", notif1.oldValue)
        assertEquals("y", notif1.newValue)
    }
}
