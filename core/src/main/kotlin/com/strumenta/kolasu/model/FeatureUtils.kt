package com.strumenta.kolasu.model

import com.strumenta.kolasu.language.Containment
import com.strumenta.kolasu.language.Feature
import com.strumenta.kolasu.testing.IgnoreChildren

fun Feature.valueToString(node: NodeLike): String {
    val value = this.value(node) ?: return "null"
    return when {
        this is Containment -> {
            if (multiplicity == Multiplicity.MANY) {
                when (value) {
                    is IgnoreChildren<*> -> "<Ignore Children Placeholder>"
                    else -> "[${(value as Collection<NodeLike>).joinToString(",") { it.nodeType }}]"
                }
            } else {
                "${(value as NodeLike).nodeType}(...)"
            }
        }
        else -> {
            if (multiplicity == Multiplicity.MANY) {
                "[${(value as Collection<*>).joinToString(",") { it.toString() }}]"
            } else {
                value.toString()
            }
        }
    }
}
