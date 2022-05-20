package com.strumenta.kolasu.language

import com.strumenta.kolasu.model.Node
import com.strumenta.kolasu.model.walkChildren
import com.strumenta.kolasu.parsing.KolasuParser

class Unparser {
    fun unparse(root: Node): String? {
        val sourceText = root.origin?.sourceText
        return if (sourceText != null) {
            var template: String = sourceText
            root.walkChildren()
                .filter { it.position != null }
                .sortedByDescending { it.position }
                .forEach {
                    val replacement = unparse(it)
                    if (replacement != null) {
                        template = template.replaceRange(
                            it.position!!.start.offset(template),
                            it.position!!.end.offset(template),
                            replacement
                        )
                    }
                }
            template
        } else null
    }
}

class LanguageModule(val parser: KolasuParser<*, *, *>, val unparser: Unparser)