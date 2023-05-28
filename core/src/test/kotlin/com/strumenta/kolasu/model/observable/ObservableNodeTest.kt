package com.strumenta.kolasu.model.observable

import com.strumenta.kolasu.model.Node
import kotlin.test.Test
import kotlin.test.assertEquals

class MyObservableNode : Node() {
    var p1: Int = 0
        set(value) {
            notifyOfPropertyChange("p1", field, value)
            field = value
        }
}

class MyObserver : Observer<Node> {
    val observations = mutableListOf<String>()
    override fun receivePropertyChangeNotification(
        node: Node,
        propertyName: String,
        oldValue: Any?,
        newValue: Any?
    ) {
        observations.add("$propertyName: $oldValue -> $newValue")
    }

    override fun receivePropertyAddedNotification(node: Node, propertyName: String, added: Any?) {
        observations.add("$propertyName: added $added")
    }

    override fun receivePropertyRemovedNotification(node: Node, propertyName: String, removed: Any?) {
        observations.add("$propertyName: removed $removed")
    }
}

class MyObservableNodeMP : Node() {

    val p5 = ObservableList<MyObservableNodeMP>()
    init {
        p5.registerObserver(MultiplePropertyListObserver(this, "p5"))
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

    @Test
    fun observeMultipleContainmentsChanges() {
        val n1 = MyObservableNodeMP()
        val n2 = MyObservableNodeMP()
        val n3 = MyObservableNodeMP()
        val obs = MyObserver()
        n1.registerObserver(obs)

        assertEquals(null, n1.parent)
        assertEquals(null, n2.parent)
        assertEquals(null, n3.parent)
        assertEquals(listOf(), obs.observations)

        n1.p5.add(n2)
        assertEquals(null, n1.parent)
        assertEquals(n1, n2.parent)
        assertEquals(null, n3.parent)
        assertEquals(
            listOf("p5: added com.strumenta.kolasu.model.observable.MyObservableNodeMP(p5=[])"),
            obs.observations
        )

        n1.p5.add(n3)
        assertEquals(null, n1.parent)
        assertEquals(n1, n2.parent)
        assertEquals(n1, n3.parent)
        assertEquals(
            listOf(
                "p5: added com.strumenta.kolasu.model.observable.MyObservableNodeMP(p5=[])",
                "p5: added com.strumenta.kolasu.model.observable.MyObservableNodeMP(p5=[])"
            ),
            obs.observations
        )

        n1.p5.remove(n2)
        assertEquals(null, n1.parent)
        assertEquals(null, n2.parent)
        assertEquals(n1, n3.parent)
        assertEquals(
            listOf(
                "p5: added com.strumenta.kolasu.model.observable.MyObservableNodeMP(p5=[])",
                "p5: added com.strumenta.kolasu.model.observable.MyObservableNodeMP(p5=[])",
                "p5: removed com.strumenta.kolasu.model.observable.MyObservableNodeMP(p5=[])"
            ),
            obs.observations
        )

        n1.p5.remove(n2)
        assertEquals(null, n1.parent)
        assertEquals(null, n2.parent)
        assertEquals(n1, n3.parent)
        assertEquals(
            listOf(
                "p5: added com.strumenta.kolasu.model.observable.MyObservableNodeMP(p5=[])",
                "p5: added com.strumenta.kolasu.model.observable.MyObservableNodeMP(p5=[])",
                "p5: removed com.strumenta.kolasu.model.observable.MyObservableNodeMP(p5=[])"
            ),
            obs.observations
        )
    }
}
