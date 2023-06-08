package com.strumenta.kolasu.model

import kotlin.test.Test
import kotlin.test.assertEquals

class HNodeA(override var name: String) : Node(), Named

data class HNodeB(val nodes: MutableList<HNodeA> = mutableListOf()) : Node()

data class HNodeC(val ref: ReferenceByName<HNodeA>) : Node()

data class HNodeD(var node: HNodeA? = null) : Node()

class HomogenousAPITest {

    @Test
    fun attributes() {
        val n1 = HNodeA("foo")
        assertEquals("foo", n1.getAttribute("name"))
        n1.setAttribute("name", "bar")
        assertEquals("bar", n1.getAttribute("name"))
        n1.name = "zum"
        assertEquals("zum", n1.getAttribute("name"))
    }

    @Test
    fun multipleContainments() {
        val n1 = HNodeA("n1")
        val n2 = HNodeA("n2")
        val n3 = HNodeA("n3")
        val c = HNodeB()
        assertEquals(emptyList(), c.getContainment<HNodeA>("nodes"))
        c.addToContainment("nodes", n1)
        assertEquals(listOf(n1), c.getContainment<HNodeA>("nodes"))
        c.nodes.add(n2)
        c.removeFromContainment("nodes", n1)
        assertEquals(listOf(n2), c.getContainment<HNodeA>("nodes"))
    }

    @Test
    fun singleContainments() {
        val n1 = HNodeA("n1")
        val n2 = HNodeA("n2")
        val n3 = HNodeA("n3")
        val c = HNodeD()
        assertEquals(emptyList(), c.getContainment<HNodeA>("node"))
        c.addToContainment("node", n1)
        assertEquals(listOf(n1), c.getContainment<HNodeA>("node"))
        c.node = n2
        assertEquals(listOf(n2), c.getContainment<HNodeA>("node"))
        c.addToContainment("node", n1)
        c.addToContainment("node", n3)
        assertEquals(listOf(n3), c.getContainment<HNodeA>("node"))
        c.removeFromContainment("node", n3)
        assertEquals(listOf(), c.getContainment<HNodeA>("node"))
    }

    @Test
    fun references() {
        val n1 = HNodeA("n1")
        val n2 = HNodeA("n2")
        val c = HNodeC(ReferenceByName("@@@"))
        assertEquals(null, c.getReference<HNodeA>("ref").referred)
        c.setReferenceReferred("ref", n1)
        assertEquals(n1, c.getReference<HNodeA>("ref").referred)
        c.ref.referred = n2
        assertEquals(n2, c.getReference<HNodeA>("ref").referred)
    }
}
