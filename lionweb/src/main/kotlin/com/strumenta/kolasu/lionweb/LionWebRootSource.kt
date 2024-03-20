package com.strumenta.kolasu.lionweb

import com.strumenta.kolasu.ids.Coordinates
import com.strumenta.kolasu.ids.IDLogic
import com.strumenta.kolasu.model.Source

data class LionWebRootSource(val sourceId: String) : Source(), IDLogic {

    override fun calculatedID(coordinates: Coordinates): String {
        return sourceId
    }
}
