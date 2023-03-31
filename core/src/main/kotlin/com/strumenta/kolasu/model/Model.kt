package com.strumenta.kolasu.model

import com.strumenta.kolasu.model.lionweb.concept
import com.strumenta.kolasu.parsing.ParseTreeOrigin
import com.strumenta.kolasu.traversing.walk
import org.lionweb.lioncore.java.metamodel.Annotation
import org.lionweb.lioncore.java.metamodel.Concept
import org.lionweb.lioncore.java.metamodel.Containment
import org.lionweb.lioncore.java.metamodel.Property
import org.lionweb.lioncore.java.metamodel.Reference
import org.lionweb.lioncore.java.model.AnnotationInstance
import org.lionweb.lioncore.java.model.Model
import org.lionweb.lioncore.java.model.Node
import org.lionweb.lioncore.java.model.ReferenceValue
import java.lang.IllegalArgumentException
import java.lang.IllegalStateException
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1
import kotlin.reflect.KVisibility
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.functions
import kotlin.reflect.full.memberProperties

interface Origin {
    val position: Position?
    val sourceText: String?
    val source: Source?
        get() = position?.source
}

class SimpleOrigin(override val position: Position?, override val sourceText: String?) : Origin

data class CompositeOrigin(
    val elements: List<Origin>,
    override val position: Position?,
    override val sourceText: String?
) : Origin

interface Destination

data class CompositeDestination(val elements: List<Destination>) : Destination
data class TextFileDestination(val position: Position?) : Destination

/**
 * The Abstract Syntax Tree will be constituted by instances of Node.
 *
 * It implements Origin as it could be the source of a AST-to-AST transformation, so the node itself can be
 * the Origin of another node.
 */
open class ASTNode() : Node, Origin, Destination {

    @Internal
    protected var positionOverride: Position? = null

    constructor(position: Position?) : this() {
        this.position = position
    }

    constructor(origin: Origin?) : this() {
        if (origin != null) {
            this.origin = origin
        }
    }

    @Internal
    @Deprecated("Use Concept")
    open val nodeType: String
        get() = this::class.qualifiedName!!

    /**
     * The properties of this AST nodes, including attributes, children, and references.
     */
    @property:Internal
    @Deprecated("Use Concept")
    open val properties: List<PropertyDescription>
        get() = try {
            nodeProperties.map { PropertyDescription.buildFor(it, this) }
        } catch (e: Throwable) {
            throw RuntimeException("Issue while getting properties of node ${this::class.qualifiedName}", e)
        }

    /**
     * The node from which this AST Node has been generated, if any.
     */
    @property:Internal
    var origin: Origin? = null

    /**
     * The parent node, if any.
     */
    @property:Internal
    private var _parent: ASTNode? = null

    /**
     * The position of this node in the source text.
     * If a position has been provided when creating this node, it is returned.
     * Otherwise, the value of this property is the position of the origin, if any.
     */
    @property:Internal
    override var position: Position?
        get() = positionOverride ?: origin?.position
        set(position) {
            this.positionOverride = position
        }

    @property:Internal
    override val source: Source?
        get() = origin?.source

    fun detach(keepPosition: Boolean = true, keepSourceText: Boolean = false) {
        val existingOrigin = origin
        if (existingOrigin != null) {
            if (keepPosition || keepSourceText) {
                this.origin = SimpleOrigin(
                    if (keepPosition) existingOrigin.position else null,
                    if (keepSourceText) existingOrigin.sourceText else null
                )
            } else {
                this.origin = null
            }
            if (existingOrigin is ASTNode && existingOrigin.destination == this) {
                existingOrigin.destination = null
            }
        }
    }

    /**
     * Tests whether the given position is contained in the interval represented by this object.
     * @param position the position
     */
    fun contains(position: Position?): Boolean {
        return this.position?.contains(position) ?: false
    }

    /**
     * Tests whether the given position overlaps the interval represented by this object.
     * @param position the position
     */
    fun overlaps(position: Position?): Boolean {
        return this.position?.overlaps(position) ?: false
    }

    /**
     * The source text for this node
     */
    @Internal
    override val sourceText: String?
        get() = origin?.sourceText

    @Internal
    var destination: Destination? = null

    /**
     * This must be final because otherwise data classes extending this will automatically generate
     * their own implementation. If Link properties are present it could lead to stack overflows in case
     * of circular graphs.
     */
    final override fun toString(): String {
        return "${this.nodeType}(${properties.joinToString(", ") { "${it.name}=${it.valueToString()}" }})"
    }

    override fun getPropertyValue(property: Property): Any? {
        if (!this.concept.allProperties().contains(property)) {
            throw IllegalArgumentException("Invalid property $property")
        }
        val memberProperty = this::class.memberProperties.find { it.name == property.simpleName }
            ?: throw IllegalStateException()
        return (memberProperty as KProperty1<ASTNode, *>).get(this)
    }

    override fun setPropertyValue(property: Property, value: Any?) {
        TODO("Not yet implemented")
    }

    override fun getChildren(): MutableList<ASTNode> {
        return this.concept.allContainments().map { getChildren(it) }.flatten().toMutableList()
    }

    override fun getChildren(containment: Containment): MutableList<ASTNode> {
        if (!this.concept.allContainments().contains(containment)) {
            throw IllegalArgumentException("Invalid containment $containment")
        }
        val memberProperty = this::class.memberProperties.find { it.name == containment.simpleName }
            ?: throw IllegalStateException()
        try {
            val value = if (memberProperty.visibility == KVisibility.PRIVATE) {
                val getter = this::class.functions.find { it.name == "get${containment.simpleName!!.capitalize()}" }!!
                getter.call(this) as MutableList<ASTNode>
            } else {
                (memberProperty as KProperty1<ASTNode, *>).get(this)
            }
            return when (value) {
                is Collection<*> -> {
                    value.toMutableList() as MutableList<ASTNode>
                }

                null -> {
                    mutableListOf()
                }

                else -> {
                    mutableListOf(value as ASTNode)
                }
            }
        } catch (e: Throwable) {
            throw RuntimeException("Unable to access containment $containment", e)
        }
    }

    override fun addChild(p0: Containment?, p1: Node?) {
        TODO("Not yet implemented")
    }

    override fun removeChild(p0: Node?) {
        TODO("Not yet implemented")
    }

    override fun getReferredNodes(p0: Reference): MutableList<Node> {
        TODO("Not yet implemented")
    }

    override fun getReferenceValues(reference: Reference): List<ReferenceValue> {
        if (!this.concept.allReferences().contains(reference)) {
            throw IllegalArgumentException("Invalid reference $reference")
        }
        val memberProperty = this::class.memberProperties.find { it.name == reference.simpleName }
            ?: throw IllegalStateException()
        try {
            val value = if (memberProperty.visibility == KVisibility.PRIVATE) {
                val getter = this::class.functions.find { it.name == "get${reference.simpleName!!.capitalize()}" }!!
                getter.call(this) as MutableList<ASTNode>
            } else {
                (memberProperty as KProperty1<ASTNode, *>).get(this)
            }
            return when (value) {
                is Collection<*> -> {
                    (value.toList() as MutableList<ReferenceByName<*>>).map {
                        ReferenceValue(it.referred as? Node, it.name)
                    }
                }

                null -> {
                    mutableListOf()
                }

                else -> {
                    mutableListOf((value as ReferenceByName<*>).let { ReferenceValue(it.referred as? Node, it.name) })
                }
            }
        } catch (e: Throwable) {
            throw RuntimeException("Unable to access reference $reference", e)
        }
    }

    override fun addReferenceValue(p0: Reference, p1: ReferenceValue?) {
        TODO("Not yet implemented")
    }

    override fun getID(): String? {
        return "${sourceID(source)}-${pathID(this)}"
    }

    private fun sourceID(source: Source?): String {
        return source?.id ?: "_UNSPECIFIED_"
    }

    private fun pathID(node: ASTNode): String {
        return if (node.parent == null) {
            ""
        } else {
            "${pathID(node.parent!!)}-${node.containmentFeature!!.simpleName}${node.indexInContainingProperty()}"
        }
    }

    override fun getModel(): Model {
        TODO("Not yet implemented")
    }

    override fun getRoot(): Node {
        TODO("Not yet implemented")
    }

    override fun getParent(): ASTNode? {
        return _parent
    }

    fun setParent(parent: ASTNode?) {
        this._parent = parent
    }

    override fun getConcept(): Concept {
        return this::class.concept
    }

    override fun getAnnotations(): MutableList<AnnotationInstance> {
        TODO("Not yet implemented")
    }

    override fun getAnnotations(p0: Annotation?): MutableList<AnnotationInstance> {
        TODO("Not yet implemented")
    }

    override fun getContainmentFeature(): Containment? {
        if (this.parent == null) {
            return null
        }
        this.parent!!.concept.allContainments().forEach { containment ->
            if (this.parent!!.getChildren(containment).contains(this)) {
                return containment
            }
        }
        throw IllegalStateException()
    }

    override fun addAnnotation(p0: AnnotationInstance?) {
        TODO("Not yet implemented")
    }
}

fun <N : ASTNode> N.withPosition(position: Position?): N {
    this.position = position
    return this
}

fun <N : ASTNode> N.withOrigin(origin: Origin?): N {
    this.origin = if (origin == this) { null } else { origin }
    return this
}

val <T : Any> Class<T>.nodeProperties: Collection<KProperty1<T, *>>
    get() = this.kotlin.nodeProperties
val <T : Any> KClass<T>.nodeProperties: Collection<KProperty1<T, *>>
    get() = memberProperties.asSequence()
        .filter {
            it.visibility == KVisibility.PUBLIC ||
                (this.functions.find { f -> f.name == "get${it.name.capitalize()}" }?.visibility == KVisibility.PUBLIC)
        }
        .filter { it.findAnnotation<Derived>() == null }
        .filter { it.getter.findAnnotation<Internal>() == null }
        .filter { it.findAnnotation<Internal>() == null }
        .filter { it.findAnnotation<Link>() == null }
        .toList()

/**
 * @return all properties of this node that are considered AST properties.
 */
val <T : ASTNode> T.nodeProperties: Collection<KProperty1<T, *>>
    get() = this.javaClass.nodeProperties

/**
 * Use this to mark properties that are internal, i.e., they are used for bookkeeping and are not part of the model,
 * so that they will not be considered branches of the AST.
 */
@Target(AnnotationTarget.PROPERTY, AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class Internal

/**
 * Use this to mark all relations which are secondary, i.e., they are calculated from other relations,
 * so that they will not be considered branches of the AST.
 */
annotation class Derived

/**
 * Use this to mark all the properties that return a Node or a list of Nodes which are not
 * contained by the Node having the properties. In other words: they are just references.
 * This will prevent them from being considered branches of the AST.
 */
annotation class Link

/**
 * Use this to mark something that does not inherit from Node as a node, so it will be included in the AST.
 */
annotation class NodeType

/**
 * Assign the given source to all nodes without a source
 */
fun assignSourceToTree(root: ASTNode, source: Source, sourceText: String) {
    root.walk().forEach {
        if (it.origin == null) {
            it.position?.let { position ->
                if (position.source == null) {
                    position.source = source
                }
            }
            it.origin = SimpleOrigin(position = it.position, sourceText)
        } else if (it.origin is ParseTreeOrigin) {
            val pto = it.origin as ParseTreeOrigin
            if (pto.source == null) {
                pto.source = source
            }
        }
    }
}
