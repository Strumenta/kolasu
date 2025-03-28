package com.strumenta.kolasu.lionweb

import com.strumenta.kolasu.ids.SimpleSourceIdProvider
import com.strumenta.kolasu.ids.SourceShouldBeSetException
import com.strumenta.kolasu.model.Source
import io.lionweb.lioncore.java.language.Concept
import io.lionweb.lioncore.kotlin.BaseNode

class ParsingResultNode(val source: Source?) : BaseNode(LIONWEB_VERSION_USED_BY_KOLASU) {
    override fun calculateID(): String? {
        return try {
            SimpleSourceIdProvider().sourceId(source) + "_ParsingResult"
        } catch (_: SourceShouldBeSetException) {
            super.calculateID()
        }
    }

    override fun getClassifier(): Concept {
        return StarLasuLWLanguage.ParsingResult
    }
}
