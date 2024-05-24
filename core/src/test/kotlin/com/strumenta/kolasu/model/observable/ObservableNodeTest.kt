package com.strumenta.kolasu.model.observable

import com.strumenta.kolasu.language.Containment
import com.strumenta.kolasu.language.Property
import com.strumenta.kolasu.language.Reference
import com.strumenta.kolasu.language.StarLasuLanguage
import com.strumenta.kolasu.language.explore
import com.strumenta.kolasu.language.intType
import com.strumenta.kolasu.model.Named
import com.strumenta.kolasu.model.Node
import com.strumenta.kolasu.model.NodeLike
import com.strumenta.kolasu.model.ReferenceValue
import kotlin.test.Test
import kotlin.test.assertEquals

object StarLasuLanguageInstance : StarLasuLanguage("com.strumenta.kolasu.model.observable") {
    init {
        explore(MyObservableNodeMP::class, NodeWithReference::class)
    }
}

class MyObservableNode : Node() {
    var p1: Int = 0
        set(value) {
            val attr = Property("p1", false, intType, { TODO() })
            notifyOfPropertyChange(attr, field, value)
            field = value
        }
}

class MyObserver(
    val description: String? = null,
) : SimpleNodeObserver() {
    val observations = mutableListOf<String>()

    override fun <V> onPropertyChange(
        node: NodeLike,
        property: Property,
        oldValue: V,
        newValue: V,
    ) {
        observations.add("${property.name}: $oldValue -> $newValue")
    }

    override fun onChildAdded(
        node: NodeLike,
        containment: Containment,
        added: NodeLike,
    ) {
        observations.add("${containment.name}: added $added")
    }

    override fun onChildRemoved(
        node: NodeLike,
        containment: Containment,
        removed: NodeLike,
    ) {
        observations.add("${containment.name}: removed $removed")
    }

    override fun onReferenceSet(
        node: NodeLike,
        reference: Reference,
        oldReferredNode: NodeLike?,
        newReferredNode: NodeLike?,
    ) {
        val oldName = if (oldReferredNode == null) "null" else (oldReferredNode as? Named)?.name ?: "<UNKNOWN>"
        val newName = if (newReferredNode == null) "null" else (newReferredNode as? Named)?.name ?: "<UNKNOWN>"
        observations.add("${reference.name}: changed from $oldName to $newName")
    }

    override fun onReferringAdded(
        node: NodeLike,
        reference: Reference,
        referring: NodeLike,
    ) {
        val myName = (node as? Named)?.name ?: "<UNKNOWN>"
        observations.add("$myName is now referred to by $referring.${reference.name}")
    }

    override fun onReferringRemoved(
        node: NodeLike,
        reference: Reference,
        referring: NodeLike,
    ) {
        val myName = (node as? Named)?.name ?: "<UNKNOWN>"
        observations.add("$myName is not referred anymore by $referring.${reference.name}")
    }
}

class MyObservableNodeMP : Node() {
    val p5 = ObservableList<MyObservableNodeMP>()

    init {
        p5.subscribe(MultiplePropertyListObserver(this, concept.requireContainment("p5")))
    }
}

data class NamedNode(
    override val name: String,
) : Node(),
    Named

data class NodeWithReference(
    val ref: ReferenceValue<NamedNode>,
    val id: Int,
) : Node() {
    init {
        ref.setContainer(this, concept.requireReference("ref"))
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
        StarLasuLanguageInstance.ensureIsRegistered()
        val n1 = MyObservableNodeMP()
        val n2 = MyObservableNodeMP()
        val n3 = MyObservableNodeMP()
        val obs = MyObserver("Observer to n1")
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
            obs.observations,
        )

        n1.p5.add(n3)
        assertEquals(null, n1.parent)
        assertEquals(n1, n2.parent)
        assertEquals(n1, n3.parent)
        assertEquals(
            listOf(
                "p5: added com.strumenta.kolasu.model.observable.MyObservableNodeMP(p5=[])",
                "p5: added com.strumenta.kolasu.model.observable.MyObservableNodeMP(p5=[])",
            ),
            obs.observations,
        )

        n1.p5.remove(n2)
        assertEquals(null, n1.parent)
        assertEquals(null, n2.parent)
        assertEquals(n1, n3.parent)
        assertEquals(
            listOf(
                "p5: added com.strumenta.kolasu.model.observable.MyObservableNodeMP(p5=[])",
                "p5: added com.strumenta.kolasu.model.observable.MyObservableNodeMP(p5=[])",
                "p5: removed com.strumenta.kolasu.model.observable.MyObservableNodeMP(p5=[])",
            ),
            obs.observations,
        )

        n1.p5.remove(n2)
        assertEquals(null, n1.parent)
        assertEquals(null, n2.parent)
        assertEquals(n1, n3.parent)
        assertEquals(
            listOf(
                "p5: added com.strumenta.kolasu.model.observable.MyObservableNodeMP(p5=[])",
                "p5: added com.strumenta.kolasu.model.observable.MyObservableNodeMP(p5=[])",
                "p5: removed com.strumenta.kolasu.model.observable.MyObservableNodeMP(p5=[])",
            ),
            obs.observations,
        )
    }

    @Test
    fun observeReferences() {
        StarLasuLanguageInstance.ensureIsRegistered()
        val obs1 = MyObserver("Observer to nwr1")
        val obs2 = MyObserver("Observer to nwr2")
        val obsA = MyObserver("Observer to a")
        val obsB = MyObserver("Observer to b")

        fun clearObservations() {
            obs1.observations.clear()
            obs2.observations.clear()
            obsA.observations.clear()
            obsB.observations.clear()
        }

        val nwr1 = NodeWithReference(ReferenceValue("foo"), 1)
        val nwr2 = NodeWithReference(ReferenceValue("bar"), 2)
        val a = NamedNode("a")
        val b = NamedNode("b")

        nwr1.subscribe(obs1)
        nwr2.subscribe(obs2)
        a.subscribe(obsA)
        b.subscribe(obsB)

        nwr1.ref.referred = a
        assertEquals(listOf("ref: changed from null to a"), obs1.observations)
        assertEquals(listOf(), obs2.observations)
        assertEquals(
            listOf(
                "a is now referred to by com.strumenta.kolasu.model.observable." +
                    "NodeWithReference(id=1, ref=Ref(foo)[Unsolved]).ref",
            ),
            obsA.observations,
        )
        assertEquals(listOf(), obsB.observations)
        clearObservations()

        nwr1.ref.referred = b
        assertEquals(listOf("ref: changed from a to b"), obs1.observations)
        assertEquals(listOf(), obs2.observations)
        assertEquals(
            listOf(
                "a is not referred anymore by com.strumenta.kolasu.model.observable." +
                    "NodeWithReference(id=1, ref=Ref(foo)[Solved]).ref",
            ),
            obsA.observations,
        )
        assertEquals(
            listOf(
                "b is now referred to by com.strumenta.kolasu.model.observable." +
                    "NodeWithReference(id=1, ref=Ref(foo)[Solved]).ref",
            ),
            obsB.observations,
        )
        clearObservations()

        nwr1.ref.referred = null
        assertEquals(listOf("ref: changed from b to null"), obs1.observations)
        assertEquals(listOf(), obs2.observations)
        assertEquals(listOf(), obsA.observations)
        assertEquals(
            listOf(
                "b is not referred anymore by com.strumenta.kolasu.model.observable." +
                    "NodeWithReference(id=1, ref=Ref(foo)[Solved]).ref",
            ),
            obsB.observations,
        )
        clearObservations()

        nwr2.ref.referred = a
        assertEquals(listOf(), obs1.observations)
        assertEquals(listOf("ref: changed from null to a"), obs2.observations)
        assertEquals(
            listOf(
                "a is now referred to by com.strumenta.kolasu.model.observable." +
                    "NodeWithReference(id=2, ref=Ref(bar)[Unsolved]).ref",
            ),
            obsA.observations,
        )
        assertEquals(listOf(), obsB.observations)
    }
}
