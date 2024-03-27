package com.strumenta.kolasu.semantics.scope

import org.junit.Test

class ScopeProviderTest {

    @Test
    fun testScopeFor() {
//        val internalNode = InternalNode(
//            name = "internal",
//            value = ValueNode("internalValue").withPosition(pos(2, 2, 2, 2))
//        ).withPosition(pos(1, 1, 1, 1))
//        val externalNode = ExternalNode(
//            name = "external",
//            value = ValueNode("externalValue").withPosition(pos(4, 4, 4, 4))
//        ).withPosition(pos(3, 3, 3, 3))
//        val containerNode = ContainerNode(
//            ReferenceByName<InternalNode>(name = "internal").apply {
//                this.position = pos(6, 6, 6, 6)
// //                this.referred = internalNode
//            },
//            ReferenceByName<ExternalNode>(name = "external").apply {
//                this.position = pos(7, 7, 7, 7)
// //                this.referred = externalNode
//            },
//            ReferenceByName<ValueNode>(name = "value").apply {
//                this.position = pos(8, 8, 8, 8)
//            }
//        ).withPosition(pos(5, 5, 5, 5))
//        val worldNode = WorldNode(
//            listOf(internalNode),
//            listOf(externalNode),
//            listOf(containerNode)
//        ).withPosition(pos(9, 9, 9, 9))
//
//        worldNode.assignParents()
//
//        nodeRepository.store(internalNode)
//        nodeRepository.store(internalNode.value)
//        nodeRepository.store(externalNode)
//        nodeRepository.store(externalNode.value)
//        nodeRepository.store(containerNode)
//        nodeRepository.store(worldNode)
//
//        val scope = myScopeProvider.scopeFor(containerNode, ContainerNode::valueNode)
//
//        assertTrue { scope.names().size == 1 }
    }
}
