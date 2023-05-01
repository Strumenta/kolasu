package com.strumenta.kolasu.transformation

import com.strumenta.kolasu.model.*
import com.strumenta.kolasu.validation.Issue
import com.strumenta.kolasu.validation.IssueSeverity
import kotlin.reflect.*
import kotlin.reflect.full.*

/**
 * A child of an AST node that is automatically populated from a source tree.
 */
annotation class Mapped(val path: String = "")

/**
 * Transformer that, given a tree node, will instantiate the corresponding transformed node.
 */
class NodeTransformer<Source, Output : Node>(
    val constructor: (Source, ASTTransformer, NodeTransformer<Source, Output>) -> Output?,
    val children: MutableMap<String, ChildNodeTransformer<Source, *, *>?> = mutableMapOf(),
    var finalizer: (Output) -> Unit = {},
    var skipChildren: Boolean = false,
    var childrenSetAtConstruction: Boolean = false
) {

    /**
     * Specify how to convert a child. The value obtained from the conversion could either be used
     * as a constructor parameter when instantiating the parent, or be used to set the value after
     * the parent has been instantiated.
     *
     * Example using the scopedToType parameter:
     * ```
     *     on.registerNodeTransformer(SASParser.DatasetOptionContext::class) { ctx ->
     *         when {
     *             ...
     *         }
     *     }
     *         .withChild(SASParser.DatasetOptionContext::macroStatementStrict, ComputedDatasetOption::computedWith, ComputedDatasetOption::class)
     *         .withChild(SASParser.DatasetOptionContext::variableList, DropDatasetOption::variables, DropDatasetOption::class)
     *         .withChild(SASParser.DatasetOptionContext::variableList, KeepDatasetOption::variables, KeepDatasetOption::class)
     *         .withChild(SASParser.DatasetOptionContext::variableList, InDatasetOption::variables, InDatasetOption::class)
     *         .withChild("indexDatasetOption.variables", IndexDatasetOption::variables, IndexDatasetOption::class)
     *  ```
     *
     *  Please note that we cannot merge this method with the variant without the type (making the type optional),
     *  as it would not permit to specify the lambda outside the list of method parameters.
     */
    fun withChild(
        targetProperty: KMutableProperty1<*, *>,
        sourceAccessor: Source.() -> Any?,
        scopedToType: KClass<*>
    ): NodeTransformer<Source, Output> = withChild(
        get = { source -> source.sourceAccessor() },
        set = (targetProperty as KMutableProperty1<Any, Any?>)::set,
        targetProperty.name,
        scopedToType
    )

    /**
     * Specify how to convert a child. The value obtained from the conversion could either be used
     * as a constructor parameter when instantiating the parent, or be used to set the value after
     * the parent has been instantiated.
     */
    fun withChild(
        targetProperty: KMutableProperty1<*, *>,
        sourceAccessor: Source.() -> Any?
    ): NodeTransformer<Source, Output> = withChild(
        get = { source -> source.sourceAccessor() },
        set = (targetProperty as KMutableProperty1<Any, Any?>)::set,
        targetProperty.name,
        null
    )

    /**
     * Specify how to convert a child. The value obtained from the conversion can only be used
     * as a constructor parameter when instantiating the parent. It cannot be used to set the value after
     * the parent has been instantiated, because the property is not mutable.
     */
    fun withChild(
        targetProperty: KProperty1<*, *>,
        sourceAccessor: Source.() -> Any?
    ): NodeTransformer<Source, Output> = withChild<Any, Any>(
        get = { source -> source.sourceAccessor() },
        null,
        targetProperty.name,
        null
    )

    /**
     * Specify how to convert a child. The value obtained from the conversion can only be used
     * as a constructor parameter when instantiating the parent. It cannot be used to set the value after
     * the parent has been instantiated, because the property is not mutable.
     *
     * Please note that we cannot merge this method with the variant without the type (making the type optional),
     * as it would not permit to specify the lambda outside the list of method parameters.
     */
    fun withChild(
        targetProperty: KProperty1<*, *>,
        sourceAccessor: Source.() -> Any?,
        scopedToType: KClass<*>
    ): NodeTransformer<Source, Output> = withChild<Any, Any>(
        get = { source -> source.sourceAccessor() },
        null,
        targetProperty.name,
        scopedToType
    )

    /**
     * Specify how to convert a child. The value obtained from the conversion could either be used
     * as a constructor parameter when instantiating the parent, or be used to set the value after
     * the parent has been instantiated.
     */
    fun <Target : Any, Child : Any> withChild(
        get: (Source) -> Any?,
        set: ((Target, Child?) -> Unit)?,
        name: String,
        scopedToType: KClass<*>? = null
    ): NodeTransformer<Source, Output> {
        val prefix = if (scopedToType != null) scopedToType.qualifiedName + "#" else ""
        if (set == null) {
            // given we have no setter we MUST set the children at construction
            childrenSetAtConstruction = true
        }
        children[prefix + name] = ChildNodeTransformer(prefix + name, get, set)
        return this
    }

    fun withFinalizer(finalizer: (Output) -> Unit): NodeTransformer<Source, Output> {
        this.finalizer = finalizer
        return this
    }

    /**
     * Tells the ASTTransformer whether this NodeTransformer already takes care of the node's children and no further computation
     * is desired on that subtree. E.g., when we're mapping an ANTLR parse tree, and we have a context that is only a
     * wrapper over several alternatives, and for some reason those are not labeled alternatives in ANTLR (subclasses),
     * we may configure the transformer as follows:
     *
     * ```kotlin
     * transformer.registerNodeTransformer(XYZContext::class) { ctx -> transformer.transform(ctx.children[0]) }
     * ```
     *
     * However, if the result of `transformer.transform(ctx.children[0])` is an instance of a Node with a child
     * annotated with `@Mapped("someProperty")`, the transformer will think that it has to populate that child,
     * according to the configuration determined by reflection. When it tries to do so, the "source" of the node will
     * be an instance of `XYZContext` that does not have a child named `someProperty`, and the transformation will fail.
     */
    fun skipChildren(skip: Boolean = true): NodeTransformer<Source, Output> {
        this.skipChildren = skip
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
 *
 * The setter could be null, if the property is not mutable. In that case the value
 * must necessarily be passed when constructing the parent.
 */
data class ChildNodeTransformer<Source, Target, Child>(
    val name: String,
    val get: (Source) -> Any?,
    val setter: ((Target, Child?) -> Unit)?
) {
    fun set(node: Target, child: Child?) {
        if (setter == null) {
            throw java.lang.IllegalStateException("Unable to set value $name in  $node")
        }
        try {
            setter!!(node, child)
        } catch (e: Exception) {
            throw Exception("$name could not set child $child of $node using $setter", e)
        }
    }
}

/**
 * Sentinel value used to represent the information that a given property is not a child node.
 */
private val NO_CHILD_NODE = ChildNodeTransformer<Any, Any, Any>("", { x -> x }, { _, _ -> })

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
    val allowGenericNode: Boolean = true
) {
    /**
     * NodeTransformers that map from source tree node to target tree node.
     */
    private val nodeTransformers = mutableMapOf<KClass<*>, NodeTransformer<*, *>>()

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
        val transformer = getNodeTransformer<Any, Node>(source::class as KClass<Any>)
        val node: Node?
        if (transformer != null) {
            node = makeNode(transformer, source, allowGenericNode = allowGenericNode)
            if (node == null) {
                return null
            }
            if (!transformer.skipChildren && !transformer.childrenSetAtConstruction) {
                setChildren(transformer, source, node)
            }
            transformer.finalizer(node)
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

    private fun setChildren(
        transformer: NodeTransformer<Any, Node>,
        source: Any,
        node: Node
    ) {
        node::class.processProperties { pd ->
            val childKey = node::class.qualifiedName + "#" + pd.name
            var childNodeTransformer = transformer.getChildNodeTransformer<Node, Any, Any>(node::class, pd.name)
            if (childNodeTransformer != null) {
                if (childNodeTransformer != NO_CHILD_NODE) {
                    setChild(childNodeTransformer, source, node, pd)
                }
            } else {
                val targetProp = node::class.memberProperties.find { it.name == pd.name }
                val mapped = targetProp?.findAnnotation<Mapped>()
                if (targetProp is KMutableProperty1 && mapped != null) {
                    val path = (mapped.path.ifEmpty { targetProp.name })
                    childNodeTransformer = ChildNodeTransformer(
                        childKey,
                        transformer.getter(path),
                        (targetProp as KMutableProperty1<Any, Any?>)::set
                    )
                    transformer.children[childKey] = childNodeTransformer as ChildNodeTransformer<Any, *, *>
                    setChild(childNodeTransformer, source, node, pd)
                } else {
                    transformer.children[childKey] = NO_CHILD_NODE
                }
            }
        }
    }

    protected open fun asOrigin(source: Any): Origin? = if (source is Origin) source else null

    protected open fun setChild(
        childNodeTransformer: ChildNodeTransformer<*, *, *>,
        source: Any,
        node: Node,
        pd: PropertyTypeDescription
    ) {
        val src = (childNodeTransformer as ChildNodeTransformer<Any, Any, Any>).get(getSource(node, source))
        val child: Any? = if (pd.multiple) {
            (src as Collection<*>?)?.mapNotNull { transform(it, node) } ?: listOf<Node?>()
        } else {
            transform(src, node)
        }
        try {
            childNodeTransformer.set(node, child)
        } catch (e: IllegalArgumentException) {
            throw Error("Could not set child $childNodeTransformer", e)
        }
    }

    protected open fun getSource(node: Node, source: Any): Any {
        return source
    }

    protected open fun <S : Any, T : Node> makeNode(
        transformer: NodeTransformer<S, T>,
        source: S,
        allowGenericNode: Boolean = true
    ): Node? {
        return try {
            transformer.constructor(source, this, transformer)
        } catch (e: Exception) {
            if (allowGenericNode) {
                GenericErrorNode(e)
            } else {
                throw e
            }
        }?.withOrigin(asOrigin(source))
    }

    protected open fun <S : Any, T : Node> getNodeTransformer(kClass: KClass<S>): NodeTransformer<S, T>? {
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

    fun <S : Any, T : Node> registerNodeTransformer(
        kclass: KClass<S>,
        transformer: (S, ASTTransformer, NodeTransformer<S, T>) -> T?
    ): NodeTransformer<S, T> {
        val nodeTransformer = NodeTransformer(transformer)
        nodeTransformers[kclass] = nodeTransformer
        return nodeTransformer
    }

    fun <S : Any, T : Node> registerNodeTransformer(
        kclass: KClass<S>,
        transformer: (S, ASTTransformer) -> T?
    ): NodeTransformer<S, T> = registerNodeTransformer(kclass) { source, transformer, _ ->
        transformer(source, transformer)
    }

    inline fun <reified S : Any, T : Node> registerNodeTransformer(
        crossinline transformer: S.(ASTTransformer) -> T?
    ): NodeTransformer<S, T> = registerNodeTransformer(S::class) { source, transformer, _ ->
        source.transformer(transformer)
    }

    fun <S : Any, T : Node> registerNodeTransformer(kclass: KClass<S>, transformer: (S) -> T?): NodeTransformer<S, T> =
        registerNodeTransformer(kclass) { input, _, _ -> transformer(input) }

    inline fun <reified S : Any, reified T : Node> registerNodeTransformer(): NodeTransformer<S, T> {
        return registerNodeTransformer(S::class, T::class)
    }

    fun <S : Any, T : Node> registerNodeTransformer(
        source: KClass<S>,
        target: KClass<T>,
        parameterConverters: List<ParameterConverter> = emptyList()
    ): NodeTransformer<S, T> {
        registerKnownClass(target)
        // We are looking for any constructor with does not take parameters or have default
        // values for all its parameters
        val emptyLikeConstructor = target.constructors.find { it.parameters.all { param -> param.isOptional } }
        val nodeTransformer = NodeTransformer(
            { source: S, _, thisTransformer ->
                if (target.isSealed) {
                    throw IllegalStateException("Unable to instantiate sealed class $target")
                }
                fun getConstructorParameterValue(kParameter: KParameter): ParameterValue {
                    try {
                        val childNodeTransformer = thisTransformer.getChildNodeTransformer<Any, T, Any>(
                            target,
                            kParameter.name!!
                        )
                        if (childNodeTransformer == null) {
                            if (kParameter.isOptional) {
                                return AbsentParameterValue
                            }
                            throw java.lang.IllegalStateException(
                                "We do not know how to produce " +
                                    "parameter ${kParameter.name!!} for $target"
                            )
                        } else {
                            return when (val childSource = childNodeTransformer.get.invoke(source)) {
                                null -> {
                                    AbsentParameterValue
                                }
                                is List<*> -> {
                                    PresentParameterValue(childSource.map { transform(it) }.toMutableList())
                                }

                                is String -> {
                                    PresentParameterValue(childSource)
                                }

                                else -> {
                                    val paramConverter = parameterConverters.find {
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
                        throw RuntimeException(
                            "Issue while populating parameter ${kParameter.name} in " +
                                "constructor ${target.qualifiedName}.${target.preferredConstructor()}",
                            t
                        )
                    }
                }
                // We check `childrenSetAtConstruction` and not `emptyLikeConstructor` because, while we set this value
                // initially based on `emptyLikeConstructor` being equal to null, this can be later changed in `withChild`,
                // so we should really check the value that `childrenSetAtConstruction` time has when we actually invoke
                // the transformer.
                if (thisTransformer.childrenSetAtConstruction) {
                    val constructor = target.preferredConstructor()
                    val constructorParamValues = constructor.parameters.map { it to getConstructorParameterValue(it) }
                        .filter { it.second is PresentParameterValue }
                        .associate { it.first to (it.second as PresentParameterValue).value }
                    try {
                        val instance = constructor.callBy(constructorParamValues)
                        instance.children.forEach { child -> child.parent = instance }
                        instance
                    } catch (t: Throwable) {
                        throw RuntimeException(
                            "Invocation of constructor $constructor failed. " +
                                "We passed: ${constructorParamValues.map { "${it.key.name}=${it.value}" }
                                    .joinToString(", ")}",
                            t
                        )
                    }
                } else {
                    if (emptyLikeConstructor == null) {
                        throw RuntimeException(
                            "childrenSetAtConstruction is set but there is no empty like " +
                                "constructor for $target"
                        )
                    }
                    target.createInstance()
                }
            },
            // If I do not have an emptyLikeConstructor, then I am forced to invoke a constructor with parameters and
            // therefore setting the children at construction time.
            // Note that we are assuming that either we set no children at construction time or we set all of them
            childrenSetAtConstruction = emptyLikeConstructor == null
        )
        nodeTransformers[source] = nodeTransformer
        return nodeTransformer
    }

    fun <T : Node> registerIdentityTransformation(nodeClass: KClass<T>) =
        registerNodeTransformer(nodeClass) { node -> node }.skipChildren()

    private fun registerKnownClass(target: KClass<*>) {
        val qualifiedName = target.qualifiedName
        val packageName = if (qualifiedName != null) {
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

    fun addIssue(message: String, severity: IssueSeverity = IssueSeverity.ERROR, position: Position? = null): Issue {
        val issue = Issue.semantic(message, severity, position)
        issues.add(issue)
        return issue
    }
}

private fun <Source : Any, Target, Child> NodeTransformer<*, *>.getChildNodeTransformer(
    nodeClass: KClass<out Source>,
    parameterName: String
): ChildNodeTransformer<Source, Target, Child>? {
    val childKey = nodeClass.qualifiedName + "#" + parameterName
    var childNodeTransformer = this.children[childKey]
    if (childNodeTransformer == null) {
        childNodeTransformer = this.children[parameterName]
    }
    return childNodeTransformer as ChildNodeTransformer<Source, Target, Child>?
}

private sealed class ParameterValue
private class PresentParameterValue(val value: Any?) : ParameterValue()
private object AbsentParameterValue : ParameterValue()

inline fun <T : Any> KClass<T>.preferredConstructor(): KFunction<T> {
    val constructors = this.constructors
    return if (constructors.size != 1) {
        if (this.primaryConstructor != null) {
            this.primaryConstructor!!
        } else {
            throw RuntimeException(
                "Node Factories support only classes with exactly one constructor or a " +
                    "primary constructor. Class ${this.qualifiedName} has ${constructors.size}"
            )
        }
    } else {
        constructors.first()
    }
}

interface ParameterConverter {
    fun isApplicable(kParameter: KParameter, value: Any?): Boolean
    fun convert(kParameter: KParameter, value: Any?): Any?
}
