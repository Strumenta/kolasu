package com.strumenta.kolasu.lionweb

import com.strumenta.kolasu.model.Source
import com.strumenta.kolasu.model.SourceWithID
import io.lionweb.lioncore.java.utils.CommonChecks

data class LionWebSource(val sourceId: String) : Source(), SourceWithID {
    override fun sourceID(): String = sourceId

    init {
        if (!CommonChecks.isValidID(sourceId)) {
            throw IllegalArgumentException("Illegal SourceId provided")
        }
    }
}
