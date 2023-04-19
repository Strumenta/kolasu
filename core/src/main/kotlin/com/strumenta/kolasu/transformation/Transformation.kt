package com.strumenta.kolasu.transformation

import com.strumenta.kolasu.model.*
import com.strumenta.kolasu.validation.Issue
import com.strumenta.kolasu.validation.IssueSeverity
import org.antlr.v4.runtime.ParserRuleContext
import org.antlr.v4.runtime.tree.ParseTree
import kotlin.reflect.KClass
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.KParameter
import kotlin.reflect.KProperty1
import kotlin.reflect.full.*

/**
 * A child of an AST node that is automatically populated from a source tree.
 */
annotation class Mapped(val path: String = "")

/**
 * Factory that, given a tree node, will instantiate the corresponding transformed node.
 */
class NodeFactory<Source, Output : Node>(
    val constructor: (Source, ASTTransformer, NodeFactory<Source, Output>) -> Output?,
    val children: MutableMap<String, ChildNodeFactory<Source, *, *>?> = mutableMapOf(),
    var finalizer: (Output) -> Unit = {},
    var skipChildren: Boolean = false,
    var childrenSetAtConstruction: Boolean = false
) {

    fun withChildFrom(
        property: KMutableProperty1<*, *>,
        sourceAccessor: Source.() -> Any?,
    ): NodeFactory<Source, Output> = withChild(
        { source: Source ->
            source.sourceAccessor()
        },
        (property as KMutableProperty1<Any, Any?>)::set,
        property.name,
        null
    )

    fun withChildFrom(
        property: KProperty1<*, *>,
        sourceAccessor: Source.() -> Any?,
    ): NodeFactory<Source, Output> = withChild<Any, Any>(
        { source: Source ->
            source.sourceAccessor()
        },
        null,
        property.name,
        null
    )

    fun withChild(
        sourceProperty: KProperty1<Source, *>,
        property: KMutableProperty1<*, *>,
        type: KClass<*>? = null
    ): NodeFactory<Source, Output> = withChild(
        (sourceProperty as KProperty1<Source, Any>)::get,
        (property as KMutableProperty1<Any, Any?>)::set,
        property.name,
        type
    )

    /**
     * ```
     *     on.registerNodeFactory(SASParser.DatasetOptionContext::class) { ctx ->
     *         when {
     *             ctx.DROP() != null -> DropDatasetOption()
     *             ctx.EXTENDOBSCOUNTER() != null -> ExtendObsCounterDatasetOption(ctx.YES() != null)
     *             ctx.gennum() != null -> GenNumDatasetOption(translateGennum(ctx.gennum()))
     *             ctx.LABEL() != null -> LabelDatasetOption(ctx.stringLiteral().asString())
     *             ctx.KEEP_VARS() != null -> KeepDatasetOption()
     *             ctx.indexDatasetOption() != null ->
     *                 IndexDatasetOption(ctx.indexDatasetOption().name?.text)
     *             ctx.IN_VARS() != null -> InDatasetOption()
     *             ctx.RENAME_VARS() != null -> RenameDatasetOption(ctx.datasetRename().map {
     *                 Rename(it.identifier(0).text, it.identifier(1).text).withParseTreeNode(it)
     *             })
     *             ctx.WHERE() != null -> WhereDatasetOption()
     *             ctx.macroStatementStrict() != null -> ComputedDatasetOption()
     *             ctx.macroFunctionCall() != null -> {
     *                 val option = ComputedDatasetOption()
     *                 option.computedWith = ExpressionStatement(on.transform(ctx.macroFunctionCall(), option) as Expression)
     *                 option
     *             }
     *             ctx.MACRO_VARIABLE() != null -> {
     *                 val option = ComputedDatasetOption()
     *                 option.computedWith = ExpressionStatement(VariableExpression(ctx.MACRO_VARIABLE().text.substring(1)))
     *                 option
     *             }
     *             //TODO refine macroLiteral
     *             else -> GenericDatasetOption(ctx.name.text, ctx.macroLiteral()?.text ?: ctx.identifier(1)?.text)
     *         }
     *     }
     *         .withChild(SASParser.DatasetOptionContext::macroStatementStrict, ComputedDatasetOption::computedWith, ComputedDatasetOption::class)
     *         .withChild(SASParser.DatasetOptionContext::variableList, DropDatasetOption::variables, DropDatasetOption::class)
     *         .withChild(SASParser.DatasetOptionContext::variableList, KeepDatasetOption::variables, KeepDatasetOption::class)
     *         .withChild(SASParser.DatasetOptionContext::variableList, InDatasetOption::variables, InDatasetOption::class)
     *         .withChild("indexDatasetOption.variables", IndexDatasetOption::variables, IndexDatasetOption::class)
     *  ```
     */
    fun <Target : Any> withChild(
        path: String,
        property: KMutableProperty1<Target, *>,
        scopedToType: KClass<Target>? = null
    ): NodeFactory<Source, Output> =
        withChild(getter(path), (property as KMutableProperty1<Any, Any?>)::set, property.name, scopedToType)

    fun <Target : Any> withChild(
        get: (Source) -> Any?,
        property: KMutableProperty1<in Target, *>,
        scopedToType: KClass<Target>? = null
    ): NodeFactory<Source, Output> =
        withChild(get, (property as KMutableProperty1<Target, Any?>)::set, property.name, scopedToType)

    fun <Target : Any, Child : Any> withChild(
        get: (Source) -> Any?,
        set: ((Target, Child?) -> Unit)?,
        name: String,
        type: KClass<*>? = null
    ): NodeFactory<Source, Output> {
        val prefix = if (type != null) type.qualifiedName + "#" else ""
        if (set == null) {
            // given we have no setter we MUST set the children at construction
            childrenSetAtConstruction = true
        }
        children[prefix + name] = ChildNodeFactory(prefix + name, get, set)
        return this
    }

    fun withFinalizer(finalizer: (Output) -> Unit): NodeFactory<Source, Output> {
        this.finalizer = finalizer
        return this
    }

    /**
     * Tells the transformer whether this factory already takes care of the node's children and no further computation
     * is desired on that subtree. E.g., when we're mapping an ANTLR parse tree, and we have a context that is only a
     * wrapper over several alternatives, and for some reason those are not labeled alternatives in ANTLR (subclasses),
     * we may configure the transformer as follows:
     *
     * ```kotlin
     * transformer.registerNodeFactory(XYZContext::class) { ctx -> transformer.transform(ctx.children[0]) }
     * ```
     *
     * However, if the result of `transformer.transform(ctx.children[0])` is an instance of a Node with a child
     * annotated with `@Mapped("someProperty")`, the transformer will think that it has to populate that child,
     * according to the configuration determined by reflection. When it tries to do so, the "source" of the node will
     * be an instance of `XYZContext` that does not have a child named `someProperty`, and the transformation will fail.
     */
    fun skipChildren(skip: Boolean = true): NodeFactory<Source, Output> {
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
 */
data class ChildNodeFactory<Source, Target, Child>(
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
    val factories = mutableMapOf<KClass<*>, NodeFactory<*, *>>()

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
        val factory = getNodeFactory<Any, Node>(source::class as KClass<Any>)
        val node: Node?
        if (factory != null) {
            node = makeNode(factory, source, allowGenericNode = allowGenericNode)
            if (node == null) {
                return null
            }
            if (!factory.skipChildren && !factory.childrenSetAtConstruction) {
                setChildren(factory, source, node)
            }
            factory.finalizer(node)
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
        factory: NodeFactory<Any, Node>,
        source: Any,
        node: Node
    ) {
        node::class.processProperties { pd ->
            val childKey = node::class.qualifiedName + "#" + pd.name
            var childNodeFactory = factory.getChildNodeFactory<Node, Any, Any>(node::class, pd.name)
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
                    factory.children[childKey] = childNodeFactory as ChildNodeFactory<Any, *, *>
                    setChild(childNodeFactory, source, node, pd)
                } else {
                    factory.children[childKey] = NO_CHILD_NODE
                }
            }
        }
    }

    protected open fun asOrigin(source: Any): Origin? = if (source is Origin) source else null

    protected open fun setChild(
        childNodeFactory: ChildNodeFactory<*, *, *>,
        source: Any,
        node: Node,
        pd: PropertyTypeDescription
    ) {
        val src = (childNodeFactory as ChildNodeFactory<Any, Any, Any>).get(getSource(node, source))
        val child: Any? = if (pd.multiple) {
            (src as Collection<*>?)?.mapNotNull { transform(it, node) } ?: listOf<Node?>()
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

    protected open fun <S : Any, T : Node> makeNode(
        factory: NodeFactory<S, T>,
        source: S,
        allowGenericNode: Boolean = true
    ): Node? {
        return try {
            factory.constructor(source, this, factory)
        } catch (e: Exception) {
            if (allowGenericNode) {
                GenericErrorNode(e)
            } else {
                throw e
            }
        }?.withOrigin(asOrigin(source))
    }

    protected open fun <S : Any, T : Node> getNodeFactory(kClass: KClass<S>): NodeFactory<S, T>? {
        val factory = factories[kClass]
        if (factory != null) {
            return factory as NodeFactory<S, T>
        } else {
            if (kClass == Any::class) {
                return null
            }
            for (superclass in kClass.superclasses) {
                val nodeFactory = getNodeFactory<S, T>(superclass as KClass<S>)
                if (nodeFactory != null) {
                    return nodeFactory
                }
            }
        }
        return null
    }

    fun <S : Any, T : Node> registerNodeFactory(
        kclass: KClass<S>,
        factory: (S, ASTTransformer, NodeFactory<S, T>) -> T?
    ): NodeFactory<S, T> {
        val nodeFactory = NodeFactory(factory)
        factories[kclass] = nodeFactory
        return nodeFactory
    }

    fun <S : Any, T : Node> registerNodeFactory(
        kclass: KClass<S>,
        factory: (S, ASTTransformer) -> T?
    ): NodeFactory<S, T> = registerNodeFactory(kclass) { source, transformer, _ -> factory(source, transformer) }

    inline fun <reified S : Any, T : Node> registerNodeFactory(
        crossinline factory: S.(ASTTransformer) -> T?
    ): NodeFactory<S, T> = registerNodeFactory(S::class) { source, transformer, _ -> source.factory(transformer) }

    fun <S : Any, T : Node> registerNodeFactory(kclass: KClass<S>, factory: (S) -> T?): NodeFactory<S, T> =
        registerNodeFactory(kclass) { input, _, _ -> factory(input) }

    inline fun <reified S : Any, reified T : Node> registerNodeFactory(): NodeFactory<S, T> {
        return registerNodeFactory(S::class, T::class)
    }

    fun <S : Any, T : Node> registerNodeFactory(source: KClass<S>, target: KClass<T>): NodeFactory<S, T> {
        registerKnownClass(target)
        // We are looking for any constructor with does not take parameters or have default
        // values for all its parameters
        val emptyConstructor = target.constructors.find { it.parameters.all { param -> param.isOptional } }
        val nodeFactory = NodeFactory({ source: S, _, f ->
            if (target.isSealed) {
                throw IllegalStateException("Unable to instantiate sealed class $target")
            }
            fun hasConstructorParameterValue(kParameter: KParameter): Boolean {
                return f.getChildNodeFactory<Any, T, Any>(target, kParameter.name!!) != null || !kParameter.isOptional
            }
            fun getConstructorParameterValue(kParameter: KParameter): Any? {
                try {
                    val childNodeFactory = f.getChildNodeFactory<Any, T, Any>(target, kParameter.name!!)
                    if (childNodeFactory == null) {
                        throw java.lang.IllegalStateException(
                            "We do not know how to produce " +
                                "parameter ${kParameter.name!!} for $target"
                        )
                    } else {
                        val childSource = childNodeFactory.get.invoke(source)
                        return when (childSource) {
                            is List<*> -> {
                                childSource.map { transform(it) }.toMutableList()
                            }

                            is String -> {
                                childSource
                            }

                            else -> {
                                if (kParameter.type == String::class.createType() && childSource is ParseTree) {
                                    childSource.text
                                } else {
                                    transform(childSource)
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
            if (emptyConstructor == null) {
                val constructor = target.preferredConstructor()
                val constructorParamValues = constructor.parameters.filter { hasConstructorParameterValue(it) }
                    .associateWith { getConstructorParameterValue(it) }
                constructor.callBy(constructorParamValues)
            } else {
                target.createInstance()
            }
        }, childrenSetAtConstruction = emptyConstructor == null)
        factories[source] = nodeFactory
        return nodeFactory
    }

    fun <T : Node> registerIdentityTransformation(nodeClass: KClass<T>) =
        registerNodeFactory(nodeClass) { node -> node }.skipChildren()

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

fun <Source : Any, Target, Child> NodeFactory<*, *>.getChildNodeFactory(
    nodeClass: KClass<out Source>,
    parameterName: String
): ChildNodeFactory<Source, Target, Child>? {
    val childKey = nodeClass.qualifiedName + "#" + parameterName
    var childNodeFactory = this.children[childKey]
    if (childNodeFactory == null) {
        childNodeFactory = this.children[parameterName]
    }
    return childNodeFactory as ChildNodeFactory<Source, Target, Child>?
}
