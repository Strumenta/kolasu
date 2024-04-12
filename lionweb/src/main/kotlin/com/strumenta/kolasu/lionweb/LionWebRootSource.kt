package com.strumenta.kolasu.lionweb

import com.strumenta.kolasu.ids.SemanticIDProvider
import com.strumenta.kolasu.model.Source

data class LionWebRootSource(val sourceId: String) : Source(), SemanticIDProvider {

    override fun calculatedID(): String {
        return sourceId
    }
}
