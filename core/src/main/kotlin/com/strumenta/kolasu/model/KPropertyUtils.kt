package com.strumenta.kolasu.model

import com.strumenta.kolasu.language.Containment
import com.strumenta.kolasu.language.Property
import com.strumenta.kolasu.language.Reference
import com.strumenta.kolasu.language.StarLasuLanguage
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.full.withNullability

/**
 * We may need to pass the current language to avoid loadClass to fail because this is called inside
 * an initializer
 */
fun <N : Any> KProperty1<N, *>.asReference(language: StarLasuLanguage? = null): Reference {
    val optional =
        when {
            (this.returnType.classifier as? KClass<*>)?.isSubclassOf(Collection::class) == true -> {
                throw IllegalStateException()
            }

            this.returnType.isMarkedNullable -> true
            else -> false
        }
    val kClass =
        this
            .returnType
            .arguments[0]
            .type
            ?.classifier as? KClass<*> ?: throw IllegalStateException()
    return Reference(
        this.name,
        optional,
        kClass.asConceptLike(language),
        { node -> this.get(node as N) as? ReferenceValue<*> },
    )
}

fun <N : Any> KProperty1<N, *>.asFeature(): com.strumenta.kolasu.language.Feature {
    return when {
        this.isAttribute() -> this.asAttribute()
        this.isContainment() -> this.asContainment()
        this.isReference() -> this.asReference()
        else -> throw IllegalStateException()
    }
}

fun <N : Any> KProperty1<N, *>.asAttribute(): Property {
    val optional =
        when {
            (this.returnType.classifier as? KClass<*>)?.isSubclassOf(Collection::class) == true -> {
                throw IllegalStateException("Attributes with a Collection type are not allowed (property $this)")
            }

            this.returnType.isMarkedNullable -> true
            else -> false
        }
    return Property(
        this.name,
        optional,
        (
            this
                .returnType
                .withNullability(
                    false,
                ).classifier as KClass<*>
        ).asDataType(),
        { node ->
            this.get(node as N)
        },
    )
}

fun <N : Any> KProperty1<N, *>.isReference(): Boolean {
    return this.returnType.classifier == ReferenceValue::class
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

fun <N : Any> KProperty1<N, *>.asContainment(language: StarLasuLanguage? = null): Containment {
    val multiplicity =
        when {
            (this.returnType.classifier as? KClass<*>)?.isSubclassOf(Collection::class) == true -> {
                Multiplicity.MANY
            }

            this.returnType.isMarkedNullable -> Multiplicity.OPTIONAL
            else -> Multiplicity.SINGULAR
        }
    if (multiplicity == Multiplicity.MANY && this.returnType.isMarkedNullable) {
        throw IllegalStateException(
            "Containments should not be defined as nullable collections " +
                "(property ${this.name})",
        )
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
    return Containment(this.name, multiplicity, type.asConceptLike(language), { node ->
        try {
            this.get(node as N)
        } catch (e: Exception) {
            throw RuntimeException(
                "Unable to get value for containment ${this.name} on node of type ${node.nodeType}",
                e,
            )
        }
    })
}

fun <N : Any> KProperty1<N, *>.isContainment(): Boolean =
    if ((this.returnType.classifier as? KClass<*>)?.isSubclassOf(Collection::class) == true) {
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
