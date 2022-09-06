package com.strumenta.kolasu.language

import com.strumenta.kolasu.model.Node
import com.strumenta.kolasu.model.walkChildren
import com.strumenta.kolasu.parsing.KolasuParser

/**
 * This permits to transform an AST into code. It can be used only for ASTs obtained from parsing, and not on
 * AST built programmatically.
 *
 * This is a work in progress.
 */
class Unparser {
    fun unparse(root: Node): String? {
        val sourceText = root.sourceText
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

/**
 * This permits to parse code into AST and viceversa going from an AST into code.
 * In the future, we may need to use a CodeGenerator, so that we can produce code also for AST created
 * programmatically.
 */
class LanguageModule(val parser: KolasuParser<*, *, *>, val unparser: Unparser)
