package com.strumenta.kolasu.model

import org.junit.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals

class IndexingTest {

    @Test
    fun computeIdsWithDefaultWalkerAndIdProvider() {
        val a1 = A(s = "a1")
        val a2 = A(s = "a2")
        val a3 = A(s = "a3")
        val b1 = B(a = a1, manyAs = listOf(a2, a3))
        val idsMap = b1.computeIds()
        assertEquals(4, idsMap.size)
        assertContains(idsMap, a1)
        assertContains(idsMap, a2)
        assertContains(idsMap, a3)
        assertContains(idsMap, b1)
    }

    @Test
    fun computeIdsWithCustomWalkerAndDefaultIdProvider() {
        val a1 = A(s = "a1")
        val a2 = A(s = "a2")
        val a3 = A(s = "a3")
        val b1 = B(a = a1, manyAs = listOf(a2, a3))
        val idsMap = b1.computeIds(walker = Node::walkLeavesFirst)
        assertEquals(4, idsMap.size)
        assertContains(idsMap, a1)
        assertContains(idsMap, a2)
        assertContains(idsMap, a3)
        assertContains(idsMap, b1)
    }

    @Test
    fun computeIdsWithDefaultWalkerAndCustomIdProvider() {
        val a1 = A(s = "a1")
        val a2 = A(s = "a2")
        val a3 = A(s = "a3")
        val b1 = B(a = a1, manyAs = listOf(a2, a3))
        var counter = 0
        val idsMap = b1.computeIds { "${counter++}" }
        assertEquals(4, idsMap.size)
        assertEquals(idsMap[b1], "0")
        assertEquals(idsMap[a1], "1")
        assertEquals(idsMap[a2], "2")
        assertEquals(idsMap[a3], "3")
    }

    @Test
    fun computeIdsWithCustomWalkerAndIdProvider() {
        val a1 = A(s = "a1")
        val a2 = A(s = "a2")
        val a3 = A(s = "a3")
        val b1 = B(a = a1, manyAs = listOf(a2, a3))
        var counter = 0
        val ids = b1.computeIds(walker = Node::walkLeavesFirst) { "${counter++}" }
        assertEquals(4, ids.size)
        assertEquals(ids[a1], "0")
        assertEquals(ids[a2], "1")
        assertEquals(ids[a3], "2")
        assertEquals(ids[b1], "3")
        print(ids)
    }
}
