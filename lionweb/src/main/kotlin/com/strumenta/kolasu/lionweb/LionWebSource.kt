package com.strumenta.kolasu.lionweb

import com.strumenta.kolasu.model.Source
import com.strumenta.kolasu.model.SourceWithID

data class LionWebSource(
    val sourceId: String,
) : Source(),
    SourceWithID {
    override fun sourceID(): String = sourceId

    override fun stringDescription(): String = "LionWebSource $sourceId"
}
