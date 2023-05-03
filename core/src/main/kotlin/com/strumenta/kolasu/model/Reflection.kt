package com.strumenta.kolasu.model

import com.sun.org.apache.xalan.internal.xsltc.compiler.util.NodeType
import kotlin.reflect.KClass
import kotlin.reflect.KClassifier
import kotlin.reflect.KProperty1
import kotlin.reflect.KType
import kotlin.reflect.KTypeProjection
import kotlin.reflect.full.isSubclassOf

fun <T : ASTNode> T.relevantMemberProperties(withPosition: Boolean = false, withNodeType: Boolean = false):
    List<KProperty1<T, *>> {
    val list = this::class.nodeProperties.map { it as KProperty1<T, *> }.toMutableList()
    if (withPosition) {
        list.add(ASTNode::range as KProperty1<T, *>)
    }
    if (withNodeType) {
        list.add(ASTNode::nodeType as KProperty1<T, *>)
    }
    return list.toList()
}

/**
 * Executes an operation on the properties definitions of a node class.
 * @param propertiesToIgnore which properties to ignore
 * @param propertyTypeOperation the operation to perform on each property.
 */
@Deprecated("Use LionWeb based reflection instead")
fun <T : Any> KClass<T>.processProperties(
    propertiesToIgnore: Set<String> = emptySet(),
    propertyTypeOperation: (PropertyTypeDescription) -> Unit
) {
    nodeProperties.forEach { p ->
        if (!propertiesToIgnore.contains(p.name)) {
            propertyTypeOperation(PropertyTypeDescription.buildFor(p))
        }
    }
}

enum class PropertyType {
    ATTRIBUTE,
    CONTAINMENT,
    REFERENCE
}

@Deprecated("Use LionWeb based reflection instead")
data class PropertyDescription(
    val name: String,
    val provideNodes: Boolean,
    val multiplicity: Multiplicity,
    val value: Any?,
    val propertyType: PropertyType
) {

    fun valueToString(): String {
        if (value == null) {
            return "null"
        }
        return if (provideNodes) {
            if (multiplicity == Multiplicity.MANY) {
                "[${(value as Collection<ASTNode>).joinToString(",") { it.nodeType }}]"
            } else {
                "${(value as ASTNode).nodeType}(...)"
            }
        } else {
            if (multiplicity == Multiplicity.MANY) {
                "[${(value as Collection<*>).joinToString(",") { it.toString() }}]"
            } else {
                value.toString()
            }
        }
    }

    val multiple: Boolean
        get() = multiplicity == Multiplicity.MANY

    companion object {

        fun multiple(property: KProperty1<in ASTNode, *>): Boolean {
            val propertyType = property.returnType
            val classifier = propertyType.classifier as? KClass<*>
            return (classifier?.isSubclassOf(Collection::class) == true)
        }

        fun optional(property: KProperty1<in ASTNode, *>): Boolean {
            val propertyType = property.returnType
            return !multiple(property) && propertyType.isMarkedNullable
        }

        fun multiplicity(property: KProperty1<in ASTNode, *>): Multiplicity {
            return when {
                multiple(property) -> Multiplicity.MANY
                optional(property) -> Multiplicity.OPTIONAL
                else -> Multiplicity.SINGULAR
            }
        }

        fun providesNodes(property: KProperty1<in ASTNode, *>): Boolean {
            val propertyType = property.returnType
            val classifier = propertyType.classifier as? KClass<*>
            return if (multiple(property)) {
                providesNodes(propertyType.arguments[0])
            } else {
                providesNodes(classifier)
            }
        }

        fun buildFor(property: KProperty1<in ASTNode, *>, node: ASTNode): PropertyDescription {
            val multiplicity = multiplicity(property)
            val provideNodes = providesNodes(property)
            return PropertyDescription(
                name = property.name,
                provideNodes = provideNodes,
                multiplicity = multiplicity,
                value = property.get(node),
                when {
                    property.isReference -> PropertyType.REFERENCE
                    provideNodes -> PropertyType.CONTAINMENT
                    else -> PropertyType.ATTRIBUTE
                }
            )
        }
    }
}

val KProperty1<in ASTNode, *>.isReference: Boolean get() =
    ((this.returnType as? KType)?.classifier as? KClass<*>) == ReferenceByName::class

private fun providesNodes(classifier: KClassifier?): Boolean {
    if (classifier == null) {
        return false
    }
    if (classifier is KClass<*>) {
        return providesNodes(classifier as? KClass<*>)
    } else {
        throw UnsupportedOperationException(
            "We are not able to determine if the classifier $classifier provides AST Nodes or not"
        )
    }
}

private fun providesNodes(kclass: KClass<*>?): Boolean {
    return kclass?.isANode() ?: false
}

/**
 * @return can [this] class be considered an AST node?
 */
fun KClass<*>.isANode(): Boolean {
    return this.isSubclassOf(ASTNode::class) || this.isMarkedAsNodeType()
}

/**
 * @return is [this] class annotated with NodeType?
 */
fun KClass<*>.isMarkedAsNodeType(): Boolean {
    return this.annotations.any { it.annotationClass == NodeType::class }
}

@Deprecated("Use LionWeb based reflection instead")
data class PropertyTypeDescription(
    val name: String,
    val provideNodes: Boolean,
    val multiple: Boolean,
    val valueType: KType
) {
    companion object {
        fun buildFor(property: KProperty1<*, *>): PropertyTypeDescription {
            val propertyType = property.returnType
            val classifier = propertyType.classifier as? KClass<*>
            val multiple = (classifier?.isSubclassOf(Collection::class) == true)
            val valueType: KType
            val provideNodes = if (multiple) {
                valueType = propertyType.arguments[0].type!!
                providesNodes(propertyType.arguments[0])
            } else {
                valueType = propertyType
                providesNodes(classifier)
            }
            return PropertyTypeDescription(
                name = property.name,
                provideNodes = provideNodes,
                multiple = multiple,
                valueType = valueType
            )
        }
    }
}

@Deprecated("Use LionWeb based reflection instead")
enum class Multiplicity {
    OPTIONAL,
    SINGULAR,
    MANY
}

private fun providesNodes(kTypeProjection: KTypeProjection): Boolean {
    val ktype = kTypeProjection.type
    return when (ktype) {
        is KClass<*> -> providesNodes(ktype as? KClass<*>)
        is KType -> providesNodes((ktype as? KType)?.classifier)
        else -> throw UnsupportedOperationException(
            "We are not able to determine if the type $ktype provides AST Nodes or not"
        )
    }
}
