package com.strumenta.kolasu.semantics

import com.strumenta.kolasu.model.NodeLike
import com.strumenta.kolasu.utils.memoize
import com.strumenta.kolasu.utils.sortBySubclassesFirst
import kotlin.reflect.KClass
import kotlin.reflect.full.isSuperclassOf

// instance
@Deprecated("The corresponding component in the semantics module should be used instead.")
class TypeComputer(
    private val typingRules: MutableMap<KClass<out NodeLike>, (NodeLike) -> NodeLike?> = mutableMapOf(),
) {
    fun loadFrom(
        configuration: TypeComputerConfiguration,
        semantics: Semantics,
    ) {
        configuration.typingRules.mapValuesTo(this.typingRules) { (_, typingRule) ->
            { node: NodeLike -> semantics.typingRule(node) }
        }
    }

    fun typeFor(node: NodeLike? = null): NodeLike? =
        node?.let {
            this
                .typingRules
                .keys
                .filter { it.isSuperclassOf(node::class) }
                .sortBySubclassesFirst()
                .firstOrNull()
                ?.let { this.typingRules[it] }
                ?.invoke(node)
        }
}

// configuration
@Deprecated("The corresponding component in the semantics module should be used instead.")
class TypeComputerConfiguration(
    val typingRules: MutableMap<KClass<out NodeLike>, Semantics.(NodeLike) -> NodeLike?> =
        mutableMapOf(
            NodeLike::class to { it },
        ),
) {
    inline fun <reified N : NodeLike> typeFor(
        nodeType: KClass<N>,
        crossinline typingRule: Semantics.(N) -> NodeLike?,
    ) {
        this.typingRules.putIfAbsent(
            nodeType,
            { semantics: Semantics, node: NodeLike ->
                if (node is N) semantics.typingRule(node) else null
            }.memoize(),
        )
    }
}

// builder

fun typeComputer(init: TypeComputerConfiguration.() -> Unit) = TypeComputerConfiguration().apply(init)
