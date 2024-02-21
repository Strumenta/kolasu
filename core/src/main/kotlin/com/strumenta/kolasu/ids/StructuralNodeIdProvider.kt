package com.strumenta.kolasu.ids

import com.strumenta.kolasu.model.Node
import com.strumenta.kolasu.model.Source
import com.strumenta.kolasu.model.containingProperty
import com.strumenta.kolasu.model.indexInContainingProperty

class ConstantSourceIdProvider(var value: String) : SourceIdProvider {
    override fun sourceId(source: Source?) = value
}

open class StructuralNodeIdProvider(var sourceIdProvider: SourceIdProvider = SimpleSourceIdProvider()) :
    NodeIdProvider {

    constructor(customSourceId: String) : this(ConstantSourceIdProvider(customSourceId))

    override fun id(kNode: Node): String {
        val id = "${sourceIdProvider.sourceId(kNode.source)}_${kNode.positionalID}"
        return id
    }

    private val Node.positionalID: String
        get() {
            return if (this.parent == null) {
                "root"
            } else {
                val cp = this.containingProperty()!!
                val postfix = if (cp.multiple) "${cp.name}_${this.indexInContainingProperty()!!}" else cp.name
                "${this.parent!!.positionalID}_$postfix"
            }
        }
}
