package com.strumenta.kolasu.model

import com.strumenta.kolasu.language.Containment
import com.strumenta.kolasu.language.Feature

fun Feature.valueToString(node: NodeLike): String {
    val value = this.value(node) ?: return "null"
    return when {
        this is Containment -> {
            if (multiplicity == Multiplicity.MANY) {
                "[${(value as Collection<NodeLike>).joinToString(",") { it.nodeType }}]"
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
