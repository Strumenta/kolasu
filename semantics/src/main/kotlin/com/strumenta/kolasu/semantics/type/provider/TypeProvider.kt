package com.strumenta.kolasu.semantics.type.provider

import com.strumenta.kolasu.model.Node
import com.strumenta.kolasu.semantics.provider.SemanticsProvider
import com.strumenta.kolasu.semantics.provider.SemanticsProviderConfigurator
import com.strumenta.kolasu.semantics.provider.SemanticsProviderRule
import com.strumenta.kolasu.utils.sortBySubclassesFirst
import com.strumenta.kolasu.validation.Issue
import kotlin.reflect.KClass
import kotlin.reflect.cast
import kotlin.reflect.full.allSuperclasses
import kotlin.reflect.full.isSuperclassOf
import kotlin.reflect.safeCast

// //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

fun typeProvider(init: TypeProviderConfigurator.() -> Unit): TypeProvider {
    return TypeProvider().apply { TypeProviderConfigurator(this).apply(init) }
}

// //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

@DslMarker
annotation class TypeProviderDsl

// //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

class TypeProvider(
    val classTypeFactories: MutableMap<KClass<out Node>, () -> Node> = mutableMapOf(),
    val nodeTypeFactories: MutableMap<KClass<out Node>, Node.() -> Node> = mutableMapOf()
) : SemanticsProvider<Node, TypeProviderRule<*>>() {

    fun typeFromNode(node: Node): Node? {
        return this.nodeTypeFactories.keys
            .filter { it.isSuperclassOf(node::class) }
            .sortBySubclassesFirst()
            .firstOrNull()?.let { this.nodeTypeFactories[it]?.invoke(node) } ?: typeFromClass(node::class)
    }

    fun typeFromClass(nodeType: KClass<*>): Node? {
        return this.classTypeFactories.keys
            .filter { it.isSuperclassOf(nodeType) }
            .sortBySubclassesFirst()
            .firstOrNull()?.let { this.classTypeFactories[it]?.invoke() }
    }
}

// //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

@TypeProviderDsl
class TypeProviderConfigurator(
    provider: TypeProvider
) : SemanticsProviderConfigurator<TypeProvider, TypeProviderRule<*>, Node>(provider) {

    fun classTypeFactory(nodeType: KClass<out Node>, init: () -> Node) {
        this.provider.classTypeFactories[nodeType] = init
    }

    fun <NodeType : Node> nodeTypeFactory(nodeType: KClass<NodeType>, init: NodeType.() -> Node) {
        this.provider.nodeTypeFactories[nodeType] = {
            require(nodeType::class.isSuperclassOf(this::class)) {
                "Factory execution error: incompatible input received" +
                    " (received: ${this::class.qualifiedName}, expected: ${nodeType.qualifiedName})"
            }
            init(nodeType.cast(this))
        }
    }

    override fun <InputType : Node> createRule(nodeType: KClass<InputType>): TypeProviderRule<*> {
        return TypeProviderRule<InputType>()
    }
}

// //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

@TypeProviderDsl
class TypeProviderRule<InputType : Node> : SemanticsProviderRule<InputType, Node>() {

    var type: Node? = null

    fun merge(left: Node, right: Node) = runBeforeEvaluation { (_, provider) ->
        type = allNodeSuperclasses(left::class).plus(allNodeSuperclasses(right::class))
            .groupBy { it }.mapValues { it.value.size }.filterValues { it == 2 }
            .keys.sortBySubclassesFirst().firstOrNull()
            ?.let { TypeProvider::class.safeCast(provider)?.typeFromClass(it) }
    }

    private fun allNodeSuperclasses(nodeType: KClass<out Node>) = sequence {
        yield(nodeType)
        yieldAll(nodeType.allSuperclasses.filter { Node::class.isSuperclassOf(it) })
    }

    override fun getOutput(
        input: InputType,
        provider: SemanticsProvider<Node, *>,
        issues: MutableList<Issue>
    ) = this.type
}

// //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
