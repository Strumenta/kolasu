package com.strumenta.kolasu.transformation

import com.strumenta.kolasu.language.Concept
import com.strumenta.kolasu.language.ConceptLike
import com.strumenta.kolasu.model.NodeLike
import com.strumenta.kolasu.model.Range
import com.strumenta.kolasu.validation.Issue
import com.strumenta.kolasu.validation.IssueSeverity
import kotlin.jvm.JvmOverloads
import kotlin.reflect.KClass

val a = 1

//
// //import com.strumenta.kolasu.model.GenericErrorNode
// import com.strumenta.kolasu.model.FeatureDescription
// import com.strumenta.kolasu.model.NodeLike
// import com.strumenta.kolasu.model.NodeOrigin
// import com.strumenta.kolasu.model.Origin
// //import com.strumenta.kolasu.model.PropertyTypeDescription
// import com.strumenta.kolasu.model.Range
// import com.strumenta.kolasu.model.withOrigin
// //import com.strumenta.kolasu.model.children
// //import com.strumenta.kolasu.model.processProperties
// //import com.strumenta.kolasu.model.withOrigin
// import com.strumenta.kolasu.validation.Issue
// import com.strumenta.kolasu.validation.IssueSeverity
// import kotlin.jvm.JvmOverloads
// import kotlin.reflect.KClass
//
// //import kotlin.reflect.KParameter
// //import kotlin.reflect.full.createInstance
// //import kotlin.reflect.full.findAnnotation
// //import kotlin.reflect.full.memberProperties
// //import kotlin.reflect.full.superclasses
//
 /**
 * Implementation of a tree-to-tree transformation. For each source node type, we can register a transformer that knows
 * how to produce a transformed node. Then, this transformer can read metadata in the transformed node to recursively
 * transform and assign children.
 * If no node transformer is provided for a source node type, a GenericNode is created, and the processing of the
 * subtree stops there.
 */
 open class MPASTTransformer(
    /**
     * Additional issues found during the transformation process.
     */
    val issues: MutableList<Issue> = mutableListOf(),
 ) {
    /**
     * NodeTransformers that map from source tree node to target tree node.
     */
    private val nodeTransformers = mutableMapOf<KClass<*>, MPNodeTransformer<*, *>>()
//
//    private val _knownClasses = mutableMapOf<String, MutableSet<KClass<*>>>()
//    val knownClasses: Map<String, Set<KClass<*>>> = _knownClasses
//
    /**
     * This ensures that the generated value is a single Node or null.
     */
    @JvmOverloads
    fun <S:Any, T: NodeLike>transform(
        source: S?,
        parent: NodeLike? = null,
    ): NodeLike? {
        val result = transformIntoNodes<S>(source, parent)
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
    protected inline fun <reified S: Any>transformIntoNodes(
        source: S?,
        parent: NodeLike? = null,
    ): List<NodeLike> {
        if (source == null) {
            return emptyList()
        }
        if (source is Collection<*>) {
            throw Error("Mapping error: received collection when value was expected")
        }
        val nodes: List<NodeLike>

        val transformer = getNodeTransformer<S, NodeLike>(S::class)
        if (transformer != null) {
            nodes = makeNodes(transformer, source)
            if (!transformer.skipChildren && !transformer.childrenSetAtConstruction) {
                nodes.forEach { node -> setChildren(transformer as MPNodeTransformer<Any, NodeLike>, source, node) }
            }
            nodes.forEach { node ->
                transformer.finalizer(node)
                node.parent = parent
            }
             return nodes
        } else {
            throw IllegalStateException("Unable to translate node $source")
        }
    }

    protected fun setChildren(
        transformer: MPNodeTransformer<Any, NodeLike>,
        source: Any,
        node: NodeLike,
    ) {
        TODO()
//        node.originalFeatures.forEach { feature ->
//            val childKey = node::class.qualifiedName + "#" + feature.name
//            var childNodeTransformer = transformer.getChildNodeTransformer<NodeLike, Any, Any>(node::class, feature.name)
//            if (childNodeTransformer != null) {
//                if (childNodeTransformer != NO_CHILD_NODE) {
//                    setChild(childNodeTransformer, source, node, feature)
//                }
//            } else {
                // MAPPED WILL NOT BE SUPPORTED FOR NOW
 //val targetProp = node::class.memberProperties.find { it.name == feature.name }
 //                val mapped = targetProp?.findAnnotation<Mapped>()
 //                if (targetProp is KMutableProperty1 && mapped != null) {
 //                    val path = (mapped.path.ifEmpty { targetProp.name })
 //                    childNodeTransformer =
 //                        ChildNodeTransformer(
 //                            childKey,
 //                            transformer.getter(path),
 //                            (targetProp as KMutableProperty1<Any, Any?>)::set,
 //                        )
 //                    transformer.children[childKey] = childNodeTransformer as ChildNodeTransformer<Any, *, *>
 //                    setChild(childNodeTransformer, source, node, feature)
 //                } else {
                    //transformer.children[childKey] = NO_CHILD_NODE
 //                }
  //          }
       }

//
//    protected open fun asOrigin(source: Any): Origin? = if (source is Origin) source else null
//
//    protected open fun setChild(
//        childNodeTransformer: ChildNodeTransformer<*, *, *>,
//        source: Any,
//        node: NodeLike,
//        feature: FeatureDescription,
//    ) {
//        val childFactory = childNodeTransformer as ChildNodeTransformer<Any, Any, Any>
//        val childrenSource = childFactory.get(getSource(node, source))
//        val child: Any? =
//            if (feature.isMultiple) {
//                (childrenSource as List<*>).map { transformIntoNodes(it, node) }.flatten() ?: listOf<NodeLike>()
//            } else {
//                transform(childrenSource, node)
//            }
//        try {
//            childNodeTransformer.set(node, child)
//        } catch (e: IllegalArgumentException) {
//            throw Error("Could not set child $childNodeTransformer", e)
//        }
//    }
//
//    protected open fun getSource(
//        node: NodeLike,
//        source: Any,
//    ): Any {
//        return source
//    }
//
    protected open fun <S : Any, T : NodeLike> makeNodes(
        transformer: MPNodeTransformer<S, T>,
        source: S,
        allowGenericNode: Boolean = true,
    ): List<NodeLike> {
        val nodes =
            try {
                transformer.constructorToUse(source, this, transformer)
            } catch (e: Exception) {
                if (allowGenericNode) {
                    listOf(GenericMPErrorNode(e))
                } else {
                    throw e
                }
            }
        // TODO re-enable me
//        nodes.forEach { node ->
//            if (node.origin == null) {
//                node.withOrigin(asOrigin(source))
//            }
//        }
        return nodes
    }
//
protected fun <S : Any, T : NodeLike> getNodeTransformer(sourceType: KClass<*>): MPNodeTransformer<S, T>? {
    println("nodeTransformers: $nodeTransformers")
    println("sourceType: $sourceType")
    val nodeTransformer = nodeTransformers[sourceType]
    if (nodeTransformer != null) {
        return nodeTransformer as MPNodeTransformer<S, T>
    } else {
        // TODO here we should get the Concept and all of its super concepts
        // TODO here for classes that we know are mapped to concepts we also consider super concepts
//        for (superConcept in concept.superConceptLikes) {
//            val superClassNodeTransformer = getNodeTransformer<S, T>(superConcept)
//            if (superClassNodeTransformer != null) {
//                return superClassNodeTransformer
//            }
//        }
    }
    return null
}

//    protected fun <S : Any, T : NodeLike> getNodeTransformer(concept: ConceptLike): MPNodeTransformer<S, T>? {
//        val nodeTransformer = nodeTransformers[concept]
//        if (nodeTransformer != null) {
//            return nodeTransformer as MPNodeTransformer<S, T>
//        } else {
//            // TODO here we should get the Concept and all of its super concepts
//             for (superConcept in concept.superConceptLikes) {
//                 val superClassNodeTransformer = getNodeTransformer<S, T>(superConcept)
//                 if (superClassNodeTransformer != null) {
//                     return superClassNodeTransformer
//                 }
//             }
//        }
//        return null
//}

     fun <S : Any, T : NodeLike> registerNodeTransformer(
        concept: KClass<*>,
        transformer: (S, MPASTTransformer, MPNodeTransformer<S, T>) -> T?,
    ): MPNodeTransformer<S, T> {
        val nodeTransformer = MPNodeTransformer.single<S, T>(transformer)
        nodeTransformers[concept] = nodeTransformer
        return nodeTransformer
    }

//    fun <S : Any?, T : NodeLike> registerNodeTransformer(
//        concept: ConceptLike,
//        transformer: (S, MPASTTransformer, MPNodeTransformer<S, T>) -> T?,
//    ): MPNodeTransformer<S, T> {
//        val nodeTransformer = MPNodeTransformer.single(transformer)
//        nodeTransformers[concept] = nodeTransformer
//        return nodeTransformer
//    }
//
//    fun <S : Any, T : NodeLike> registerMultipleNodeTransformer(
//        kclass: KClass<S>,
//        transformer: (S, ASTTransformer, NodeTransformer<S, T>) -> List<T>,
//    ): NodeTransformer<S, T> {
//        val nodeTransformer = NodeTransformer(transformer)
//        nodeTransformers[kclass] = nodeTransformer
//        return nodeTransformer
//    }
//
//    fun <S : Any, T : NodeLike> registerNodeTransformer(
//        kclass: KClass<S>,
//        transformer: (S, ASTTransformer) -> T?,
//    ): NodeTransformer<S, T> =
//        registerNodeTransformer(kclass) { source, transformer, _ ->
//            transformer(
//                source,
//                transformer,
//            )
//        }
//
//    fun <S : Any, T : NodeLike> registerMultipleNodeTransformer(
//        kclass: KClass<S>,
//        factory: (S) -> List<T>,
//    ): NodeTransformer<S, T> = registerMultipleNodeTransformer(kclass) { input, _, _ -> factory(input) }
//
//    inline fun <reified S : Any, reified T : NodeLike> registerNodeTransformer(): NodeTransformer<S, T> {
//        TODO()
//        //return registerNodeTransformer(S::class, T::class)
//    }
//
//    inline fun <reified S : Any, T : NodeLike> registerNodeTransformer(
//        crossinline transformer: S.(ASTTransformer) -> T?,
//    ): NodeTransformer<S, T> =
//        registerNodeTransformer(S::class) { source, transformer, _ ->
//            source.transformer(transformer)
//        }
//
//    fun <S : Any, T : NodeLike> registerNodeTransformer(
//        kclass: KClass<S>,
//        transformer: (S) -> T?,
//    ): NodeTransformer<S, T> = registerNodeTransformer(kclass) { input, _, _ -> transformer(input) }
//
//    /**
//     * Define the origin of the node as the source, but only if source is a Node, otherwise
//     * this method does not do anything.
//     */
//    private fun <N : NodeLike> N.settingNodeOrigin(source: Any): N {
//        if (source is NodeLike) {
//            this.origin = NodeOrigin(source)
//        }
//        return this
//    }
//
//    inline fun <reified S : Any> notTranslateDirectly(): NodeTransformer<S, NodeLike> =
//        TODO()
// //        registerNodeTransformer<S, NodeLike> {
// //            throw java.lang.IllegalStateException(
// //                "A Node of this type (${this.javaClass.canonicalName}) should never be translated directly. " +
// //                    "It is expected that the container will not delegate the translation of this node but it will " +
// //                    "handle it directly",
// //            )
// //        }
//
// //    fun <S : Any, T : NodeLike> registerNodeTransformer(
// //        source: KClass<S>,
// //        target: KClass<T>,
// //        parameterConverters: List<ParameterConverter> = emptyList(),
// //    ): NodeTransformer<S, T> {
// //        registerKnownClass(target)
// //        // We are looking for any constructor with does not take parameters or have default
// //        // values for all its parameters
// //        val emptyLikeConstructor = target.constructors.find { it.parameters.all { param -> param.isOptional } }
// //        val nodeTransformer =
// //            NodeTransformer.single(
// //                { source: S, _, thisTransformer ->
// //                    if (target.isSealed) {
// //                        throw IllegalStateException("Unable to instantiate sealed class $target")
// //                    }
// //
// //                    fun getConstructorParameterValue(kParameter: KParameter): ParameterValue {
// //                        try {
// //                            val childNodeTransformer =
// //                                thisTransformer.getChildNodeTransformer<Any, T, Any>(
// //                                    target,
// //                                    kParameter.name!!,
// //                                )
// //                            if (childNodeTransformer == null) {
// //                                if (kParameter.isOptional) {
// //                                    return AbsentParameterValue
// //                                }
// //                                // see https://youtrack.jetbrains.com/issue/KT-65341
// //                                val paramName = kParameter.name!!
// //                                throw java.lang.IllegalStateException(
// //                                    "We do not know how to produce " +
// //                                        "parameter $paramName for $target",
// //                                )
// //                            } else {
// //                                return when (val childSource = childNodeTransformer.get.invoke(source)) {
// //                                    null -> {
// //                                        AbsentParameterValue
// //                                    }
// //
// //                                    is List<*> -> {
// //                                        PresentParameterValue(
// //                                            childSource
// //                                                .map { transformIntoNodes(it) }
// //                                                .flatten()
// //                                                .toMutableList(),
// //                                        )
// //                                    }
// //
// //                                    is String -> {
// //                                        PresentParameterValue(childSource)
// //                                    }
// //
// //                                    else -> {
// //                                        val paramConverter =
// //                                            parameterConverters.find {
// //                                                it.isApplicable(kParameter, childSource)
// //                                            }
// //                                        if (paramConverter != null) {
// //                                            PresentParameterValue(paramConverter.convert(kParameter, childSource))
// //                                        } else {
// //                                            PresentParameterValue(transform(childSource))
// //                                        }
// //                                    }
// //                                }
// //                            }
// //                        } catch (t: Throwable) {
// //                            // See https://youtrack.jetbrains.com/issue/KT-65341
// //                            val paramName = kParameter.name!!
// //                            throw RuntimeException(
// //                                "Issue while populating parameter $paramName in " +
// //                                    "constructor ${target.qualifiedName}.${target.preferredConstructor()}",
// //                                t,
// //                            )
// //                        }
// //                    }
// //                    // We check `childrenSetAtConstruction` and not `emptyLikeConstructor` because, while we set this value
// //                    // initially based on `emptyLikeConstructor` being equal to null, this can be later changed in
// //                    // `withChild`, so we should really check the value that `childrenSetAtConstruction` time has when
// //                    // we actually invoke
// //                    // the transformer.
// //                    if (thisTransformer.childrenSetAtConstruction) {
// //                        val constructor = target.preferredConstructor()
// //                        val constructorParamValues =
// //                            constructor
// //                                .parameters
// //                                .map { it to getConstructorParameterValue(it) }
// //                                .filter { it.second is PresentParameterValue }
// //                                .associate { it.first to (it.second as PresentParameterValue).value }
// //                        try {
// //                            val instance = constructor.callBy(constructorParamValues)
// //                            instance.children.forEach { child -> child.parent = instance }
// //                            instance.settingNodeOrigin(source)
// //                        } catch (t: Throwable) {
// //                            throw RuntimeException(
// //                                "Invocation of constructor $constructor failed. " +
// //                                    "We passed: ${
// //                                        constructorParamValues.map { "${it.key.name}=${it.value}" }
// //                                            .joinToString(", ")
// //                                    }",
// //                                t,
// //                            )
// //                        }
// //                    } else {
// //                        if (emptyLikeConstructor == null) {
// //                            throw RuntimeException(
// //                                "childrenSetAtConstruction is set but there is no empty like " +
// //                                    "constructor for $target",
// //                            )
// //                        }
// //                        target.createInstance().settingNodeOrigin(source)
// //                    }
// //                },
// //                // If I do not have an emptyLikeConstructor, then I am forced to invoke a constructor with parameters and
// //                // therefore setting the children at construction time.
// //                // Note that we are assuming that either we set no children at construction time or we set all of them
// //                childrenSetAtConstruction = emptyLikeConstructor == null,
// //            )
// //        nodeTransformers[source] = nodeTransformer
// //        return nodeTransformer
// //    }
//
//    fun <T : NodeLike> registerIdentityTransformation(nodeClass: KClass<T>) =
//        registerNodeTransformer(nodeClass) { node -> node }.skipChildren()
//
//    private fun registerKnownClass(target: KClass<*>) {
//        val qualifiedName = target.qualifiedName
//        val packageName =
//            if (qualifiedName != null) {
//                val endIndex = qualifiedName.lastIndexOf('.')
//                if (endIndex >= 0) {
//                    qualifiedName.substring(0, endIndex)
//                } else {
//                    ""
//                }
//            } else {
//                ""
//            }
//        TODO()
//        //val set = _knownClasses.computeIfAbsent(packageName) { mutableSetOf() }
//        //set.add(target)
//    }
//
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
