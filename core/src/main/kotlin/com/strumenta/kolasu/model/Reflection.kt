package com.strumenta.kolasu.model

import com.strumenta.kolasu.language.Attribute
import com.strumenta.kolasu.language.Containment
import com.strumenta.kolasu.language.Feature
import com.strumenta.kolasu.language.Reference
import com.strumenta.kolasu.testing.IgnoreChildren
import kotlin.reflect.KClass
import kotlin.reflect.KClassifier
import kotlin.reflect.KProperty1
import kotlin.reflect.KType
import kotlin.reflect.KTypeProjection
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.full.superclasses
import kotlin.reflect.full.withNullability

fun <T : BaseASTNode> T.relevantMemberProperties(
    withPosition: Boolean = false,
    withNodeType: Boolean = false,
    includeDerived: Boolean = false,
): List<KProperty1<T, *>> {
    val list =
        if (includeDerived) {
            this::class.nodeProperties.map { it as KProperty1<T, *> }.toMutableList()
        } else {
            this::class.nodeOriginalProperties.map { it as KProperty1<T, *> }.toMutableList()
        }
    if (withPosition) {
        list.add(BaseASTNode::position as KProperty1<T, *>)
    }
    if (withNodeType) {
        list.add(BaseASTNode::nodeType as KProperty1<T, *>)
    }
    return list.toList()
}

/**
 * Executes an operation on the properties definitions of a node class.
 * @param propertiesToIgnore which properties to ignore
 * @param propertyTypeOperation the operation to perform on each property.
 */
fun <T : Any> KClass<T>.processProperties(
    propertiesToIgnore: Set<String> = emptySet(),
    propertyTypeOperation: (PropertyTypeDescription) -> Unit,
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
    REFERENCE,
}

data class PropertyDescription(
    val name: String,
    val provideNodes: Boolean,
    val multiplicity: Multiplicity,
    val value: Any?,
    val propertyType: PropertyType,
    val derived: Boolean,
) {
    fun valueToString(): String {
        if (value == null) {
            return "null"
        }
        return if (provideNodes) {
            if (multiplicity == Multiplicity.MANY) {
                when (value) {
                    is IgnoreChildren<*> -> "<Ignore Children Placeholder>"
                    else -> "[${(value as Collection<BaseASTNode>).joinToString(",") { it.nodeType }}]"
                }
            } else {
                "${(value as BaseASTNode).nodeType}(...)"
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
        fun <N : BaseASTNode> multiple(property: KProperty1<N, *>): Boolean {
            val propertyType = property.returnType
            val classifier = propertyType.classifier as? KClass<*>
            return (classifier?.isSubclassOf(Collection::class) == true)
        }

        fun <N : BaseASTNode> optional(property: KProperty1<N, *>): Boolean {
            val propertyType = property.returnType
            return !multiple(property) && propertyType.isMarkedNullable
        }

        fun <N : BaseASTNode> multiplicity(property: KProperty1<N, *>): Multiplicity {
            return when {
                multiple(property) -> Multiplicity.MANY
                optional(property) -> Multiplicity.OPTIONAL
                else -> Multiplicity.SINGULAR
            }
        }

        fun <N : BaseASTNode> providesNodes(property: KProperty1<N, *>): Boolean {
            val propertyType = property.returnType
            val classifier = propertyType.classifier as? KClass<*>
            return if (multiple(property)) {
                providesNodes(propertyType.arguments[0])
            } else {
                providesNodes(classifier)
            }
        }

        fun <N : BaseASTNode> buildFor(
            property: KProperty1<N, *>,
            node: BaseASTNode,
        ): PropertyDescription {
            val multiplicity = multiplicity(property)
            val provideNodes = providesNodes(property)
            return PropertyDescription(
                name = property.name,
                provideNodes = provideNodes,
                multiplicity = multiplicity,
                value = property.get(node as N),
                when {
                    property.isReference() -> PropertyType.REFERENCE
                    provideNodes -> PropertyType.CONTAINMENT
                    else -> PropertyType.ATTRIBUTE
                },
                derived = property.findAnnotation<Derived>() != null,
            )
        }
    }
}

private fun providesNodes(classifier: KClassifier?): Boolean {
    if (classifier == null) {
        return false
    }
    if (classifier is KClass<*>) {
        return providesNodes(classifier as? KClass<*>)
    } else {
        throw UnsupportedOperationException(
            "We are not able to determine if the classifier $classifier provides AST Nodes or not",
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
    return this.isSubclassOf(BaseASTNode::class) || this.isMarkedAsNodeType()
}

val KClass<*>.isConcept: Boolean
    get() = isANode() && !this.java.isInterface

val KClass<*>.isConceptInterface: Boolean
    get() = isANode() && this.java.isInterface

/**
 * @return is [this] class annotated with NodeType?
 */
fun KClass<*>.isMarkedAsNodeType(): Boolean {
    return this.annotations.any { it.annotationClass == NodeType::class } ||
        this.superclasses.any { it.isMarkedAsNodeType() }
}

data class PropertyTypeDescription(
    val name: String,
    val provideNodes: Boolean,
    val multiple: Boolean,
    val valueType: KType,
) {
    companion object {
        fun buildFor(property: KProperty1<*, *>): PropertyTypeDescription {
            val propertyType = property.returnType
            val classifier = propertyType.classifier as? KClass<*>
            val multiple = (classifier?.isSubclassOf(Collection::class) == true)
            val valueType: KType
            val provideNodes =
                if (multiple) {
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
                valueType = valueType,
            )
        }
    }
}

enum class Multiplicity {
    OPTIONAL,
    SINGULAR,
    MANY,
}

private fun providesNodes(kTypeProjection: KTypeProjection): Boolean {
    val ktype = kTypeProjection.type
    return when (ktype) {
        is KClass<*> -> providesNodes(ktype as? KClass<*>)
        is KType -> providesNodes((ktype as? KType)?.classifier)
        else -> throw UnsupportedOperationException(
            "We are not able to determine if the type $ktype provides AST Nodes or not",
        )
    }
}

fun <N : Any> KProperty1<N, *>.isContainment(): Boolean {
    return if ((this.returnType.classifier as? KClass<*>)?.isSubclassOf(Collection::class) == true) {
        providesNodes(this.returnType.arguments[0].type!!.classifier as KClass<out BaseASTNode>)
    } else {
        providesNodes(this.returnType.classifier as KClass<out BaseASTNode>)
    }
}

fun <N : Any> KProperty1<N, *>.isReference(): Boolean {
    return this.returnType.classifier == ReferenceByName::class
}

fun <N : Any> KProperty1<N, *>.isAttribute(): Boolean {
    return !isContainment() && !isReference()
}

fun <N : BaseASTNode> KProperty1<N, *>.containedType(): KClass<out BaseASTNode> {
    require(isContainment())
    return if ((this.returnType.classifier as? KClass<*>)?.isSubclassOf(Collection::class) == true) {
        this.returnType.arguments[0].type!!.classifier as KClass<out BaseASTNode>
    } else {
        this.returnType.classifier as KClass<out BaseASTNode>
    }
}

fun <N : BaseASTNode> KProperty1<N, *>.referredType(): KClass<out BaseASTNode> {
    require(isReference())
    return this.returnType.arguments[0].type!!.classifier as KClass<out BaseASTNode>
}

fun <N : Any> KProperty1<N, *>.asContainment(): Containment {
    val multiplicity =
        when {
            (this.returnType.classifier as? KClass<*>)?.isSubclassOf(Collection::class) == true -> {
                if (this.returnType.isMarkedNullable) {
                    throw IllegalStateException(
                        "Containments should not be defined as nullable collections " +
                            "(property ${this.name})",
                    )
                }
                Multiplicity.MANY
            }

            this.returnType.isMarkedNullable -> Multiplicity.OPTIONAL
            else -> Multiplicity.SINGULAR
        }
    val type =
        if (multiplicity == Multiplicity.MANY) {
            this.returnType.arguments[0].type!!.classifier as KClass<*>
        } else {
            this.returnType.classifier as KClass<*>
        }
    return Containment(this.name, multiplicity, type)
}

fun <N : Any> KProperty1<N, *>.asReference(): Reference {
    val optional =
        when {
            (this.returnType.classifier as? KClass<*>)?.isSubclassOf(Collection::class) == true -> {
                throw IllegalStateException()
            }

            this.returnType.isMarkedNullable -> true
            else -> false
        }
    return Reference(this.name, optional, this.returnType.arguments[0].type?.classifier as KClass<*>)
}

fun <N : Any> KProperty1<N, *>.asAttribute(): Attribute {
    val optional =
        when {
            (this.returnType.classifier as? KClass<*>)?.isSubclassOf(Collection::class) == true -> {
                throw IllegalStateException("Attributes with a Collection type are not allowed (property $this)")
            }

            this.returnType.isMarkedNullable -> true
            else -> false
        }
    return Attribute(this.name, optional, this.returnType.withNullability(false))
}

private val featuresCache = mutableMapOf<KClass<*>, List<Feature>>()

fun <N : Any> KClass<N>.allFeatures(): List<Feature> {
    val res = mutableListOf<Feature>()
    res.addAll(declaredFeatures())
    supertypes.mapNotNull { (it.classifier as? KClass<*>) }.forEach { supertype ->
        res.addAll(supertype.allFeatures())
    }
    return res
}

fun <N : Any> KClass<N>.isInherited(feature: Feature): Boolean {
    this.supertypes.map { it.classifier as KClass<*> }.any { supertype ->
        supertype.allFeatures().any { f -> f.name == feature.name }
    }
    return false
}

fun <N : Any> KClass<N>.declaredFeatures(includeDerived: Boolean = false): List<Feature> {
    if (!featuresCache.containsKey(this)) {
        // Named can be used also for things which are not Node, so we treat it as a special case
        featuresCache[this] =
            if (!isANode() && this != Named::class) {
                emptyList()
            } else {
                val inheritedNamed =
                    supertypes.map { (it.classifier as? KClass<*>)?.allFeatures()?.map { it.name } ?: emptyList() }
                        .flatten()
                        .toSet()
                val notInheritedProps =
                    (if (includeDerived) nodeProperties else nodeOriginalProperties)
                        .filter { it.name !in inheritedNamed }
                notInheritedProps.map {
                    when {
                        it.isAttribute() -> {
                            it.asAttribute()
                        }

                        it.isReference() -> {
                            it.asReference()
                        }

                        it.isContainment() -> {
                            it.asContainment()
                        }

                        else -> throw IllegalStateException()
                    }
                }
            }
    }
    return featuresCache[this]!!
}
