package me.tomassetti.kolasu.model

import java.lang.reflect.ParameterizedType
import java.util.*
import kotlin.reflect.KParameter
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.jvm.javaType
import kotlin.reflect.memberProperties
import kotlin.reflect.primaryConstructor

//
// Generic part: valid for all languages
//

interface Node {
    val position: Position?
}

annotation class Derived

///
/// Printing
///

const val indentBlock = "  "

fun Node.relevantMemberProperties() = this.javaClass.kotlin.memberProperties.filter { !it.name.startsWith("component") && !it.name.equals("position") }

fun Node.multilineString(indent: String = "") : String {
    val sb = StringBuffer()
    if (this.relevantMemberProperties().isEmpty()) {
        sb.append("$indent${this.javaClass.simpleName}\n")
    } else {
        sb.append("$indent${this.javaClass.simpleName} {\n")
        this.relevantMemberProperties().forEach {
            val mt = it.returnType.javaType
            if (mt is ParameterizedType && mt.rawType.equals(List::class.java)) {
                val paramType = mt.actualTypeArguments[0]
                if (paramType is Class<*> && Node::class.java.isAssignableFrom(paramType)) {
                    sb.append("$indent$indentBlock${it.name} = [\n")
                    (it.get(this) as List<out Node>).forEach { sb.append(it.multilineString(indent + indentBlock + indentBlock)) }
                    sb.append("$indent$indentBlock]\n")
                }
            } else {
                val value = it.get(this)
                if (value is Node) {
                    sb.append("$indent$indentBlock${it.name} = [\n")
                    sb.append(value.multilineString(indent + indentBlock + indentBlock))
                    sb.append("$indent$indentBlock]\n")
                } else {
                    sb.append("$indent$indentBlock${it.name} = ${it.get(this)}\n")
                }
            }
        }
        sb.append("$indent} // ${this.javaClass.simpleName}\n")
    }
    return sb.toString()
}

///
/// Position
///

data class Point(val line: Int, val column: Int) {
    override fun toString() = "Line $line, Column $column"

    /**
     * Translate the Point to an offset in the original code stream.
     */
    fun offset(code: String) : Int {
        val lines = code.split("\n")
        val newLines = this.line - 1
        return lines.subList(0, this.line - 1).foldRight(0, { it, acc -> it.length + acc }) + newLines + column
    }

    fun isBefore(other: Point) : Boolean = line < other.line || (line == other.line && column < other.column)

}

data class Position(val start: Point, val end: Point) {

    init {
        if (end.isBefore(start)) {
            throw IllegalArgumentException("End should follows start")
        }
    }

    /**
     * Given the whole code extract the portion of text corresponding to this position
     */
    fun text(wholeText: String): String {
        return wholeText.substring(start.offset(wholeText), end.offset(wholeText))
    }

    fun length(code: String) = end.offset(code) - start.offset(code)
}

/**
 * Utility function to create a Position
 */
fun pos(startLine:Int, startCol:Int, endLine:Int, endCol:Int) = Position(Point(startLine,startCol),Point(endLine,endCol))

fun Node.isBefore(other: Node) : Boolean = position!!.start.isBefore(other.position!!.start)

///
/// Processing
///

fun Node.process(operation: (Node) -> Unit) {
    operation(this)
    this.javaClass.kotlin.memberProperties.filter { it.findAnnotation<Derived>() == null }.forEach { p ->
        val v = p.get(this)
        when (v) {
            is Node -> v.process(operation)
            is Collection<*> -> v.forEach { if (it is Node) it.process(operation) }
        }
    }
}

fun <T: Node> Node.specificProcess(klass: Class<T>, operation: (T) -> Unit) {
    process { if (klass.isInstance(it)) { operation(it as T) } }
}

fun <T: Node> Node.collectByType(klass: Class<T>) : List<T> {
    val res = LinkedList<T>()
    this.specificProcess(klass, {res.add(it)})
    return res
}

fun Node.processConsideringParent(operation: (Node, Node?) -> Unit, parent: Node? = null) {
    operation(this, parent)
    this.javaClass.kotlin.memberProperties.forEach { p ->
        val v = p.get(this)
        when (v) {
            is Node -> v.processConsideringParent(operation, this)
            is Collection<*> -> v.forEach { if (it is Node) it.processConsideringParent(operation, this) }
        }
    }
}

///
/// Navigation
///

fun Node.childParentMap() : Map<Node, Node> {
    val map = IdentityHashMap<Node, Node>()
    this.processConsideringParent({ child, parent -> if (parent != null) map[child] = parent })
    return map
}

fun <T: Node> Node.ancestor(klass: Class<T>, childParentMap: Map<Node, Node>) : T?{
    if (childParentMap.containsKey(this)) {
        val p = childParentMap[this]
        if (klass.isInstance(p)) {
            return p as T
        }
        return p!!.ancestor(klass, childParentMap)
    }
    return null
}

///
/// Transforming
///

fun Node.transform(operation: (Node) -> Node) : Node {
    operation(this)
    val changes = HashMap<String, Any>()
    this.javaClass.kotlin.memberProperties.forEach { p ->
        val v = p.get(this)
        when (v) {
            is Node -> {
                val newValue = v.transform(operation)
                if (newValue != v) changes[p.name] = newValue
            }
            is Collection<*> -> {
                val newValue = v.map { if (it is Node) it.transform(operation) else it }
                if (newValue != v) changes[p.name] = newValue
            }
        }
    }
    var instanceToTransform = this
    if (!changes.isEmpty()) {
        val constructor = this.javaClass.kotlin.primaryConstructor!!
        val params = HashMap<KParameter, Any?>()
        constructor.parameters.forEach { param ->
            if (changes.containsKey(param.name)) {
                params[param] = changes[param.name]
            } else {
                params[param] = this.javaClass.kotlin.memberProperties.find { param.name == it.name }!!.get(this)
            }
        }
        instanceToTransform = constructor.callBy(params)
    }
    return operation(instanceToTransform)
}

///
/// Named
///

interface Named {
    val name: String
}

///
/// References
///

data class ReferenceByName<N>(val name: String, var referred: N? = null) where N : Named {
    override fun toString(): String {
        if (referred == null) {
            return "Ref($name)[Unsolved]"
        } else {
            return "Ref($name)[Solved]"
        }
    }

    override fun hashCode(): Int {
        return name.hashCode() * (7 + if (referred == null) 1 else 2)
    }

    val resolved : Boolean
        get() = referred != null
}

fun <N> ReferenceByName<N>.tryToResolve(candidates: List<N>) : Boolean where N : Named {
    val res = candidates.find { it.name == this.name }
    this.referred = res
    return res != null
}

fun <N> ReferenceByName<N>.tryToResolve(possibleValue: N?) : Boolean where N : Named {
    if (possibleValue == null) {
        return false
    } else {
        this.referred = possibleValue
        return true
    }
}


