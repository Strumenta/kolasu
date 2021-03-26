package com.strumenta.kolasu.generation

import com.strumenta.kolasu.model.Position
import java.lang.IllegalStateException
import java.util.*

class Containment(val parent: ASTNode, val containmentName: String, val remover: (element: ASTNode)->Unit) {
    fun remove(element: ASTNode) {
        remover(element)
    }
}

abstract class ASTNode(val position: Position? = null) {
    fun assignTo(containment: Containment) {
        this.containment = containment
    }

    val parent: ASTNode?
        get() = containment?.parent
    var containment: Containment? = null
}

sealed class Rule(val name: String) : ASTNode()
class LexerRule(name: String) : Rule(name)
class ParserRule(name: String) : Rule(name)

class ContainmentList<E: ASTNode>(val containment: Containment) : MutableList<E> {
    private val data = LinkedList<E>()

    override val size: Int
        get() = TODO("Not yet implemented")

    override fun contains(element: E): Boolean {
        return data.contains(element)
    }

    override fun containsAll(elements: Collection<E>): Boolean {
        TODO("Not yet implemented")
    }

    override fun get(index: Int): E {
        TODO("Not yet implemented")
    }

    override fun indexOf(element: E): Int {
        TODO("Not yet implemented")
    }

    override fun isEmpty(): Boolean {
        TODO("Not yet implemented")
    }

    override fun iterator(): MutableIterator<E> {
        TODO("Not yet implemented")
    }

    override fun lastIndexOf(element: E): Int {
        TODO("Not yet implemented")
    }

    override fun add(element: E): Boolean {
        if (this.contains(element)) {
            throw IllegalStateException("Cannot add again")
        }
        this.data.add(element)
        element.containment?.remove(element)
        element.assignTo(containment)
        return true
    }

    override fun add(index: Int, element: E) {
        TODO("Not yet implemented")
    }

    override fun addAll(index: Int, elements: Collection<E>): Boolean {
        TODO("Not yet implemented")
    }

    override fun addAll(elements: Collection<E>): Boolean {
        TODO("Not yet implemented")
    }

    override fun clear() {
        TODO("Not yet implemented")
    }

    override fun listIterator(): MutableListIterator<E> {
        TODO("Not yet implemented")
    }

    override fun listIterator(index: Int): MutableListIterator<E> {
        TODO("Not yet implemented")
    }

    override fun remove(element: E): Boolean {
        TODO("Not yet implemented")
    }

    override fun removeAll(elements: Collection<E>): Boolean {
        TODO("Not yet implemented")
    }

    override fun removeAt(index: Int): E {
        TODO("Not yet implemented")
    }

    override fun retainAll(elements: Collection<E>): Boolean {
        TODO("Not yet implemented")
    }

    override fun set(index: Int, element: E): E {
        TODO("Not yet implemented")
    }

    override fun subList(fromIndex: Int, toIndex: Int): MutableList<E> {
        TODO("Not yet implemented")
    }

}

class Grammar(var name: String, var type: Type) : ASTNode() {
    enum class Type {
        LEXER,
        PARSER,
        MIXED
    }
    val rules : MutableList<Rule> = ContainmentList(Containment(this, "rules") {
        removeRule(it as Rule)
    })
    private fun removeRule(element: Rule) {
        this.rules.remove(element)
    }
}

fun main(args: Array<String>) {
    val r1 = LexerRule("lr1")
    val r2 = LexerRule("lr2")
    val p1 = ParserRule("pr1")
    val g = Grammar("myGrammar", Grammar.Type.MIXED)
    g.rules.add(r1)
    g.rules.add(r2)
    g.rules.add(p1)
    println(r1.parent)
}