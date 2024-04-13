package com.strumenta.kolasu.lionweb

import com.strumenta.kolasu.ids.ConstantSourceIdProvider
import com.strumenta.kolasu.ids.Coordinates
import com.strumenta.kolasu.ids.SimpleSourceIdProvider
import com.strumenta.kolasu.ids.SourceIdProvider
import com.strumenta.kolasu.ids.StructuralNodeIdProvider
import com.strumenta.kolasu.model.NodeLike
import io.lionweb.lioncore.java.utils.CommonChecks

class StructuralLionWebNodeIdProvider(
    sourceIdProvider: SourceIdProvider = SimpleSourceIdProvider(),
) : StructuralNodeIdProvider(sourceIdProvider) {
    constructor(customSourceId: String) : this(ConstantSourceIdProvider(customSourceId))

    override fun idUsingCoordinates(
        kNode: NodeLike,
        coordinates: Coordinates,
    ): String {
        val id = super.idUsingCoordinates(kNode, coordinates)
        if (!CommonChecks.isValidID(id)) {
            throw IllegalStateException("An invalid LionWeb Node ID has been produced: $id. Produced for $kNode")
        }
        return id
    }
}
