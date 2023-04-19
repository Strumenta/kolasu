package com.strumenta.kolasu.antlr.mapping

import com.strumenta.kolasu.antlr.parsing.withParseTreeNode
import com.strumenta.kolasu.model.ASTNode
import com.strumenta.kolasu.transformation.ASTTransformer
import com.strumenta.kolasu.transformation.NodeTransformer
import com.strumenta.kolasu.validation.Issue
import org.antlr.v4.runtime.ParserRuleContext
import kotlin.reflect.KClass

/**
 * Implements a transformation from an ANTLR parse tree (the output of the parser) to an AST (a higher-level
 * representation of the source code).
 */
open class ParseTreeToASTTransformer(issues: MutableList<Issue> = mutableListOf(), allowGenericNode: Boolean = true) :
    ASTTransformer(issues, allowGenericNode) {
    /**
     * Performs the transformation of a node and, recursively, its descendants. In addition to the overridden method,
     * it also assigns the parseTreeNode to the AST node so that it can keep track of its position.
     * However, a node transformer can override the parseTreeNode of the nodes it creates (but not the parent).
     */
    override fun transform(source: Any?, parent: ASTNode?): ASTNode? {
        val node = super.transform(source, parent)
        if (node != null && node.origin == null && source is ParserRuleContext) {
            node.withParseTreeNode(source)
        }
        return node
    }

//    override fun getSource(node: ASTNode, source: Any): Any {
//        val origin = node.origin
//        return if (origin is ParseTreeOrigin) origin.parseTree else source
//    }
//
//    override fun asOrigin(source: Any): Origin? {
//        return if (source is ParseTree) ParseTreeOrigin(source) else null
//    }

    fun <P : ParserRuleContext> registerNodeTransformerUnwrappingChild(
        kclass: KClass<P>
    ): NodeTransformer<P, ASTNode> = registerNodeTransformer(kclass) { source, transformer, _ ->
        val nodeChildren = source.children.filterIsInstance<ParserRuleContext>()
        require(nodeChildren.size == 1) {
            "Node $source (${source.javaClass}) has ${nodeChildren.size} " +
                "node children: $nodeChildren"
        }
        transformer.transform(nodeChildren[0]) as ASTNode
    }
}
