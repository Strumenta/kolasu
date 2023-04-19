package com.strumenta.kolasu.mapping

import com.strumenta.kolasu.model.Node
import com.strumenta.kolasu.transformation.NodeFactory
import org.antlr.v4.runtime.ParserRuleContext
import kotlin.reflect.KClass

fun <T> ParseTreeToASTTransformer.translateCasted(original: ParserRuleContext): T {
    val result = transform(original)
    if (result is Nothing) {
        throw IllegalStateException("Transformation produced Nothing")
    }
    return result as T
}

fun <T> ParseTreeToASTTransformer.translateOptCasted(original: ParserRuleContext?): T? {
    return if (original == null) {
        null
    } else {
        transform(original) as T
    }
}

fun <T> ParseTreeToASTTransformer.translateList(original: Collection<out ParserRuleContext>?): MutableList<T> {
    return original?.map { transform(it) as T }?.toMutableList() ?: mutableListOf()
}

fun <T> ParseTreeToASTTransformer.translateOptional(original: ParserRuleContext?): T? {
    return original?.let { transform(it) as T }
}

fun <T> ParseTreeToASTTransformer.translateOnlyChild(parent: ParserRuleContext): T {
    return translateCasted(parent.onlyChild)
}

inline fun <P : ParserRuleContext> ParseTreeToASTTransformer.registerNodeFactoryUnwrappingChild(
    kclass: KClass<P>
): NodeFactory<P, Node> = registerNodeFactory(kclass) { source, transformer, _ ->
    val nodeChildren = source.children.filterIsInstance<ParserRuleContext>()
    require(nodeChildren.size == 1) { "Node $source (${source.javaClass}) has ${nodeChildren.size} nide children: $nodeChildren" }
    transformer.transform(nodeChildren[0]) as Node
}

val ParserRuleContext.onlyChild: ParserRuleContext
    get() {
    val nodeChildren = children.filterIsInstance<ParserRuleContext>()
    require(nodeChildren.size == 1)
    require(nodeChildren[0] is ParserRuleContext)
    return nodeChildren[0]
}
