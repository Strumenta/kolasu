package com.strumenta.kolasu.model

import org.antlr.v4.runtime.ParserRuleContext

/**
 * The Abstract Syntax Tree will be constituted by instances of Node.
 */
open class Node(open val specifiedPosition: Position? = null) {
    var parseTreeNode: ParserRuleContext? = null
    var parent: Node? = null

    val position: Position?
        get() = specifiedPosition ?: parseTreeNode?.position
}

/**
 * This should be used for all relations which are secondary, i.e., they are calculated from other relations
 */
annotation class Derived

/**
 * This will be used to mark all the properties that returns a Node or a list of Node which are not
 * contained by the Node having the properties, they are just references
 */
annotation class Link
