package com.strumenta.kolasu.model

import com.strumenta.kolasu.language.BaseStarLasuLanguage
import com.strumenta.kolasu.language.ConceptLike
import com.strumenta.kolasu.language.DataType
import com.strumenta.kolasu.language.EnumType
import com.strumenta.kolasu.language.EnumerationLiteral
import com.strumenta.kolasu.language.Feature
import com.strumenta.kolasu.language.PrimitiveType
import com.strumenta.kolasu.language.StarLasuLanguage
import kotlin.reflect.KClass
import kotlin.reflect.full.allSuperclasses
import kotlin.reflect.full.findAnnotations
import kotlin.reflect.full.isSubclassOf

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

val KClass<*>.packageName: String
    get() = this.qualifiedName!!.removeSuffix(".${this.simpleName}")

fun KClass<*>.asConceptLike(language: StarLasuLanguage? = null): ConceptLike {
    if (this == NodeLike::class) {
        return BaseStarLasuLanguage.astNode
    }

    // We want to force the object to be loaded
    val languageAssociations = this.findAnnotations(LanguageAssociation::class)
    val languageClass =
        when (languageAssociations.size) {
            0 -> "${this.packageName}.StarLasuLanguageInstance"
            1 -> languageAssociations.first().language.qualifiedName!!
            else -> throw IllegalStateException()
        }
    try {
        val languageInstance: StarLasuLanguage =
            language ?: run {
                val jClass = this::class.java.classLoader.loadClass(languageClass)
                jClass.kotlin.objectInstance as StarLasuLanguage
            }
        return languageInstance.getConceptLike(this.simpleName!!)
    } catch (e: ClassNotFoundException) {
        throw IllegalStateException("No expected Language class $languageClass", e)
    }
}

fun KClass<*>.asDataType(): DataType {
    if (this.allSuperclasses.contains(Enum::class)) {
        val literals =
            (
                this
                    .members
                    .find {
                        it.name == "values"
                    }!!
                    .call() as Array<Enum<*>>
            ).map { EnumerationLiteral(it.name) }
        return EnumType(this.qualifiedName!!, literals.toMutableList())
    } else {
        return PrimitiveType(this.qualifiedName!!)
    }
}

/**
 * @return can [this] class be considered an AST node?
 */
fun KClass<*>.isANode(): Boolean = this.isSubclassOf(NodeLike::class)

val KClass<*>.isConcept: Boolean
    get() = isANode() && !this.java.isInterface

val KClass<*>.isConceptInterface: Boolean
    get() = isANode() && this.java.isInterface && this != NodeLike::class

internal fun providesNodes(kclass: KClass<*>?): Boolean = kclass?.isANode() ?: false

/**
 * Executes an operation on the properties definitions of a node class.
 * @param propertiesToIgnore which properties to ignore
 * @param propertyTypeOperation the operation to perform on each property.
 */
fun <T : Any> KClass<T>.processFeatures(
    propertiesToIgnore: Set<String> = emptySet(),
    propertyTypeOperation: (PropertyTypeDescription) -> Unit,
) {
    nodeProperties.forEach { p ->
        if (!propertiesToIgnore.contains(p.name)) {
            propertyTypeOperation(PropertyTypeDescription.buildFor(p))
        }
    }
}
