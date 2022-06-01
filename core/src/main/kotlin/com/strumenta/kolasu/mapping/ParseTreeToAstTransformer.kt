package com.strumenta.kolasu.mapping

import com.strumenta.kolasu.model.Node
import com.strumenta.kolasu.model.Origin
import com.strumenta.kolasu.model.Position
import com.strumenta.kolasu.parsing.ParseTreeOrigin
import com.strumenta.kolasu.parsing.withParseTreeNode
import com.strumenta.kolasu.transfomation.ASTTransformer
import com.strumenta.kolasu.validation.Issue
import org.antlr.v4.runtime.ParserRuleContext
import org.antlr.v4.runtime.tree.ParseTree


/**
 * Implements a transformation from an ANTLR parse tree (the output of the parser) to an AST (a higher-level
 * representation of the source code).
 */
open class ParseTreeToASTTransformer(issues: MutableList<Issue> = mutableListOf()): ASTTransformer(issues) {
    /**
     * Performs the transformation of a node and, recursively, its descendants. In addition to the overridden method,
     * it also assigns the parseTreeNode to the AST node so that it can keep track of its position.
     * However, a node factory can override the parseTreeNode of the nodes it creates (but not the parent).
     */
    override fun transform(source: Any?, parent: Node?): Node? {
        val node = super.transform(source, parent)
        if(node != null && node.origin == null && source is ParserRuleContext) {
            node.withParseTreeNode(source)
        }
        return node
    }

    override fun getSource(node: Node, source: Any): Any {
        val origin = node.origin
        return if (origin is ParseTreeOrigin) origin.parseTree else source
    }

    override fun asOrigin(source: Any): Origin? {
        return if (source is ParseTree) ParseTreeOrigin(source) else null
    }
}