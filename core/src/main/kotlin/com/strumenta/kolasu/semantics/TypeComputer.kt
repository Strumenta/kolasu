package com.strumenta.kolasu.semantics

import com.strumenta.kolasu.model.Node
import com.strumenta.kolasu.utils.memoize
import kotlin.reflect.KClass
import kotlin.reflect.full.isSuperclassOf

// instance

class TypeComputer(
    private val typingRules: MutableMap<KClass<out Node>, (Node) -> Node?> = mutableMapOf()
) {
    fun loadFrom(configuration: TypeComputerConfiguration, semantics: Semantics) {
        configuration.typingRules.mapValuesTo(this.typingRules) {
                (_, typingRule) ->
            { node: Node -> semantics.typingRule(node) }
        }
    }

    fun typeFor(node: Node? = null): Node? {
        return node?.let {
            this.typingRules.keys
                .filter { it.isSuperclassOf(node::class) }
                .sortedWith { left, right ->
                    when {
                        left.isSuperclassOf(right) -> 1
                        right.isSuperclassOf(left) -> -1
                        else -> 0
                    }
                }.firstOrNull()?.let { this.typingRules[it] }?.invoke(node)
        }
    }
}

// configuration

class TypeComputerConfiguration(
    val typingRules: MutableMap<KClass<out Node>, Semantics.(Node) -> Node?> = mutableMapOf(
        Node::class to { it }
    )
) {
    inline fun <reified N : Node> typeFor(nodeType: KClass<N>, crossinline typingRule: Semantics.(N) -> Node?) {
        this.typingRules.putIfAbsent(
            nodeType,
            { semantics: Semantics, node: Node ->
                if (node is N) semantics.typingRule(node) else null
            }.memoize()
        )
    }
}

// builder

fun typeComputer(init: TypeComputerConfiguration.() -> Unit) = TypeComputerConfiguration().apply(init)
