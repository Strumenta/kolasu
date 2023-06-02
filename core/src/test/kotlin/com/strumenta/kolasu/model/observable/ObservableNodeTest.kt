package com.strumenta.kolasu.model.observable

import com.strumenta.kolasu.model.Named
import com.strumenta.kolasu.model.Node
import com.strumenta.kolasu.model.ReferenceByName
import kotlin.test.Test
import kotlin.test.assertEquals

class MyObservableNode : Node() {
    var p1: Int = 0
        set(value) {
            notifyOfPropertyChange("p1", field, value)
            field = value
        }
}

class MyObserver : SimpleNodeObserver() {
    val observations = mutableListOf<String>()
    override fun <V> onAttributeChange(node: Node, attributeName: String, oldValue: V, newValue: V) {
        observations.add("$attributeName: $oldValue -> $newValue")
    }

    override fun onChildAdded(node: Node, containmentName: String, added: Node) {
        observations.add("$containmentName: added $added")
    }

    override fun onChildRemoved(node: Node, containmentName: String, removed: Node) {
        observations.add("$containmentName: removed $removed")
    }

    override fun onReferenceSet(node: Node, referenceName: String, oldReferredNode: Node?, newReferredNode: Node?) {
        val oldName = if (oldReferredNode == null) "null" else (oldReferredNode as? Named)?.name ?: "<UNKNOWN>"
        val newName = if (newReferredNode == null) "null" else (newReferredNode as? Named)?.name ?: "<UNKNOWN>"
        observations.add("$referenceName: changed from $oldName to $newName")
    }

    override fun onReferringAdded(node: Node, referenceName: String, referring: Node) {
        val myName = (node as? Named)?.name ?: "<UNKNOWN>"
        observations.add("$myName is now referred to by $referring.$referenceName")
    }

    override fun onReferringRemoved(node: Node, referenceName: String, referring: Node) {
        val myName = (node as? Named)?.name ?: "<UNKNOWN>"
        observations.add("$myName is not referred anymore by $referring.$referenceName")
    }
}

class MyObservableNodeMP : Node() {

    val p5 = ObservableList<MyObservableNodeMP>()
    init {
        p5.subscribe(MultiplePropertyListObserver(this, "p5"))
    }
}

data class NamedNode(override val name: String) : Node(), Named

data class NodeWithReference(val ref: ReferenceByName<NamedNode>, val id: Int) : Node() {
    init {
        ref.subscribe(ReferenceToNodeObserver(this, "ref"))
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
        n.subscribe(obs)
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
        n1.subscribe(obs)

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

    @Test
    fun observeReferences() {
        val obs1 = MyObserver()
        val obs2 = MyObserver()
        val obsA = MyObserver()
        val obsB = MyObserver()

        fun clearObservations() {
            obs1.observations.clear()
            obs2.observations.clear()
            obsA.observations.clear()
            obsB.observations.clear()
        }

        val nwr1 = NodeWithReference(ReferenceByName("foo"), 1)
        val nwr2 = NodeWithReference(ReferenceByName("bar"), 2)
        val a = NamedNode("a")
        val b = NamedNode("b")

        nwr1.subscribe(obs1)
        nwr2.subscribe(obs2)
        a.subscribe(obsA)
        b.subscribe(obsB)

        nwr1.ref.referred = a
        assertEquals(listOf("ref: changed from null to a"), obs1.observations)
        assertEquals(listOf(), obs2.observations)
        assertEquals(listOf(
            "a is now referred to by com.strumenta.kolasu.model.observable.NodeWithReference(id=1, ref=Ref(foo)[Unsolved]).ref"),
            obsA.observations)
        assertEquals(listOf(), obsB.observations)
        clearObservations()

        nwr1.ref.referred = b
        assertEquals(listOf("ref: changed from a to b"), obs1.observations)
        assertEquals(listOf(), obs2.observations)
        assertEquals(listOf(
            "a is not referred anymore by com.strumenta.kolasu.model.observable.NodeWithReference(id=1, ref=Ref(foo)[Solved]).ref"), obsA.observations)
        assertEquals(listOf("b is now referred to by com.strumenta.kolasu.model.observable.NodeWithReference(id=1, ref=Ref(foo)[Solved]).ref"), obsB.observations)
        clearObservations()

        nwr1.ref.referred = null
        assertEquals(listOf("ref: changed from b to null"), obs1.observations)
        assertEquals(listOf(), obs2.observations)
        assertEquals(listOf(), obsA.observations)
        assertEquals(listOf("b is not referred anymore by com.strumenta.kolasu.model.observable.NodeWithReference(id=1, ref=Ref(foo)[Solved]).ref"), obsB.observations)
        clearObservations()

        nwr2.ref.referred = a
        assertEquals(listOf(), obs1.observations)
        assertEquals(listOf("ref: changed from null to a"), obs2.observations)
        assertEquals(listOf("a is now referred to by com.strumenta.kolasu.model.observable.NodeWithReference(id=2, ref=Ref(bar)[Unsolved]).ref"), obsA.observations)
        assertEquals(listOf(), obsB.observations)
    }
}
