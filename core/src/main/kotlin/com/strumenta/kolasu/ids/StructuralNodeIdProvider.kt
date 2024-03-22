package com.strumenta.kolasu.ids

import com.strumenta.kolasu.model.NodeLike
import com.strumenta.kolasu.model.Source
import com.strumenta.kolasu.model.indexInContainingProperty

class ConstantSourceIdProvider(
    var value: String,
) : SourceIdProvider {
    override fun sourceId(source: Source?) = value
}

open class StructuralNodeIdProvider(
    var sourceIdProvider: SourceIdProvider = SimpleSourceIdProvider(),
) : NodeIdProvider {
    constructor(customSourceId: String) : this(ConstantSourceIdProvider(customSourceId))

    override fun idUsingCoordinates(
        kNode: NodeLike,
        coordinates: Coordinates,
    ): String {
        if (kNode is IDLogic) {
            return kNode.calculatedID(coordinates)
        }
        when (coordinates) {
            is RootCoordinates -> {
                val sourceId =
                    try {
                        sourceIdProvider.sourceId(kNode.source)
                    } catch (e: IDGenerationException) {
                        throw IDGenerationException("Cannot get source id for node $kNode", e)
                    }
                return "${sourceId}_root"
            }
            is NonRootCoordinates -> {
                // TODO consider getting index from Coordinates
                val index = kNode.indexInContainingProperty()!!
                val postfix = if (index == 0) coordinates.containmentName else "${coordinates.containmentName}_$index"
                return "${coordinates.containerID!!}_$postfix"
            }
        }
    }
}
