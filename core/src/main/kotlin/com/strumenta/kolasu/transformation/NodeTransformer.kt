package com.strumenta.kolasu.transformation

import com.strumenta.kolasu.model.NodeLike
import kotlin.reflect.KClass
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.KProperty1
import kotlin.reflect.full.memberFunctions
import kotlin.reflect.full.memberProperties

/**
 * Transformer that, given a tree node, will instantiate the corresponding transformed node.
 */
class NodeTransformer<Source, Output : NodeLike>(
    val constructorToUse: (Source, ASTTransformer, NodeTransformer<Source, Output>) -> List<Output>,
    var children: MutableMap<String, ChildNodeTransformer<Source, *, *>?> = mutableMapOf(),
    var finalizer: (Output) -> Unit = {},
    var skipChildren: Boolean = false,
    var childrenSetAtConstruction: Boolean = false,
) {
    companion object {
        fun <Source, Output : NodeLike> single(
            singleConstructor: (Source, ASTTransformer, NodeTransformer<Source, Output>) -> Output?,
            children: MutableMap<String, ChildNodeTransformer<Source, *, *>?> = mutableMapOf(),
            finalizer: (Output) -> Unit = {},
            skipChildren: Boolean = false,
            childrenSetAtConstruction: Boolean = false,
        ): NodeTransformer<Source, Output> {
            return NodeTransformer({ source, at, nf ->
                val result = singleConstructor(source, at, nf)
                if (result == null) emptyList() else listOf(result)
            }, children, finalizer, skipChildren, childrenSetAtConstruction)
        }
    }

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
        scopedToType: KClass<*>,
    ): NodeTransformer<Source, Output> =
        withChild(
            get = { source -> source.sourceAccessor() },
            set = (targetProperty as KMutableProperty1<Any, Any?>)::set,
            targetProperty.name,
            scopedToType,
        )

    /**
     * Specify how to convert a child. The value obtained from the conversion could either be used
     * as a constructor parameter when instantiating the parent, or be used to set the value after
     * the parent has been instantiated.
     */
    fun withChild(
        targetProperty: KMutableProperty1<*, *>,
        sourceAccessor: Source.() -> Any?,
    ): NodeTransformer<Source, Output> =
        withChild(
            get = { source -> source.sourceAccessor() },
            set = (targetProperty as KMutableProperty1<Any, Any?>)::set,
            targetProperty.name,
            null,
        )

    /**
     * Specify how to convert a child. The value obtained from the conversion can only be used
     * as a constructor parameter when instantiating the parent. It cannot be used to set the value after
     * the parent has been instantiated, because the property is not mutable.
     */
    fun withChild(
        targetProperty: KProperty1<*, *>,
        sourceAccessor: Source.() -> Any?,
    ): NodeTransformer<Source, Output> =
        withChild<Any, Any>(
            get = { source -> source.sourceAccessor() },
            null,
            targetProperty.name,
            null,
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
        scopedToType: KClass<*>,
    ): NodeTransformer<Source, Output> =
        withChild<Any, Any>(
            get = { source -> source.sourceAccessor() },
            null,
            targetProperty.name,
            scopedToType,
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
        scopedToType: KClass<*>? = null,
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

    fun getter(path: String) =
        { src: Source ->
            var sub: Any? = src
            for (elem in path.split('.')) {
                if (sub == null) {
                    break
                }
                sub = getSubExpression(sub, elem)
            }
            sub
        }

    private fun getSubExpression(
        src: Any,
        elem: String,
    ): Any? =
        if (src is Collection<*>) {
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
