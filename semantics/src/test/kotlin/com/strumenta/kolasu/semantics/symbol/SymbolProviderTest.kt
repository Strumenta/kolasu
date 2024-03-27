package com.strumenta.kolasu.semantics.symbol

import com.strumenta.kolasu.ids.nodeIdProvider
import com.strumenta.kolasu.model.Node
import com.strumenta.kolasu.model.PossiblyNamed
import com.strumenta.kolasu.model.ReferenceByName
import com.strumenta.kolasu.model.kReferenceByNameProperties
import com.strumenta.kolasu.model.pos
import com.strumenta.kolasu.model.withPosition
import com.strumenta.kolasu.semantics.symbol.provider.symbolProvider
import org.junit.Test

data class PersonNode(
    override val name: String,
    val reference: ReferenceByName<PersonNode>? = null
) : Node(), PossiblyNamed
data class AnotherNode(
    val anotherProperty: String = "hello",
    val pippo: ReferenceByName<PersonNode> = ReferenceByName(
        name = "first"
    )
) : Node()

class SymbolProviderTest {

    @Test
    fun testSymbolFor() {
        val anotherNode = AnotherNode().withPosition(pos(10, 10, 10, 10))
        val firstPerson = PersonNode(name = "Lorenzo").withPosition(pos(1, 1, 1, 1))
        val secondPerson = PersonNode(
            name = "Federico",
            reference = ReferenceByName<PersonNode>(name = "Lorenzo").apply { this.position = pos(2, 2, 2, 2) }
        ).withPosition(pos(2, 1, 2, 10))

        val nodeIdProvider = nodeIdProvider {
            idFor(Node::class) { (node) ->
                "${node::class.qualifiedName!!}:${node.position!!.start.line}"
            }
        }
        val symbolProvider = symbolProvider(nodeIdProvider) {
            rule(PersonNode::class) { (node) ->
                include("name", "some_random_name")
                node.kReferenceByNameProperties().forEach { referenceByName ->
                    include(referenceByName.name, referenceByName.get(node))
                }
            }
            rule(AnotherNode::class) { (node) ->
                include("name", "some_random_name")
                include("anotherProperty", node.anotherProperty)
                include("pippo", node.pippo)
            }
        }

        val anotherSymbol = symbolProvider.from(anotherNode)
        val firstSymbol = symbolProvider.from(firstPerson)
        val secondSymbol = symbolProvider.from(secondPerson)
    }
}
