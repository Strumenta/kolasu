package com.strumenta.kolasu.lionweb

import com.strumenta.kolasu.model.Multiplicity
import io.lionweb.lioncore.java.language.Classifier
import io.lionweb.lioncore.java.language.Concept
import io.lionweb.lioncore.java.language.Containment
import io.lionweb.lioncore.java.language.DataType
import io.lionweb.lioncore.java.language.Interface
import io.lionweb.lioncore.java.language.Language
import io.lionweb.lioncore.java.language.PrimitiveType
import io.lionweb.lioncore.java.language.Property
import io.lionweb.lioncore.java.language.Reference
import io.lionweb.lioncore.java.model.impl.DynamicNode
import kotlin.random.Random

// TODO: move this to LionWeb Kotlin, once that project is created

/**
 * Create a LionWeb Language with the given name.
 */
fun lwLanguage(name: String): Language {
    val cleanedName = name.lowercase().lwIDCleanedVersion()
    return Language(name, "language-$cleanedName-id", "language-$cleanedName-key", "1")
}

fun Language.addConcept(name: String): Concept {
    return Concept(this,  name, idForContainedElement(name), keyForContainedElement(name))
        .also(::addElement)
}

fun Language.addInterface(name: String): Interface {
    return Interface(this, name, idForContainedElement(name), keyForContainedElement(name))
        .also(::addElement)
}

fun Language.addPrimitiveType(name: String): PrimitiveType {
    return PrimitiveType(this, name, idForContainedElement(name))
        .apply {
            key = keyForContainedElement(name)
        }
        .also(::addElement)
}

fun Concept.addContainment(
    name: String,
    containedConcept: Concept,
    multiplicity: Multiplicity = Multiplicity.SINGULAR,
): Containment {
    return Containment().apply {
        this.name = name
        this.id = "${this@addContainment.id!!.removeSuffix("-id")}-$name-id"
        this.key = "${this@addContainment.key!!.removeSuffix("-key")}-$name-key"
        this.type = containedConcept
        // consider simplification: this.isOptional = multiplicity != Multiplicity.SINGULAR
        this.setOptional(
            when (multiplicity) {
                Multiplicity.SINGULAR -> false
                Multiplicity.MANY -> true
                Multiplicity.OPTIONAL -> true
            },
        )
        this.setMultiple(
            when (multiplicity) {
                Multiplicity.SINGULAR -> false
                Multiplicity.MANY -> true
                Multiplicity.OPTIONAL -> false
            },
        )
    }.also(::addFeature)
}

fun Concept.addProperty(
    name: String,
    type: DataType<*>,
    multiplicity: Multiplicity = Multiplicity.SINGULAR,
): Property {
    require(multiplicity != Multiplicity.MANY) { "Properties cannot have MANY multiplicity" }
    return Property().apply {
        this.name = name
        this.id = "${this@addProperty.id!!.removeSuffix("-id")}-$name-id"
        this.key = "${this@addProperty.key!!.removeSuffix("-key")}-$name-key"
        this.type = type
        this.setOptional(
            when (multiplicity) {
                Multiplicity.SINGULAR -> false
                Multiplicity.MANY -> true
                Multiplicity.OPTIONAL -> true
            },
        )
    }.also(::addFeature)
}

val Multiplicity.optional
    get() = this == Multiplicity.OPTIONAL || this == Multiplicity.MANY

val Multiplicity.multiple
    get() = this == Multiplicity.MANY

fun Concept.addReference(
    name: String,
    type: Classifier<*>,
    multiplicity: Multiplicity = Multiplicity.SINGULAR,
): Reference {
    return Reference().apply {
        this.name = name
        this.id = "${this@addReference.id!!.removeSuffix("-id")}-$name-id"
        this.key = "${this@addReference.key!!.removeSuffix("-key")}-$name-key"
        this.type = type
        this.setOptional(multiplicity.optional)
        this.setMultiple(multiplicity.multiple)
    }.also(::addFeature)
}

/**
 * Create a Dynamic Node with the given Concept and a random node ID.
 */
fun Concept.dynamicNode(nodeId: String = "node-id-rand-${Random.nextInt()}"): DynamicNode = DynamicNode(nodeId, this)

/**
 * Cleans a string for use in LIonWeb IDs
 */
fun String.lwIDCleanedVersion(): String =
    this
        .replace(".", "_")
        .replace(" ", "_")
        .replace("/", "_")

private fun Language.idPrefixForContainedElements(): String = this.id!!.removePrefix("language-").removeSuffix("-id")

private fun Language.keyPrefixForContainedElements(): String = this.key!!.removePrefix("language-").removeSuffix("-key")

fun Language.idForContainedElement(containedElementName: String): String =
    "${this.idPrefixForContainedElements()}-${containedElementName.lwIDCleanedVersion()}-id"

fun Language.keyForContainedElement(containedElementName: String): String =
    "${this.keyPrefixForContainedElements()}-${containedElementName.lwIDCleanedVersion()}-key"
