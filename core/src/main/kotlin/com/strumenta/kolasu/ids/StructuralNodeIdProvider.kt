package com.strumenta.kolasu.ids

import com.strumenta.kolasu.model.ASTRoot
import com.strumenta.kolasu.model.Node
import com.strumenta.kolasu.model.Source

class ConstantSourceIdProvider(var value: String) : SourceIdProvider {
    override fun sourceId(source: Source?) = value
}

open class StructuralNodeIdProvider(var sourceIdProvider: SourceIdProvider = SimpleSourceIdProvider()) :
    NodeIdProvider {

    constructor(customSourceId: String) : this(ConstantSourceIdProvider(customSourceId))

    override fun idUsingCoordinates(kNode: Node, coordinates: Coordinates): String {
        if (kNode is SemanticIDProvider) {
            return kNode.calculatedID()
        }
        val canBeRoot = kNode::class.annotations.any { it is ASTRoot }
        val mustBeRoot = kNode::class.annotations.any { it is ASTRoot && !it.canBeNotRoot }
        when (coordinates) {
            is RootCoordinates -> {
                if (!canBeRoot) {
                    throw NodeShouldNotBeRootException("Node $kNode should not be root")
                }
                val sourceId = try {
                    sourceIdProvider.sourceId(kNode.source)
                } catch (e: SourceShouldBeSetException) {
                    throw SourceShouldBeSetException("Source should be set for node $kNode", e)
                } catch (e: IDGenerationException) {
                    throw IDGenerationException("Cannot get source id for node $kNode", e)
                }
                return "${sourceId}_root"
            }
            is NonRootCoordinates -> {
                if (mustBeRoot) {
                    throw NodeShouldBeRootException("Node $kNode should be root")
                }
                val index = coordinates.indexInContainment
                val postfix = if (index == 0) coordinates.containmentName else "${coordinates.containmentName}_$index"
                return "${coordinates.containerID!!}_$postfix"
            }
        }
    }
}
