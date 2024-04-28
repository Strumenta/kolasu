package com.strumenta.kolasu.transformation

import com.strumenta.kolasu.language.Concept
import com.strumenta.kolasu.model.NodeLike
import com.strumenta.kolasu.model.Range
import com.strumenta.kolasu.model.withOrigin
import com.strumenta.kolasu.validation.Issue
import com.strumenta.kolasu.validation.IssueSeverity
import kotlin.jvm.JvmOverloads
import kotlin.reflect.KClass

typealias DefaultTransformer = (Any, MPASTTransformer) -> List<NodeLike>

// CONSIDER USING CONCEPT INSTEAD OF CLASSES WHEN SPECIFYING THE TRANSFORMER

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
    private val conceptBaseNodeTransformers = mutableMapOf<Concept, MPNodeTransformer<*, *>>()
    private val classBaseNodeTransformers = mutableMapOf<KClass<*>, MPNodeTransformer<*, *>>()

    private var defaultTransformer: DefaultTransformer? = null

    /**
     * This ensures that the generated value is a single Node or null.
     */
    @JvmOverloads
    fun <S : Any> transform(source: S?): NodeLike? {
        val result = transformIntoNodes(source)
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

    private fun conceptOf(source: Any): Concept? {
        return this.conceptBaseNodeTransformers.keys.find { concept ->
            concept.explicitlySetKotlinClass?.isInstance(source) ?: false
        }
    }

    /**
     * Performs the transformation of a node and, recursively, its descendants.
     */
    @JvmOverloads
    fun <S : Any> transformIntoNodes(source: S?): List<NodeLike> {
        if (source == null) {
            return emptyList()
        }
        if (source is Collection<*>) {
            throw Error("Mapping error: received collection when value was expected")
        }
        val concept = conceptOf(source)
        val transformer = concept?.let { c -> getNodeTransformer<S, NodeLike>(c) }
        if (transformer != null) {
            return makeNodes(transformer, source)
        } else {
            val otherTransformer = classBaseNodeTransformers[source::class]
            if (otherTransformer != null) {
                return makeNodes<Any, NodeLike>(otherTransformer as MPNodeTransformer<Any, NodeLike>, source as Any)
            }
            if (defaultTransformer != null) {
                return defaultTransformer!!.invoke(source, this)
            }
            throw IllegalStateException("Unable to translate node $source")
        }
    }

    protected open fun <S : Any, T : NodeLike> makeNodes(
        transformer: MPNodeTransformer<S, T>,
        source: S,
    ): List<NodeLike> {
        val nodes = transformer.constructorToUse(source, this, transformer)
        // TODO re-enable me
        nodes.forEach { node ->
            if (node.origin == null) {
                node.withOrigin(asOrigin(source))
            }
        }
        return nodes
    }

    private fun <S : Any, T : NodeLike> getNodeTransformer(concept: Concept): MPNodeTransformer<S, T>? {
        val nodeTransformer = conceptBaseNodeTransformers[concept]
        if (nodeTransformer != null) {
            return nodeTransformer as MPNodeTransformer<S, T>
        } else {
            // TODO here we should get the Concept and all of its super concepts
            // TODO here for classes that we know are mapped to concepts we also consider super concepts
            for (superConcept in concept.superConceptLikes) {
                val superClassNodeTransformer = getNodeTransformer<S, T>(superConcept as Concept)
                if (superClassNodeTransformer != null) {
                    return superClassNodeTransformer
                }
            }
        }
        return null
    }

    fun <S : Any, T : NodeLike> registerNodeTransformer(
        concept: Concept,
        transformer: (S, MPASTTransformer, MPNodeTransformer<S, T>) -> T?,
    ): MPNodeTransformer<S, T> {
        val nodeTransformer = MPNodeTransformer.single<S, T>(transformer)
        conceptBaseNodeTransformers[concept] = nodeTransformer
        return nodeTransformer
    }

    fun <S : Any, T : NodeLike> registerNodeTransformer(
        kClass: KClass<*>,
        transformer: (S, MPASTTransformer, MPNodeTransformer<S, T>) -> T?,
    ): MPNodeTransformer<S, T> {
        val nodeTransformer = MPNodeTransformer.single<S, T>(transformer)
        classBaseNodeTransformers[kClass] = nodeTransformer
        return nodeTransformer
    }

    fun registerDefaultNodeTransformer(transformer: DefaultTransformer) {
        defaultTransformer = transformer
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

/**
 * Translate the given node and ensure a certain type will be obtained.
 *
 * Example:
 * ```
 * JPostIncrementExpr(translateCasted<JExpression>(expression().first()))
 * ```
 */
fun <T> MPASTTransformer.translateCasted(original: Any): T {
    val result = transform(original)
    if (result is Nothing) {
        throw IllegalStateException("Transformation produced Nothing")
    }
    return result as T
}
