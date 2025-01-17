package com.strumenta.kolasu.lionweb

import com.strumenta.kolasu.model.Multiplicity
import io.lionweb.lioncore.java.language.Concept
import io.lionweb.lioncore.java.language.DataType
import io.lionweb.lioncore.java.language.IKeyed
import io.lionweb.lioncore.java.language.Interface
import io.lionweb.lioncore.java.language.Language
import io.lionweb.lioncore.java.language.Property
import io.lionweb.lioncore.java.model.Node
import io.lionweb.lioncore.java.model.impl.DynamicNode
import kotlin.random.Random

// TODO: use equivalent methods from LionWeb Kotlin

fun Language.addInterface(name: String): Interface {
    val intf =
        Interface(
            this,
            name,
            this.idForContainedElement(name),
            this.keyForContainedElement(name),
        )
    this.addElement(intf)
    return intf
}

fun Concept.addProperty(
    name: String,
    type: DataType<*>,
    multiplicity: Multiplicity = Multiplicity.SINGULAR,
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
                },
            )
        }
    this.addFeature(property)
    return property
}

val Multiplicity.optional
    get() = this == Multiplicity.OPTIONAL || this == Multiplicity.MANY

val Multiplicity.multiple
    get() = this == Multiplicity.MANY

/**
 * Create a Dynamic Node with the given Concept and a random node ID.
 */
fun Concept.dynamicNode(nodeId: String = "node-id-rand-${Random.nextInt()}"): DynamicNode = DynamicNode(nodeId, this)

fun String.lwIDCleanedVersion(): String =
    this
        .replace(".", "_")
        .replace(" ", "_")
        .replace("/", "_")

private fun Node.idPrefixForContainedElements(): String = this.id!!.removePrefix("language-").removeSuffix("-id")

private fun IKeyed<*>.keyPrefixForContainedElements(): String =
    this
        .key!!
        .removePrefix(
            "language-",
        ).removeSuffix("-key")

fun Node.idForContainedElement(containedElementName: String): String =
    "${this.idPrefixForContainedElements()}-${containedElementName.lwIDCleanedVersion()}-id"

fun IKeyed<*>.keyForContainedElement(containedElementName: String): String =
    "${this.keyPrefixForContainedElements()}-${containedElementName.lwIDCleanedVersion()}-key"
