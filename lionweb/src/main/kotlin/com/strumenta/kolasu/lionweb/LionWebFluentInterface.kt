package com.strumenta.kolasu.lionweb

import com.strumenta.kolasu.model.Multiplicity
import io.lionweb.lioncore.java.language.*
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
    val concept =
        Concept(
            this,
            name,
            this.idForContainedElement(name),
            this.keyForContainedElement(name)
        )
    this.addElement(concept)
    return concept
}

fun Language.addPrimitiveType(name: String): PrimitiveType {
    val primitiveType =
        PrimitiveType(
            this,
            name,
            this.idForContainedElement(name)
        )
    primitiveType.key = this.keyForContainedElement(name)
    this.addElement(primitiveType)
    return primitiveType
}

fun Concept.addContainment(
    name: String,
    containedConcept: Concept,
    multiplicity: Multiplicity = Multiplicity.SINGULAR
): Containment {
    val containment =
        Containment().apply {
            this.name = name
            this.id = "${this@addContainment.id!!.removeSuffix("-id")}-$name-id"
            this.key = "${this@addContainment.key!!.removeSuffix("-key")}-$name-key"
            this.type = containedConcept
            this.setOptional(
                when (multiplicity) {
                    Multiplicity.SINGULAR -> false
                    Multiplicity.MANY -> true
                    Multiplicity.OPTIONAL -> true
                }
            )
            this.setMultiple(
                when (multiplicity) {
                    Multiplicity.SINGULAR -> false
                    Multiplicity.MANY -> true
                    Multiplicity.OPTIONAL -> false
                }
            )
        }
    this.addFeature(containment)
    return containment
}

fun Concept.addProperty(
    name: String,
    type: PrimitiveType,
    multiplicity: Multiplicity = Multiplicity.SINGULAR
): Property {
    require(multiplicity != Multiplicity.MANY)
    val property =
        Property().apply {
            this.name = name
            this.id = "${this@addProperty.id!!.removeSuffix("-id")}-$name-id"
            this.key = "${this@addProperty.key!!.removeSuffix("-key")}-$name-key"
            this.type = type
            this.setOptional(
                when (multiplicity) {
                    Multiplicity.SINGULAR -> false
                    Multiplicity.MANY -> true
                    Multiplicity.OPTIONAL -> true
                }
            )
        }
    this.addFeature(property)
    return property
}

val Multiplicity.optional
    get() = this == Multiplicity.OPTIONAL || this == Multiplicity.MANY

val Multiplicity.multiple
    get() = this == Multiplicity.MANY

fun Concept.addReference(
    name: String,
    type: Classifier<*>,
    multiplicity: Multiplicity = Multiplicity.SINGULAR
): Reference {
    val reference =
        Reference().apply {
            this.name = name
            this.id = "${this@addReference.id!!.removeSuffix("-id")}-$name-id"
            this.key = "${this@addReference.key!!.removeSuffix("-key")}-$name-key"
            this.type = type
            this.setOptional(multiplicity.optional)
            this.setMultiple(multiplicity.multiple)
        }
    this.addFeature(reference)
    return reference
}

/**
 * Create a Dynamic Node with the given Concept and a random node ID.
 */
fun Concept.dynamicNode(nodeId: String = "node-id-rand-${Random.nextInt()}"): DynamicNode {
    return DynamicNode(nodeId, this)
}

fun String.lwIDCleanedVersion(): String {
    return this.replace(".", "_")
        .replace(" ", "_")
        .replace("/", "_")
}

private fun Language.idPrefixForContainedElements(): String {
    return this.id!!.removePrefix("language-").removeSuffix("-id")
}

private fun Language.keyPrefixForContainedElements(): String {
    return this.key!!.removePrefix("language-").removeSuffix("-key")
}

fun Language.idForContainedElement(containedElementName: String): String {
    return "${this.idPrefixForContainedElements()}-${containedElementName.lwIDCleanedVersion()}-id"
}

fun Language.keyForContainedElement(containedElementName: String): String {
    return "${this.keyPrefixForContainedElements()}-${containedElementName.lwIDCleanedVersion()}-key"
}
