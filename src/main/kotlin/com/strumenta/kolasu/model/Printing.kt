package com.strumenta.kolasu.model

import java.lang.reflect.ParameterizedType
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.javaType

private const val indentBlock = "  "

fun Node.relevantMemberProperties() = this.javaClass.kotlin.memberProperties
        .filter { !it.name.startsWith("component") && it.name != "position" && it.name != "parent" }

@Suppress("UNCHECKED_CAST")
fun Node.multilineString(indent: String = ""): String {
    val sb = StringBuffer()
    if (this.relevantMemberProperties().isEmpty()) {
        sb.append("$indent${this.javaClass.simpleName}\n")
    } else {
        sb.append("$indent${this.javaClass.simpleName} {\n")
        this.relevantMemberProperties().forEach { property ->
            val mt = property.returnType.javaType
            if (mt is ParameterizedType && mt.rawType == List::class.java) {
                val paramType = mt.actualTypeArguments[0]
                if (paramType is Class<*> && Node::class.java.isAssignableFrom(paramType)) {
                    sb.append("$indent$indentBlock${property.name} = [\n")
                    (property.get(this) as List<Node>).forEach {
                        sb.append(it.multilineString(indent + indentBlock + indentBlock))
                    }
                    sb.append("$indent$indentBlock]\n")
                }
            } else {
                val value = property.get(this)
                if (value is Node) {
                    sb.append("$indent$indentBlock${property.name} = [\n")
                    sb.append(value.multilineString(indent + indentBlock + indentBlock))
                    sb.append("$indent$indentBlock]\n")
                } else {
                    sb.append("$indent$indentBlock${property.name} = ${property.get(this)}\n")
                }
            }
        }
        sb.append("$indent} // ${this.javaClass.simpleName}\n")
    }
    return sb.toString()
}
