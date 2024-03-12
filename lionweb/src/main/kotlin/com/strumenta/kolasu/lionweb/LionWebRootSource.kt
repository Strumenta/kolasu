package com.strumenta.kolasu.lionweb

import com.strumenta.kolasu.ids.IDLogic
import com.strumenta.kolasu.model.Source

data class LionWebRootSource(val sourceId: String) : Source(), IDLogic {
    override val calculatedID: String
        get() = sourceId
}
