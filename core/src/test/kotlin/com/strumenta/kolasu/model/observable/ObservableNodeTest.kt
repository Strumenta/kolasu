package com.strumenta.kolasu.model.observable

import kotlin.test.Test
import kotlin.test.assertEquals

class MyObservableNode : ObservableNode() {
    var p1: Int = 0
        set(value) {
            notifyOfPropertyChange("p1", field, value)
            field = value
        }
}

class MyObserver : Observer<MyObservableNode> {
    val observations = mutableListOf<String>()
    override fun receivePropertyChangeNotification(
        node: MyObservableNode,
        propertyName: String,
        oldValue: Any?,
        newValue: Any?
    ) {
        observations.add("$propertyName: $oldValue -> $newValue")
    }

}

class ObservableNodeTest {
    @Test
    fun observePropertyChange() {
        val n = MyObservableNode()
        val obs = MyObserver()
        assertEquals(listOf(), obs.observations)
        n.p1 = 1
        assertEquals(listOf(), obs.observations)
        n.registerObserver(obs)
        n.p1 = 2
        assertEquals(listOf("p1: 1 -> 2"), obs.observations)
        n.p1 = 3
        assertEquals(listOf("p1: 1 -> 2", "p1: 2 -> 3"), obs.observations)
    }
}