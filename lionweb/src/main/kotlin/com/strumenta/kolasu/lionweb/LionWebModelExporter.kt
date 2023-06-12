package com.strumenta.kolasu.lionweb

import com.strumenta.kolasu.model.Source
import com.strumenta.kolasu.model.allFeatures
import com.strumenta.kolasu.traversing.walk
import io.lionweb.lioncore.java.language.Concept
import io.lionweb.lioncore.java.language.Containment
import io.lionweb.lioncore.java.language.Language
import io.lionweb.lioncore.java.language.Property
import io.lionweb.lioncore.java.language.Reference
import io.lionweb.lioncore.java.model.Node
import io.lionweb.lioncore.java.model.ReferenceValue
import io.lionweb.lioncore.java.model.impl.DynamicNode
import java.lang.IllegalArgumentException

private val com.strumenta.kolasu.model.Node.positionalID: String
    get() {
        if (this.parent == null) {
            return "root"
        } else {
            TODO()
        }
    }
private val Source?.id: String
    get() {
        if (this == null) {
            return "UNKNOWN_SOURCE"
        } else {
            TODO("Source $this")
        }
    }

class LionWebModelExporter {

    private val languageExporter = LionWebLanguageExporter()
    private val nodesMapping = mutableMapOf<com.strumenta.kolasu.model.Node, Node>()

    fun correspondingLanguage(kolasuLanguage: KolasuLanguage): Language {
        return languageExporter.correspondingLanguage(kolasuLanguage)
    }

    fun recordLanguage(kolasuLanguage: KolasuLanguage) {
        languageExporter.export(kolasuLanguage)
    }

    fun export(kolasuTree: com.strumenta.kolasu.model.Node): Node {
        if (nodesMapping.containsKey(kolasuTree)) {
            return nodesMapping[kolasuTree]!!
        }
        kolasuTree.walk().forEach {
            nodesMapping.computeIfAbsent(it) {
                DynamicNode(nodeID(it), findConcept(it))
            }
        }
        kolasuTree.walk().forEach { kNode ->
            val lwNode = nodesMapping[kNode]!!
            val kFeatures = kNode.javaClass.kotlin.allFeatures()
            lwNode.concept.allFeatures().forEach { feature ->
                when (feature) {
                    is Property -> {
                        val kAttribute = kFeatures.find { it.name == feature.name }
                            as? com.strumenta.kolasu.language.Attribute
                            ?: throw IllegalArgumentException("Property ${feature.name} not found in $kNode")
                        val kValue = kNode.getAttributeValue(kAttribute)
                        lwNode.setPropertyValue(feature, kValue)
                    }
                    is Containment -> {
                        val kContainment = kFeatures.find { it.name == feature.name }
                            as com.strumenta.kolasu.language.Containment
                        val kValue = kNode.getChildren(kContainment)
                        kValue.forEach { kChild ->
                            val lwChild = nodesMapping[kChild]!!
                            lwNode.addChild(feature, lwChild)
                        }
                    }
                    is Reference -> {
                        val kReference = kFeatures.find { it.name == feature.name }
                            as com.strumenta.kolasu.language.Reference
                        val kValue = kNode.getReference(kReference)
                        val lwReferred: Node? = if (kValue.referred == null) null
                        else nodesMapping[kValue.referred!! as com.strumenta.kolasu.model.Node]!!
                        lwNode.addReferenceValue(feature, ReferenceValue(lwReferred, kValue.name))
                    }
                }
            }
        }

        return nodesMapping[kolasuTree]!!
    }

    private fun findConcept(kNode: com.strumenta.kolasu.model.Node): Concept {
        return languageExporter.toConcept(kNode.javaClass.kotlin)
    }

    private fun nodeID(kNode: com.strumenta.kolasu.model.Node): String {
        return "${kNode.source.id}-${kNode.positionalID}"
    }
}
