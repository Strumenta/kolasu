package com.strumenta.kolasu.model

import com.strumenta.kolasu.language.Attribute
import com.strumenta.kolasu.language.Containment
import com.strumenta.kolasu.language.Feature
import com.strumenta.kolasu.language.Reference
import kotlin.reflect.KClass
import kotlin.reflect.KClassifier
import kotlin.reflect.KProperty1
import kotlin.reflect.KType
import kotlin.reflect.KTypeProjection
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.full.withNullability

fun <T : NodeLike> T.relevantMemberProperties(
    withRange: Boolean = false,
    withNodeType: Boolean = false,
): List<KProperty1<T, *>> {
    val list = this::class.nodeProperties.map { it as KProperty1<T, *> }.toMutableList()
    if (withRange) {
        list.add(NodeLike::range as KProperty1<T, *>)
    }
    if (withNodeType) {
        list.add(NodeLike::nodeType as KProperty1<T, *>)
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
) {
    fun valueToString(): String {
        if (value == null) {
            return "null"
        }
        return if (provideNodes) {
            if (multiplicity == Multiplicity.MANY) {
                "[${(value as Collection<NodeLike>).joinToString(",") { it.nodeType }}]"
            } else {
                "${(value as NodeLike).nodeType}(...)"
            }
        } else {
            if (multiplicity == Multiplicity.MANY) {
                "[${(value as Collection<*>).joinToString(",") { it.toString() }}]"
            } else {
                value.toString()
            }
        }
    }

    val isMultiple: Boolean
        get() = multiplicity == Multiplicity.MANY

    companion object {
        fun <N : NodeLike> multiple(property: KProperty1<N, *>): Boolean {
            val propertyType = property.returnType
            val classifier = propertyType.classifier as? KClass<*>
            return (classifier?.isSubclassOf(Collection::class) == true)
        }

        fun <N : NodeLike> optional(property: KProperty1<N, *>): Boolean {
            val propertyType = property.returnType
            return !multiple(property) && propertyType.isMarkedNullable
        }

        fun <N : NodeLike> multiplicity(property: KProperty1<N, *>): Multiplicity {
            return when {
                multiple(property) -> Multiplicity.MANY
                optional(property) -> Multiplicity.OPTIONAL
                else -> Multiplicity.SINGULAR
            }
        }

        fun <N : NodeLike> providesNodes(property: KProperty1<N, *>): Boolean {
            val propertyType = property.returnType
            val classifier = propertyType.classifier as? KClass<*>
            return if (multiple(property)) {
                providesNodes(propertyType.arguments[0])
            } else {
                providesNodes(classifier)
            }
        }

        fun <N : NodeLike> buildFor(
            property: KProperty1<N, *>,
            node: NodeLike,
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
    return this.isSubclassOf(NodeLike::class) || this.isMarkedAsNodeType()
}

val KClass<*>.isConcept: Boolean
    get() = isANode() && !this.java.isInterface

val KClass<*>.isConceptInterface: Boolean
    get() = isANode() && this.java.isInterface

/**
 * @return is [this] class annotated with NodeType?
 */
fun KClass<*>.isMarkedAsNodeType(): Boolean {
    return this.annotations.any { it.annotationClass == NodeType::class }
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
        providesNodes(
            this
                .returnType
                .arguments[0]
                .type!!
                .classifier as KClass<out NodeLike>,
        )
    } else {
        providesNodes(this.returnType.classifier as KClass<out NodeLike>)
    }
}

fun <N : Any> KProperty1<N, *>.isReference(): Boolean {
    return this.returnType.classifier == ReferenceByName::class
}

fun <N : Any> KProperty1<N, *>.isAttribute(): Boolean {
    return !isContainment() && !isReference()
}

fun <N : NodeLike> KProperty1<N, *>.containedType(): KClass<out NodeLike> {
    require(isContainment())
    return if ((this.returnType.classifier as? KClass<*>)?.isSubclassOf(Collection::class) == true) {
        this
            .returnType
            .arguments[0]
            .type!!
            .classifier as KClass<out NodeLike>
    } else {
        this.returnType.classifier as KClass<out NodeLike>
    }
}

fun <N : NodeLike> KProperty1<N, *>.referredType(): KClass<out NodeLike> {
    require(isReference())
    return this
        .returnType
        .arguments[0]
        .type!!
        .classifier as KClass<out NodeLike>
}

fun <N : Any> KProperty1<N, *>.asContainment(): Containment {
    val multiplicity =
        when {
            (this.returnType.classifier as? KClass<*>)?.isSubclassOf(Collection::class) == true -> {
                Multiplicity.MANY
            }

            this.returnType.isMarkedNullable -> Multiplicity.OPTIONAL
            else -> Multiplicity.SINGULAR
        }
    val type =
        if (multiplicity == Multiplicity.MANY) {
            this
                .returnType
                .arguments[0]
                .type!!
                .classifier as KClass<*>
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
    return Reference(
        this.name,
        optional,
        this
            .returnType
            .arguments[0]
            .type
            ?.classifier as KClass<*>,
    )
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

fun <N : Any> KClass<N>.declaredFeatures(): List<Feature> {
    if (!featuresCache.containsKey(this)) {
        // Named can be used also for things which are not Node, so we treat it as a special case
        featuresCache[this] =
            if (!isANode() && this != Named::class) {
                emptyList()
            } else {
                val inheritedNamed =
                    supertypes
                        .map { (it.classifier as? KClass<*>)?.allFeatures()?.map { it.name } ?: emptyList() }
                        .flatten()
                        .toSet()
                val notInheritedProps = nodeProperties.filter { it.name !in inheritedNamed }
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
