package com.strumenta.kolasu.model

import com.strumenta.kolasu.mapping.position
import org.antlr.v4.runtime.ParserRuleContext

/**
 * The Abstract Syntax Tree will be constituted by instances of Node.
 */
open class Node(open val specifiedPosition: Position? = null) {
    @Derived
    open val properties: List<PropertyDescription>
        get() = try {
            nodeProperties.map { PropertyDescription.buildFor(it, this) }
        } catch (e: Throwable) {
            throw RuntimeException("Issue while getting properties of $this (${this.javaClass})")
        }

    var parseTreeNode: ParserRuleContext? = null
    var parent: Node? = null

    val position: Position?
        get() = specifiedPosition ?: parseTreeNode?.position
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
