package com.strumenta.kolasu.transformation

import com.strumenta.kolasu.model.GenericErrorNode
import com.strumenta.kolasu.model.NodeLike
import com.strumenta.kolasu.model.NodeOrigin
import com.strumenta.kolasu.model.Origin
import com.strumenta.kolasu.model.PropertyTypeDescription
import com.strumenta.kolasu.model.Range
import com.strumenta.kolasu.model.children
import com.strumenta.kolasu.model.processFeatures
import com.strumenta.kolasu.model.withOrigin
import com.strumenta.kolasu.validation.Issue
import com.strumenta.kolasu.validation.IssueSeverity
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.KParameter
import kotlin.reflect.full.createInstance
import kotlin.reflect.full.primaryConstructor
import kotlin.reflect.full.superclasses

/**
 * Implementation of a tree-to-tree transformation. For each source node type, we can register a transformer that knows
 * how to produce a transformed node. Then, this transformer can read metadata in the transformed node to recursively
 * transform and assign children.
 * If no node transformer is provided for a source node type, a GenericNode is created, and the processing of the
 * subtree stops there.
 */
open class ASTTransformer(
    /**
     * Additional issues found during the transformation process.
     */
    val issues: MutableList<Issue> = mutableListOf(),
    val allowGenericNode: Boolean = true,
) {
    /**
     * NodeTransformers that map from source tree node to target tree node.
     */
    private val nodeTransformers = mutableMapOf<KClass<*>, NodeTransformer<*, *>>()

    private val _knownClasses = mutableMapOf<String, MutableSet<KClass<*>>>()
    val knownClasses: Map<String, Set<KClass<*>>> = _knownClasses

    /**
     * This ensures that the generated value is a single Node or null.
     */
    @JvmOverloads
    fun transform(
        source: Any?,
        parent: NodeLike? = null,
    ): NodeLike? {
        val result = transformIntoNodes(source, parent)
        return when (result.size) {
            0 -> null
            1 -> {
                val node = result.first()
                require(node is NodeLike)
                node
            }

            else -> throw IllegalStateException(
                "Cannot transform into a single Node as multiple nodes where produced",
            )
        }
    }

    /**
     * Performs the transformation of a node and, recursively, its descendants.
     */
    @JvmOverloads
    open fun transformIntoNodes(
        source: Any?,
        parent: NodeLike? = null,
    ): List<NodeLike> {
        if (source == null) {
            return emptyList()
        }
        if (source is Collection<*>) {
            throw Error("Mapping error: received collection when value was expected")
        }
        val transformer = getNodeTransformer<Any, NodeLike>(source::class as KClass<Any>)
        val nodes: List<NodeLike>
        if (transformer != null) {
            nodes = makeNodes(transformer, source, allowGenericNode = allowGenericNode)
            if (!transformer.skipChildren && !transformer.childrenSetAtConstruction) {
                nodes.forEach { node -> setChildren(transformer, source, node) }
            }
            nodes.forEach { node ->
                transformer.finalizer(node)
                node.parent = parent
            }
        } else {
            if (allowGenericNode) {
                val origin = asOrigin(source)
                nodes = listOf(GenericNode(parent).withOrigin(origin))
                issues.add(
                    Issue.semantic(
                        "Source node not mapped: ${source::class.qualifiedName}",
                        IssueSeverity.WARNING,
                        origin?.range,
                    ),
                )
            } else {
                throw IllegalStateException("Unable to translate node $source (class ${source.javaClass})")
            }
        }
        return nodes
    }

    private fun setChildren(
        transformer: NodeTransformer<Any, NodeLike>,
        source: Any,
        node: NodeLike,
    ) {
        node::class.processFeatures { pd ->
            val childNodeTransformer = transformer.getChildNodeTransformer<Any, NodeLike, Any>(node::class, pd.name)
            if (childNodeTransformer != null) {
                if (childNodeTransformer != NO_CHILD_NODE) {
                    setChild(childNodeTransformer, source, node, pd)
                }
            } else {
                val childKey = node::class.qualifiedName + "#" + pd.name
                transformer.children[childKey] = NO_CHILD_NODE
            }
        }
    }

    protected open fun asOrigin(source: Any): Origin? = if (source is Origin) source else null

    protected open fun setChild(
        childNodeTransformer: ChildNodeTransformer<*, *, *>,
        source: Any,
        node: NodeLike,
        pd: PropertyTypeDescription,
    ) {
        val childFactory = childNodeTransformer as ChildNodeTransformer<Any, Any, Any>
        val childrenSource = childFactory.get(getSource(node, source))
        val child: Any? =
            if (pd.multiple) {
                (childrenSource as List<*>).map { transformIntoNodes(it, node) }.flatten() ?: listOf<NodeLike>()
            } else {
                transform(childrenSource, node)
            }
        try {
            childNodeTransformer.set(node, child)
        } catch (e: IllegalArgumentException) {
            throw Error("Could not set child $childNodeTransformer", e)
        }
    }

    protected open fun getSource(
        node: NodeLike,
        source: Any,
    ): Any {
        return source
    }

    protected open fun <S : Any, T : NodeLike> makeNodes(
        transformer: NodeTransformer<S, T>,
        source: S,
        allowGenericNode: Boolean = true,
    ): List<NodeLike> {
        val nodes =
            try {
                transformer.constructorToUse(source, this, transformer)
            } catch (e: Exception) {
                if (allowGenericNode) {
                    listOf(GenericErrorNode(e))
                } else {
                    throw e
                }
            }
        nodes.forEach { node ->
            if (node.origin == null) {
                node.withOrigin(asOrigin(source))
            }
        }
        return nodes
    }

    protected open fun <S : Any, T : NodeLike> getNodeTransformer(kClass: KClass<S>): NodeTransformer<S, T>? {
        val nodeTransformer = nodeTransformers[kClass]
        if (nodeTransformer != null) {
            return nodeTransformer as NodeTransformer<S, T>
        } else {
            if (kClass == Any::class) {
                return null
            }
            for (superclass in kClass.superclasses) {
                val superClassNodeTransformer = getNodeTransformer<S, T>(superclass as KClass<S>)
                if (superClassNodeTransformer != null) {
                    return superClassNodeTransformer
                }
            }
        }
        return null
    }

    fun <S : Any, T : NodeLike> registerNodeTransformer(
        kclass: KClass<S>,
        transformer: (S, ASTTransformer, NodeTransformer<S, T>) -> T?,
    ): NodeTransformer<S, T> {
        val nodeTransformer = NodeTransformer.single(transformer)
        nodeTransformers[kclass] = nodeTransformer
        return nodeTransformer
    }

    fun <S : Any, T : NodeLike> registerMultipleNodeTransformer(
        kclass: KClass<S>,
        transformer: (S, ASTTransformer, NodeTransformer<S, T>) -> List<T>,
    ): NodeTransformer<S, T> {
        val nodeTransformer = NodeTransformer(transformer)
        nodeTransformers[kclass] = nodeTransformer
        return nodeTransformer
    }

    fun <S : Any, T : NodeLike> registerNodeTransformer(
        kclass: KClass<S>,
        transformer: (S, ASTTransformer) -> T?,
    ): NodeTransformer<S, T> =
        registerNodeTransformer(kclass) { source, transformer, _ ->
            transformer(
                source,
                transformer,
            )
        }

    fun <S : Any, T : NodeLike> registerMultipleNodeTransformer(
        kclass: KClass<S>,
        factory: (S) -> List<T>,
    ): NodeTransformer<S, T> = registerMultipleNodeTransformer(kclass) { input, _, _ -> factory(input) }

    inline fun <reified S : Any, reified T : NodeLike> registerNodeTransformer(): NodeTransformer<S, T> {
        return registerNodeTransformer(S::class, T::class)
    }

    inline fun <reified S : Any, T : NodeLike> registerNodeTransformer(
        crossinline transformer: S.(ASTTransformer) -> T?,
    ): NodeTransformer<S, T> =
        registerNodeTransformer(S::class) { source, transformer, _ ->
            source.transformer(transformer)
        }

    fun <S : Any, T : NodeLike> registerNodeTransformer(
        kclass: KClass<S>,
        transformer: (S) -> T?,
    ): NodeTransformer<S, T> = registerNodeTransformer(kclass) { input, _, _ -> transformer(input) }

    /**
     * Define the origin of the node as the source, but only if source is a Node, otherwise
     * this method does not do anything.
     */
    private fun <N : NodeLike> N.settingNodeOrigin(source: Any): N {
        if (source is NodeLike) {
            this.origin = NodeOrigin(source)
        }
        return this
    }

    inline fun <reified S : Any> notTranslateDirectly(): NodeTransformer<S, NodeLike> =
        registerNodeTransformer<S, NodeLike> {
            throw java.lang.IllegalStateException(
                "A Node of this type (${this.javaClass.canonicalName}) should never be translated directly. " +
                    "It is expected that the container will not delegate the translation of this node but it will " +
                    "handle it directly",
            )
        }

    fun <S : Any, T : NodeLike> registerNodeTransformer(
        source: KClass<S>,
        target: KClass<T>,
        parameterConverters: List<ParameterConverter> = emptyList(),
    ): NodeTransformer<S, T> {
        registerKnownClass(target)
        // We are looking for any constructor with does not take parameters or have default
        // values for all its parameters
        val emptyLikeConstructor = target.constructors.find { it.parameters.all { param -> param.isOptional } }
        val nodeTransformer =
            NodeTransformer.single(
                { source: S, _, thisTransformer ->
                    if (target.isSealed) {
                        throw IllegalStateException("Unable to instantiate sealed class $target")
                    }

                    fun getConstructorParameterValue(kParameter: KParameter): ParameterValue {
                        try {
                            val childNodeTransformer =
                                thisTransformer.getChildNodeTransformer<Any, T, Any>(
                                    target,
                                    kParameter.name!!,
                                )
                            if (childNodeTransformer == null) {
                                if (kParameter.isOptional) {
                                    return AbsentParameterValue
                                }
                                // see https://youtrack.jetbrains.com/issue/KT-65341
                                val paramName = kParameter.name!!
                                throw java.lang.IllegalStateException(
                                    "We do not know how to produce " +
                                        "parameter $paramName for $target",
                                )
                            } else {
                                return when (val childSource = childNodeTransformer.get.invoke(source)) {
                                    null -> {
                                        AbsentParameterValue
                                    }

                                    is List<*> -> {
                                        PresentParameterValue(
                                            childSource
                                                .map { transformIntoNodes(it) }
                                                .flatten()
                                                .toMutableList(),
                                        )
                                    }

                                    is String -> {
                                        PresentParameterValue(childSource)
                                    }

                                    else -> {
                                        val paramConverter =
                                            parameterConverters.find {
                                                it.isApplicable(kParameter, childSource)
                                            }
                                        if (paramConverter != null) {
                                            PresentParameterValue(paramConverter.convert(kParameter, childSource))
                                        } else {
                                            PresentParameterValue(transform(childSource))
                                        }
                                    }
                                }
                            }
                        } catch (t: Throwable) {
                            // See https://youtrack.jetbrains.com/issue/KT-65341
                            val paramName = kParameter.name!!
                            throw RuntimeException(
                                "Issue while populating parameter $paramName in " +
                                    "constructor ${target.qualifiedName}.${target.pickConstructor()}",
                                t,
                            )
                        }
                    }
                    // We check `childrenSetAtConstruction` and not `emptyLikeConstructor` because, while we set this value
                    // initially based on `emptyLikeConstructor` being equal to null, this can be later changed in
                    // `withChild`, so we should really check the value that `childrenSetAtConstruction` time has when
                    // we actually invoke
                    // the transformer.
                    if (thisTransformer.childrenSetAtConstruction) {
                        val constructor = target.pickConstructor()
                        val constructorParamValues =
                            constructor
                                .parameters
                                .map { it to getConstructorParameterValue(it) }
                                .filter { it.second is PresentParameterValue }
                                .associate { it.first to (it.second as PresentParameterValue).value }
                        try {
                            val instance = constructor.callBy(constructorParamValues)
                            instance.children.forEach { child -> child.parent = instance }
                            instance.settingNodeOrigin(source)
                        } catch (t: Throwable) {
                            throw RuntimeException(
                                "Invocation of constructor $constructor failed. " +
                                    "We passed: ${
                                        constructorParamValues.map { "${it.key.name}=${it.value}" }
                                            .joinToString(", ")
                                    }",
                                t,
                            )
                        }
                    } else {
                        if (emptyLikeConstructor == null) {
                            throw RuntimeException(
                                "childrenSetAtConstruction is set but there is no empty like " +
                                    "constructor for $target",
                            )
                        }
                        target.createInstance().settingNodeOrigin(source)
                    }
                },
                // If I do not have an emptyLikeConstructor, then I am forced to invoke a constructor with parameters and
                // therefore setting the children at construction time.
                // Note that we are assuming that either we set no children at construction time or we set all of them
                childrenSetAtConstruction = emptyLikeConstructor == null,
            )
        nodeTransformers[source] = nodeTransformer
        return nodeTransformer
    }

    fun <T : NodeLike> registerIdentityTransformation(nodeClass: KClass<T>) =
        registerNodeTransformer(nodeClass) { node -> node }.skipChildren()

    private fun registerKnownClass(target: KClass<*>) {
        val qualifiedName = target.qualifiedName
        val packageName =
            if (qualifiedName != null) {
                val endIndex = qualifiedName.lastIndexOf('.')
                if (endIndex >= 0) {
                    qualifiedName.substring(0, endIndex)
                } else {
                    ""
                }
            } else {
                ""
            }
        val set = _knownClasses.computeIfAbsent(packageName) { mutableSetOf() }
        set.add(target)
    }

    fun addIssue(
        message: String,
        severity: IssueSeverity = IssueSeverity.ERROR,
        range: Range? = null,
    ): Issue {
        val issue = Issue.semantic(message, severity, range)
        issues.add(issue)
        return issue
    }
}

fun <T : Any> KClass<T>.pickConstructor(): KFunction<T> {
    val constructors = this.constructors
    return if (constructors.size != 1) {
        if (this.primaryConstructor != null) {
            this.primaryConstructor!!
        } else {
            throw RuntimeException(
                "Node Factories support only classes with exactly one constructor or a " +
                    "primary constructor. Class ${this.qualifiedName} has ${constructors.size}",
            )
        }
    } else {
        constructors.first()
    }
}
