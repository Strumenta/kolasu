package com.strumenta.kolasu.transformation

import com.strumenta.kolasu.model.*
import com.strumenta.kolasu.validation.Issue
import com.strumenta.kolasu.validation.IssueSeverity
import java.lang.IllegalStateException
import kotlin.reflect.KClass
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.KProperty1
import kotlin.reflect.full.*

/**
 * A child of an AST node that is automatically populated from a source tree.
 */
annotation class Mapped(val path: String = "")

/**
 * Factory that, given a tree node, will instantiate the corresponding transformed node.
 */
class NodeFactory<Source>(
    val constructor: (Source, ASTTransformer, NodeFactory<Source>) -> Node,
    val children: MutableMap<String, ChildNodeFactory<Source, *, *>?> = mutableMapOf()
) {
    fun withChild(
        sourceProperty: KProperty1<Source, *>,
        property: KMutableProperty1<*, *>,
        type: KClass<*>? = null
    ): NodeFactory<Source> = withChild(
        (sourceProperty as KProperty1<Source, Any>)::get,
        (property as KMutableProperty1<Any, Any?>)::set,
        property.name,
        type
    )

    fun <Target : Any> withChild(
        path: String,
        property: KMutableProperty1<Target, *>,
        scopedToType: KClass<Target>? = null
    ): NodeFactory<Source> =
        withChild(getter(path), (property as KMutableProperty1<Any, Any?>)::set, property.name, scopedToType)

    fun <Target : Any> withChild(
        get: (Source) -> Any?,
        property: KMutableProperty1<in Target, *>,
        scopedToType: KClass<Target>? = null
    ): NodeFactory<Source> =
        withChild(get, (property as KMutableProperty1<Target, Any?>)::set, property.name, scopedToType)

    fun <Target : Any, Child : Any> withChild(
        get: (Source) -> Any?,
        set: (Target, Child?) -> Unit,
        name: String,
        type: KClass<*>? = null
    ): NodeFactory<Source> {
        val prefix = if (type != null) type.qualifiedName + "#" else ""
        children[prefix + name] = ChildNodeFactory(prefix + name, get, set)
        return this
    }

    fun getter(path: String) = { src: Source ->
        var sub: Any? = src
        for (elem in path.split('.')) {
            if (sub == null) {
                break
            }
            sub = getSubExpression(sub, elem)
        }
        sub
    }

    private fun getSubExpression(src: Any, elem: String): Any? {
        return if (src is Collection<*>) {
            src.map { getSubExpression(it!!, elem) }
        } else {
            val sourceProp = src::class.memberProperties.find { it.name == elem }
            if (sourceProp == null) {
                val sourceMethod =
                    src::class.memberFunctions.find { it.name == elem && it.parameters.size == 1 }
                        ?: throw Error("$elem not found in $src (${src::class})")
                sourceMethod.call(src)
            } else {
                (sourceProp as KProperty1<Any, Any>).get(src)
            }
        }
    }
}

/**
 * Information on how to retrieve a child node.
 */
data class ChildNodeFactory<Source, Target, Child>(
    val name: String,
    val get: (Source) -> Any?,
    val setter: (Target, Child?) -> Unit
) {
    fun set(node: Target, child: Child?) {
        try {
            setter(node, child)
        } catch (e: Exception) {
            throw Exception("$name could not set child $child of $node using $setter", e)
        }
    }
}

/**
 * Sentinel value used to represent the information that a given property is not a child node.
 */
private val NO_CHILD_NODE = ChildNodeFactory<Any, Any, Any>("", { x -> x }, { _, _ -> })

/**
 * Implementation of a tree-to-tree transformation. For each source node type, we can register a factory that knows how
 * to create a transformed node. Then, this transformer can read metadata in the transformed node to recursively
 * transform and assign children.
 * If no factory is provided for a source node type, a GenericNode is created, and the processing of the subtree stops
 * there.
 */
open class ASTTransformer(
    /**
     * Additional issues found during the transformation process.
     */
    val issues: MutableList<Issue> = mutableListOf(),
    val allowGenericNode: Boolean = true
) {
    /**
     * Factories that map from source tree node to target tree node.
     */
    val factories = mutableMapOf<KClass<*>, NodeFactory<*>>()

    private val _knownClasses = mutableMapOf<String, MutableSet<KClass<*>>>()
    val knownClasses: Map<String, Set<KClass<*>>> = _knownClasses

    /**
     * Performs the transformation of a node and, recursively, its descendants.
     */
    @JvmOverloads
    open fun transform(source: Any?, parent: Node? = null): Node? {
        if (source == null) {
            return null
        }
        if (source is Collection<*>) {
            throw Error("Mapping error: received collection when value was expected")
        }
        val factory = getNodeFactory(source::class) as NodeFactory<Any>?
        val node: Node
        if (factory != null) {
            node = makeNode(factory, source, allowGenericNode = allowGenericNode)
            node::class.processProperties { pd ->
                val childKey = node::class.qualifiedName + "#" + pd.name
                var childNodeFactory = factory.children[childKey]
                if (childNodeFactory == null) {
                    childNodeFactory = factory.children[pd.name]
                }
                if (childNodeFactory != null) {
                    if (childNodeFactory != NO_CHILD_NODE) {
                        setChild(childNodeFactory, source, node, pd)
                    }
                } else {
                    val targetProp = node::class.memberProperties.find { it.name == pd.name }
                    val mapped = targetProp?.findAnnotation<Mapped>()
                    if (targetProp is KMutableProperty1 && mapped != null) {
                        val path = (mapped.path.ifEmpty { targetProp.name })
                        childNodeFactory = ChildNodeFactory(
                            childKey, factory.getter(path), (targetProp as KMutableProperty1<Any, Any?>)::set
                        )
                        factory.children[childKey] = childNodeFactory
                        setChild(childNodeFactory, source, node, pd)
                    } else {
                        factory.children[childKey] = NO_CHILD_NODE
                    }
                }
            }
            node.parent = parent
        } else {
            if (allowGenericNode) {
                val origin = asOrigin(source)
                node = GenericNode(parent).withOrigin(origin)
                issues.add(
                    Issue.semantic(
                        "Source node not mapped: ${source::class.qualifiedName}",
                        IssueSeverity.INFO,
                        origin?.position
                    )
                )
            } else {
                throw IllegalStateException("Unable to translate node $source (class ${source.javaClass})")
            }
        }
        return node
    }

    protected open fun asOrigin(source: Any): Origin? = if (source is Origin) source else null

    protected open fun setChild(
        childNodeFactory: ChildNodeFactory<*, *, *>,
        source: Any,
        node: Node,
        pd: PropertyTypeDescription
    ) {
        val src = (childNodeFactory as ChildNodeFactory<Any, Any, Any>).get(getSource(node, source))
        val child = if (pd.multiple) {
            (src as Collection<*>?)?.map { transform(it, node) } ?: listOf<Node?>()
        } else {
            transform(src, node)
        }
        try {
            childNodeFactory.set(node, child)
        } catch (e: IllegalArgumentException) {
            throw Error("Could not set child $childNodeFactory", e)
        }
    }

    protected open fun getSource(node: Node, source: Any): Any {
        return source
    }

    protected open fun <S : Any> makeNode(factory: NodeFactory<S>, source: S, allowGenericNode: Boolean = true): Node {
        return try {
            factory.constructor(source, this, factory)
        } catch (e: Exception) {
            if (allowGenericNode) {
                GenericErrorNode(e)
            } else {
                throw e
            }
        }.withOrigin(asOrigin(source))
    }

    protected open fun <S : Any> getNodeFactory(kClass: KClass<S>): NodeFactory<S>? {
        val factory = factories[kClass]
        if (factory != null) {
            return factory as NodeFactory<S>
        } else {
            if (kClass == Any::class) {
                return null
            }
            for (superclass in kClass.superclasses) {
                val nodeFactory = getNodeFactory(superclass)
                if (nodeFactory != null) {
                    return nodeFactory as NodeFactory<S>
                }
            }
        }
        return null
    }

    fun <S : Any, T : Node> registerNodeFactory(
        kclass: KClass<S>,
        factory: (S, ASTTransformer, NodeFactory<S>) -> T
    ): NodeFactory<S> {
        val nodeFactory = NodeFactory(factory)
        factories[kclass] = nodeFactory
        return nodeFactory
    }

    fun <S : Any, T : Node> registerNodeFactory(kclass: KClass<S>, factory: (S, ASTTransformer) -> T): NodeFactory<S> =
        registerNodeFactory(kclass) { source, transformer, _ -> factory(source, transformer) }

    fun <S : Any, T : Node> registerNodeFactory(kclass: KClass<S>, factory: (S) -> T): NodeFactory<S> =
        registerNodeFactory(kclass) { input, _, _ -> factory(input) }

    fun <S : Any, T : Node> registerNodeFactory(source: KClass<S>, target: KClass<T>): NodeFactory<S> {
        registerKnownClass(target)
        val nodeFactory = NodeFactory<S>({ _, _, _ ->
            if (target.isSealed) {
                throw IllegalStateException("Unable to instantiate sealed class $target")
            }
            target.createInstance()
        })
        factories[source] = nodeFactory
        return nodeFactory
    }

    private fun registerKnownClass(target: KClass<*>) {
        val qualifiedName = target.qualifiedName
        val packageName = if (qualifiedName != null) {
            val endIndex = qualifiedName.lastIndexOf('.')
            if (endIndex >= 0) {
                qualifiedName.substring(0, endIndex)
            } else ""
        } else ""
        val set = _knownClasses.computeIfAbsent(packageName) { mutableSetOf() }
        set.add(target)
    }

    fun addIssue(message: String, severity: IssueSeverity = IssueSeverity.ERROR, position: Position? = null): Issue {
        val issue = Issue.semantic(message, severity, position)
        issues.add(issue)
        return issue
    }
}
