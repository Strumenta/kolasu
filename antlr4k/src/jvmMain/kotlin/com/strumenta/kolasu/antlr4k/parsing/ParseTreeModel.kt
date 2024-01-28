package com.strumenta.kolasu.antlr4k.parsing

import org.antlr.v4.kotlinruntime.ParserRuleContext
import org.antlr.v4.kotlinruntime.Vocabulary
import org.antlr.v4.kotlinruntime.tree.TerminalNode

/**
 * Given an actual parse-tree produced by ANTLR, it creates a Parse Tree model.
 */
fun toParseTreeModel(
    node: ParserRuleContext,
    vocabulary: Vocabulary,
): ParseTreeNode {
    val res = ParseTreeNode(node.javaClass.simpleName.removeSuffix("Context"))
    node.children?.forEach { c ->
        when (c) {
            is ParserRuleContext -> res.child(toParseTreeModel(c, vocabulary))
            is TerminalNode -> res.child(ParseTreeLeaf(vocabulary.getSymbolicName(c.symbol.type)!!, c.text))
        }
    }
    return res
}
