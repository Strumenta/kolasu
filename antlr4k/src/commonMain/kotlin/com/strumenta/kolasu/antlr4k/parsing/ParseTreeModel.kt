package com.strumenta.kolasu.antlr4k.parsing

/**
 * Either a Parse Tree terminal/leaf or non-terminal/node
 */
sealed class ParseTreeElement {
    abstract fun multiLineString(indentation: String = ""): String
}

/**
 * Representation of the information contained in a Parse Tree terminal or leaf.
 */
class ParseTreeLeaf(
    val type: String,
    val text: String,
) : ParseTreeElement() {
    override fun toString(): String {
        return "T:$type[$text]"
    }

    override fun multiLineString(indentation: String): String = "${indentation}T:$type[$text]\n"
}

/**
 * Representation of the information contained in a Parse Tree non-terminal or node.
 */
class ParseTreeNode(
    val name: String,
) : ParseTreeElement() {
    val children = mutableListOf<ParseTreeElement>()

    fun child(c: ParseTreeElement): ParseTreeNode {
        children.add(c)
        return this
    }

    override fun toString(): String {
        return "Node($name) $children"
    }

    override fun multiLineString(indentation: String): String {
        val sb = StringBuilder()
        sb.append("${indentation}$name\n")
        children.forEach { c -> sb.append(c.multiLineString("$indentation  ")) }
        return sb.toString()
    }
}
