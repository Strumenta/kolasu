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

/**
 * Type-safe builder to create new [TypeProvider] instances.
 *
 * @param init configuration of the type provider
 * @return a [TypeProvider] instance realising the specific rules
 *
 * @author Lorenzo Addazi <lorenzo.addazi@strumenta.com>
 **/
fun typeProvider(init: TypeProviderConfigurator.() -> Unit): TypeProvider {
    return TypeProvider().apply { TypeProviderConfigurator(this).apply(init) }
}

/**
 * Annotation class grouping elements of the Type Provider
 * DSL - used to configure [TypeProvider] instances
 *
 * @author Lorenzo Addazi <lorenzo.addazi@strumenta.com>
 **/
@DslMarker
annotation class TypeProviderDsl

/**
 * Query-side representation of a [TypeProvider] associating
 * [Node] instances to the corresponding type - a [Node] instance as well.
 *
 * @param classTypeFactories type factories from node types
 * @param nodeTypeFactories type factories from node instances
 **/
class TypeProvider(
    val classTypeFactories: MutableMap<KClass<out Node>, () -> Node> = mutableMapOf(),
    val nodeTypeFactories: MutableMap<KClass<out Node>, Node.() -> Node> = mutableMapOf()
) : SemanticsProvider<Node, TypeProviderRule<*>>() {

    /**
     * Create type representation for the given [node].
     * @param node the node from which to build the type representation
     * @return a type instance corresponding to the given [node] type
     **/
    fun typeFromNode(node: Node): Node? {
        return this.nodeTypeFactories.keys
            .filter { it.isSuperclassOf(node::class) }
            .sortBySubclassesFirst()
            .firstOrNull()?.let { this.nodeTypeFactories[it]?.invoke(node) } ?: typeFromClass(node::class)
    }

    /**
     * Create type representation for the given [nodeType] - used during unification.
     * @param nodeType the node type from which to build the type representation
     * @return a type instance corresponding to the given [nodeType]
     **/
    fun typeFromClass(nodeType: KClass<*>): Node? {
        return this.classTypeFactories.keys
            .filter { it.isSuperclassOf(nodeType) }
            .sortBySubclassesFirst()
            .firstOrNull()?.let { this.classTypeFactories[it]?.invoke() }
    }
}

/**
 * Configuration-side representation of a [TypeProvider]
 * supporting the declarative specification of typing rules.
 *
 * @param provider the configured [TypeProvider] instance
 * @see SemanticsProviderConfigurator
 *
 * @author Lorenzo Addazi <lorenzo.addazi@strumenta.com>
 **/
@TypeProviderDsl
class TypeProviderConfigurator(
    provider: TypeProvider
) : SemanticsProviderConfigurator<TypeProvider, TypeProviderRule<*>, Node>(provider) {

    /**
     * Define a class type factory for the given [nodeType].
     * @param nodeType the target node type
     * @param init the class type factory configuration
     **/
    fun classTypeFactory(nodeType: KClass<out Node>, init: () -> Node) {
        this.provider.classTypeFactories[nodeType] = init
    }

    /**
     * Define a node type factory for the given [nodeType].
     * @param nodeType the target node type
     * @param init the node type factory configuration
     **/
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

/**
 * Type provider rule definition exposing the configuration API
 * for single typing rules and handling the actual evaluation.
 *
 * @see SemanticsProviderRule
 *
 * @author Lorenzo Addazi <lorenzo.addazi@strumenta.com>
 **/
@TypeProviderDsl
class TypeProviderRule<InputType : Node> : SemanticsProviderRule<InputType, Node>() {

    /**
     * The final type judgement of this rule.
     **/
    var type: Node? = null

    /**
     * Define the final type judgement as the
     * least common ancestor among two types - unification.
     * @param left the first type
     * @param right the other type
     **/
    fun merge(left: Node, right: Node) = runBeforeEvaluation { (_, provider) ->
        type = allNodeSuperclasses(left::class).plus(allNodeSuperclasses(right::class))
            .groupBy { it }.mapValues { it.value.size }.filterValues { it == 2 }
            .keys.sortBySubclassesFirst().firstOrNull()
            ?.let { TypeProvider::class.safeCast(provider)?.typeFromClass(it) }
    }

    /**
     * Retrieve all superclasses extending [Node] from [nodeType].
     * @param nodeType the subject node type
     * @return a sequence of superclasses extending [Node]
     **/
    private fun allNodeSuperclasses(nodeType: KClass<out Node>) = sequence {
        yield(nodeType)
        yieldAll(nodeType.allSuperclasses.filter { Node::class.isSuperclassOf(it) })
    }

    override fun getOutput(
        node: InputType,
        provider: SemanticsProvider<Node, *>,
        issues: MutableList<Issue>
    ) = this.type
}
