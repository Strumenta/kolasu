package com.strumenta.kolasu.model.observable

import kotlin.test.Test
import kotlin.test.assertEquals


class MyListObserver : ListObserver<Int> {
    val observations = mutableListOf<String>()

    override fun added(e: Int) {
        observations.add("added $e")
    }

    override fun removed(e: Int) {
        observations.add("removed $e")
    }

}

class ObservableListTest {
    @Test
    fun observeAdditionAndRemoval() {
        val li = ObservableList<Int>()
        val o = MyListObserver()
        li.registerObserver(o)
        li.add(1)
        li.add(2)
        li.add(3)
        li.remove(2)
        li.remove(2)
        assertEquals(listOf("added 1", "added 2", "added 3", "removed 2"), o.observations)
    }
}