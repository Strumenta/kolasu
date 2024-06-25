package com.strumenta.kolasu.antlr4j.mapping

import com.strumenta.kolasu.antlr4j.parsing.ParseTreeOrigin
import com.strumenta.kolasu.antlr4j.parsing.withParseTreeNode
import com.strumenta.kolasu.model.NodeLike
import com.strumenta.kolasu.model.Origin
import com.strumenta.kolasu.model.Source
import com.strumenta.kolasu.transformation.ASTTransformer
import com.strumenta.kolasu.transformation.NodeTransformer
import com.strumenta.kolasu.validation.Issue
import org.antlr.v4.runtime.ParserRuleContext
import org.antlr.v4.runtime.tree.ParseTree
import kotlin.reflect.KClass

/**
 * Implements a transformation from an ANTLR parse tree (the output of the parser) to an AST (a higher-level
 * representation of the source code).
 */
open class ParseTreeToASTTransformer(
    issues: MutableList<Issue> = mutableListOf(),
    allowGenericNode: Boolean = true,
    val source: Source? = null,
    throwOnUnmappedNode: Boolean = true,
) : ASTTransformer(issues, allowGenericNode, throwOnUnmappedNode) {
    /**
     * Performs the transformation of a node and, recursively, its descendants. In addition to the overridden method,
     * it also assigns the parseTreeNode to the AST node so that it can keep track of its range.
     * However, a node transformer can override the parseTreeNode of the nodes it creates (but not the parent).
     */
    override fun transformIntoNodes(
        source: Any?,
        parent: NodeLike?,
        expectedType: KClass<out NodeLike>,
    ): List<NodeLike> {
        val transformed = super.transformIntoNodes(source, parent, expectedType)
        return transformed
            .map { node ->
                if (source is ParserRuleContext) {
                    if (node.origin == null) {
                        node.withParseTreeNode(source, this.source)
                    } else if (node.range != null && node.source == null) {
                        node.range!!.source = this.source
                    }
                }
                return listOf(node)
            }.flatten()
    }

    override fun getSource(
        node: NodeLike,
        source: Any,
    ): Any {
        val origin = node.origin
        return if (origin is ParseTreeOrigin) origin.parseTree else source
    }

    override fun asOrigin(source: Any): Origin? = if (source is ParseTree) ParseTreeOrigin(source) else null

    /**
     * Often in ANTLR grammar we have rules which wraps other rules and act as
     * wrapper. When there is only a ParserRuleContext child we can transform
     * that child and return that result.
     */
    fun <P : ParserRuleContext> registerNodeTransformerUnwrappingChild(
        kclass: KClass<P>,
    ): NodeTransformer<P, NodeLike> =
        registerNodeTransformer(kclass) { source, transformer, _ ->
            val nodeChildren = source.children.filterIsInstance<ParserRuleContext>()
            require(nodeChildren.size == 1) {
                "Node $source (${source.javaClass}) has ${nodeChildren.size} " +
                    "node children: $nodeChildren"
            }
            transformer.transform(nodeChildren[0]) as NodeLike
        }

    /**
     * Alternative to registerNodeFactoryUnwrappingChild(KClass) which is slightly more concise.
     */
    inline fun <reified P : ParserRuleContext> registerNodeFactoryUnwrappingChild(): NodeTransformer<P, NodeLike> =
        registerNodeTransformerUnwrappingChild(P::class)
}
