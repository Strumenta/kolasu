package com.strumenta.kolasu.model

import java.lang.reflect.ParameterizedType
import kotlin.reflect.KProperty1
import kotlin.reflect.KVisibility.INTERNAL
import kotlin.reflect.KVisibility.PRIVATE
import kotlin.reflect.KVisibility.PROTECTED
import kotlin.reflect.KVisibility.PUBLIC
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.isAccessible
import kotlin.reflect.jvm.javaType

private const val indentBlock = "  "

fun Node.relevantMemberProperties(withPosition: Boolean = false, withNodeType: Boolean = false) =
    this.javaClass.kotlin.memberProperties
        .filter {
            !it.name.startsWith("component") &&
                (it.name != "position" || withPosition) &&
                (it.name != "nodeType" || withNodeType) &&
                it.name != "specifiedPosition" &&
                it.name != "properties" &&
                it.name != "parseTreeNode" &&
                it.name != "parent"
        }

data class DebugPrintConfiguration(
    val skipEmptyCollections: Boolean = false,
    val skipNull: Boolean = false,
    val forceShowPosition: Boolean = false,
    val hide: List<String> = emptyList(),
    val skipPrivateProperties: Boolean = true,
    val skipProtectedProperties: Boolean = true,
    val skipInternalProperties: Boolean = true,
    val skipPublicProperties: Boolean = false
)

private fun KProperty1<Node, *>.hasRelevantVisibility(configuration: DebugPrintConfiguration): Boolean {
    return when (requireNotNull(this.visibility)) {
        PRIVATE -> !configuration.skipPrivateProperties
        PROTECTED -> !configuration.skipProtectedProperties
        INTERNAL -> !configuration.skipInternalProperties
        PUBLIC -> !configuration.skipPublicProperties
    }
}

private fun Node.showSingleAttribute(indent: String, sb: StringBuilder, propertyName: String, value: Any?) {
    sb.append("$indent$indentBlock$propertyName = ${value}\n")
}

fun Any?.debugPrint(indent: String = "", configuration: DebugPrintConfiguration = DebugPrintConfiguration()): String {
    val sb = StringBuilder()
    sb.append("$indent${this}\n")
    return sb.toString()
}

// some fancy reflection tests make sure the cast always succeeds
@Suppress("UNCHECKED_CAST")
fun Node.debugPrint(indent: String = "", configuration: DebugPrintConfiguration = DebugPrintConfiguration()): String {
    val sb = StringBuilder()
    if (this.relevantMemberProperties(withPosition = configuration.forceShowPosition).isEmpty()) {
        sb.append("$indent${this.javaClass.simpleName}\n")
    } else {
        sb.append("$indent${this.javaClass.simpleName} {\n")
        this.relevantMemberProperties(withPosition = configuration.forceShowPosition).forEach { property ->
            if (configuration.hide.contains(property.name)) {
                // skipping
            } else {
                val mt = property.returnType.javaType
                if (mt is ParameterizedType && mt.rawType == List::class.java) {
                    if (property.hasRelevantVisibility(configuration)) {
                        property.isAccessible = true
                        if (property.get(this) == null && !configuration.skipNull) {
                            sb.append("$indent$indentBlock${property.name} = null")
                        } else {
                            val value = property.get(this) as List<*>
                            if (value.isEmpty() && configuration.skipEmptyCollections) {
                                // nothing to do
                            } else {
                                val paramType = mt.actualTypeArguments[0]
                                if (paramType is Class<*> && Node::class.java.isAssignableFrom(paramType)) {
                                    sb.append("$indent$indentBlock${property.name} = [\n")
                                    (value as List<Node>).forEach {
                                        sb.append(
                                            it.debugPrint(
                                                indent + indentBlock + indentBlock, configuration
                                            )
                                        )
                                    }
                                    sb.append("$indent$indentBlock]\n")
                                } else {
                                    sb.append("$indent$indentBlock${property.name} = [\n")
                                    value.forEach {
                                        sb.append(
                                            it?.debugPrint(
                                                indent + indentBlock + indentBlock, configuration
                                            )
                                        )
                                    }
                                    sb.append("$indent$indentBlock]\n")
                                }
                            }
                        }
                    }
                } else {
                    if (property.hasRelevantVisibility(configuration)) {
                        property.isAccessible = true
                        val value = property.get(this)
                        if (value == null && configuration.skipNull) {
                            // nothing to do
                        } else {
                            if (value is Node) {
                                sb.append("$indent$indentBlock${property.name} = [\n")
                                sb.append(
                                    value.debugPrint(
                                        indent + indentBlock + indentBlock, configuration
                                    )
                                )
                                sb.append("$indent$indentBlock]\n")
                            } else {
                                this.showSingleAttribute(indent, sb, property.name, value)
                            }
                        }
                    }
                }
            }
        }
        sb.append("$indent} // ${this.javaClass.simpleName}\n")
    }
    return sb.toString()
}
