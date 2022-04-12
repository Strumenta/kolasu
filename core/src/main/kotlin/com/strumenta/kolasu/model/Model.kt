package com.strumenta.kolasu.model

interface Origin {
    val position: Position?
    val sourceText: String?
}

class JustPosition(override val position: Position) : Origin {
    override val sourceText: String? = null
}

/**
 * The Abstract Syntax Tree will be constituted by instances of Node.
 */
open class Node() : Origin {

    constructor(position: Position?) : this() {
        if (position != null) {
            origin = JustPosition(position)
        }
    }

    constructor(origin: Origin?) : this() {
        if (origin != null) {
            this.origin = origin
        }
    }

    @Derived
    open val nodeType: String
        get() = this.javaClass.canonicalName

    /**
     * The properties of this AST nodes, including attributes, children, and references.
     */
    @Derived
    open val properties: List<PropertyDescription>
        get() = try {
            nodeProperties.map { PropertyDescription.buildFor(it, this) }
        } catch (e: Throwable) {
            throw RuntimeException("Issue while getting properties of $this (${this.javaClass})", e)
        }

    /**
     * The node from which this AST Node has been generated, if any.
     */
    @Derived
    var origin: Origin? = null

    /**
     * The parent node, if any.
     */
    @Derived
    var parent: Node? = null

    /**
     * The position of this node in the source text.
     * If a position has been provided when creating this node, it is returned.
     * Otherwise, the value of this property is the position of the original parse tree node, if any.
     */
    @Derived
    override var position: Position?
        get() = origin?.position
        set(position) {
            if (origin != null && origin !is JustPosition) {
                throw IllegalStateException("Node $this already has an origin: $origin")
            }
            if (position != null) {
                this.origin = JustPosition(position)
            }
        }

    /**
     * The source text for this node
     */
    @Derived
    override val sourceText: String?
        get() = origin?.sourceText
}

fun <N : Node> N.withPosition(position: Position?): N {
    this.position = position
    return this
}

fun <N : Node> N.withOrigin(origin: Origin?): N {
    this.origin = origin
    return this
}

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
