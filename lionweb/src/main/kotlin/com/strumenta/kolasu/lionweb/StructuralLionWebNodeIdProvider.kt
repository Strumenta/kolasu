package com.strumenta.kolasu.lionweb

import com.strumenta.kolasu.model.NodeLike
import com.strumenta.kolasu.model.Source
import com.strumenta.kolasu.model.containingProperty
import com.strumenta.kolasu.model.indexInContainingProperty
import io.lionweb.lioncore.java.utils.CommonChecks

class ConstantSourceIdProvider(
    var value: String,
) : SourceIdProvider {
    override fun sourceId(source: Source?) = value
}

class StructuralLionWebNodeIdProvider(
    var sourceIdProvider: SourceIdProvider = SimpleSourceIdProvider(),
) : LionWebNodeIdProvider {
    constructor(customSourceId: String) : this(ConstantSourceIdProvider(customSourceId))

    override fun id(kNode: NodeLike): String {
        val id = "${sourceIdProvider.sourceId(kNode.source)}_${kNode.positionalID}"
        if (!CommonChecks.isValidID(id)) {
            throw IllegalStateException("An invalid LionWeb Node ID has been produced")
        }
        return id
    }

    private val KNode.positionalID: String
        get() {
            return if (this.parent == null) {
                "root"
            } else {
                val cp = this.containingProperty()!!
                val postfix = if (cp.isMultiple) "${cp.name}_${this.indexInContainingProperty()!!}" else cp.name
                "${this.parent!!.positionalID}_$postfix"
            }
        }
}
