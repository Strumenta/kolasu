package com.strumenta.kolasu.language

import com.strumenta.kolasu.model.Node
import com.strumenta.kolasu.traversing.walkChildren

// TODO Alessio this is a work in progress
class Unparser {
    fun unparse(root: Node): String? {
        val sourceText = root.sourceText
        return if (sourceText != null) {
            var template: String = sourceText
            root.walkChildren()
                .filter { it.range != null }
                .sortedByDescending { it.range }
                .forEach {
                    val replacement = unparse(it)
                    if (replacement != null) {
                        template = template.replaceRange(
                            it.range!!.start.offset(template),
                            it.range!!.end.offset(template),
                            replacement
                        )
                    }
                }
            template
        } else null
    }
}
