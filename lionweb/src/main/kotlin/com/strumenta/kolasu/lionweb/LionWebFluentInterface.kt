package com.strumenta.kolasu.lionweb

import com.strumenta.kolasu.model.Multiplicity
import io.lionweb.lioncore.java.language.Concept
import io.lionweb.lioncore.java.language.IKeyed
import io.lionweb.lioncore.java.model.Node
import io.lionweb.lioncore.java.model.impl.DynamicNode
import kotlin.random.Random

val Multiplicity.optional
    get() = this == Multiplicity.OPTIONAL || this == Multiplicity.MANY

val Multiplicity.multiple
    get() = this == Multiplicity.MANY

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

private fun Node.idPrefixForContainedElements(): String {
    return this.id!!.removePrefix("language-").removeSuffix("-id")
}

private fun IKeyed<*>.keyPrefixForContainedElements(): String {
    return this.key!!.removePrefix("language-").removeSuffix("-key")
}

fun Node.idForContainedElement(containedElementName: String): String {
    return "${this.idPrefixForContainedElements()}-${containedElementName.lwIDCleanedVersion()}-id"
}

fun IKeyed<*>.keyForContainedElement(containedElementName: String): String {
    return "${this.keyPrefixForContainedElements()}-${containedElementName.lwIDCleanedVersion()}-key"
}
