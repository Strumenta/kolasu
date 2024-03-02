package com.strumenta.kolasu.ids

import com.strumenta.kolasu.model.NodeLike
import com.strumenta.kolasu.model.Source
import com.strumenta.kolasu.model.containingProperty
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

    override fun id(kNode: NodeLike): String {
        val id = "${sourceIdProvider.sourceId(kNode.source)}_${kNode.positionalID}"
        return id
    }

    private val NodeLike.positionalID: String
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
