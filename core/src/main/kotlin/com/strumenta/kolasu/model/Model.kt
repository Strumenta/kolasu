package com.strumenta.kolasu.model

interface WithPosition {
    val position: Position?
}

class JustPosition(override val position: Position): WithPosition

/**
 * The Abstract Syntax Tree will be constituted by instances of Node.
 */
open class Node(): WithPosition {

    constructor(position: Position?) : this() {
        if (position != null) {
            origin = JustPosition(position)
        }
    }

    constructor(origin: WithPosition?) : this() {
        if (origin != null) {
            this.origin = origin
        }
    }

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
    var origin: WithPosition? = null

    /**
     * The parent node, if any.
     */
    var parent: Node? = null

    /**
     * The position of this node in the source text.
     * If a position has been provided when creating this node, it is returned.
     * Otherwise, the value of this property is the position of the original parse tree node, if any.
     */
    override val position: Position?
        get() = origin?.position
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
