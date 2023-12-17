package com.strumenta.kolasu.semantics

import com.strumenta.kolasu.model.INode
import com.strumenta.kolasu.utils.memoize
import com.strumenta.kolasu.utils.sortBySubclassesFirst
import kotlin.reflect.KClass
import kotlin.reflect.full.isSuperclassOf

// instance

class TypeComputer(
    private val typingRules: MutableMap<KClass<out INode>, (INode) -> INode?> = mutableMapOf()
) {
    fun loadFrom(configuration: TypeComputerConfiguration, semantics: Semantics) {
        configuration.typingRules.mapValuesTo(this.typingRules) {
                (_, typingRule) ->
            { node: INode -> semantics.typingRule(node) }
        }
    }

    fun typeFor(node: INode? = null): INode? {
        return node?.let {
            this.typingRules.keys
                .filter { it.isSuperclassOf(node::class) }
                .sortBySubclassesFirst()
                .firstOrNull()?.let { this.typingRules[it] }?.invoke(node)
        }
    }
}

// configuration

class TypeComputerConfiguration(
    val typingRules: MutableMap<KClass<out INode>, Semantics.(INode) -> INode?> = mutableMapOf(
        INode::class to { it }
    )
) {
    inline fun <reified N : INode> typeFor(nodeType: KClass<N>, crossinline typingRule: Semantics.(N) -> INode?) {
        this.typingRules.putIfAbsent(
            nodeType,
            { semantics: Semantics, node: INode ->
                if (node is N) semantics.typingRule(node) else null
            }.memoize()
        )
    }
}

// builder

fun typeComputer(init: TypeComputerConfiguration.() -> Unit) = TypeComputerConfiguration().apply(init)
