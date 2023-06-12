package com.strumenta.kolasu.lionweb

import com.strumenta.kolasu.model.Source
import com.strumenta.kolasu.model.allFeatures
import com.strumenta.kolasu.model.containingProperty
import com.strumenta.kolasu.model.indexInContainingProperty
import com.strumenta.kolasu.traversing.walk
import io.lionweb.lioncore.java.language.Concept
import io.lionweb.lioncore.java.language.Containment
import io.lionweb.lioncore.java.language.Language
import io.lionweb.lioncore.java.language.Property
import io.lionweb.lioncore.java.language.Reference
import io.lionweb.lioncore.java.model.Node
import io.lionweb.lioncore.java.model.ReferenceValue
import io.lionweb.lioncore.java.model.impl.DynamicNode
import io.lionweb.lioncore.java.serialization.JsonSerialization
import java.lang.IllegalArgumentException

private val com.strumenta.kolasu.model.Node.positionalID: String
    get() {
        return if (this.parent == null) {
            "root"
        } else {
            val cp = this.containingProperty()!!
            val postfix = if (cp.multiple) "${cp.name}[${this.indexInContainingProperty()!!}]" else cp.name
            "${this.parent!!.positionalID}-${postfix}"
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

class LionWebModelImporterAndExporter {

    private val languageExporter = LionWebLanguageExporter()
    private val kolasuToLWNodesMapping = mutableMapOf<com.strumenta.kolasu.model.Node, Node>()
    private val lwToKolasuNodesMapping = mutableMapOf<Node,com.strumenta.kolasu.model.Node>()

    private fun registerMapping(kNode: com.strumenta.kolasu.model.Node, lwNode: Node) {
        kolasuToLWNodesMapping[kNode] = lwNode
        lwToKolasuNodesMapping[lwNode] = kNode
    }

    fun correspondingLanguage(kolasuLanguage: KolasuLanguage): Language {
        return languageExporter.correspondingLanguage(kolasuLanguage)
    }

    fun recordLanguage(kolasuLanguage: KolasuLanguage) {
        languageExporter.export(kolasuLanguage)
    }

    fun export(kolasuTree: com.strumenta.kolasu.model.Node): Node {
        if (kolasuToLWNodesMapping.containsKey(kolasuTree)) {
            return kolasuToLWNodesMapping[kolasuTree]!!
        }
        kolasuTree.walk().forEach {
            if (!kolasuToLWNodesMapping.containsKey(it)) {
                val lwNode = DynamicNode(nodeID(it), findConcept(it))
                registerMapping(it, lwNode)
            }
        }
        kolasuTree.walk().forEach { kNode ->
            val lwNode = kolasuToLWNodesMapping[kNode]!!
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
                            val lwChild = kolasuToLWNodesMapping[kChild]!!
                            lwNode.addChild(feature, lwChild)
                        }
                    }
                    is Reference -> {
                        val kReference = kFeatures.find { it.name == feature.name }
                            as com.strumenta.kolasu.language.Reference
                        val kValue = kNode.getReference(kReference)
                        val lwReferred: Node? = if (kValue.referred == null) null
                        else kolasuToLWNodesMapping[kValue.referred!! as com.strumenta.kolasu.model.Node]!!
                        lwNode.addReferenceValue(feature, ReferenceValue(lwReferred, kValue.name))
                    }
                }
            }
        }

        return kolasuToLWNodesMapping[kolasuTree]!!
    }

    fun import(lwTree: Node): com.strumenta.kolasu.model.Node {
        lwTree.thisAndAllDescendants().forEach { lwNode ->
            val kClass = languageExporter.getConceptsToKolasuClassesMapping()[lwNode.concept]!!
            val kNode: com.strumenta.kolasu.model.Node = TODO()
            registerMapping(kNode, lwNode)
        }
        lwTree.thisAndAllDescendants().forEach { lwNode ->
            val kNode: com.strumenta.kolasu.model.Node = lwToKolasuNodesMapping[lwNode]!!
            TODO()
        }
        return lwToKolasuNodesMapping[lwTree]!!
    }

    private fun findConcept(kNode: com.strumenta.kolasu.model.Node): Concept {
        return languageExporter.toConcept(kNode.javaClass.kotlin)
    }

    private fun nodeID(kNode: com.strumenta.kolasu.model.Node): String {
        return "${kNode.source.id}-${kNode.positionalID}"
    }

    fun unserializeToNodes(json: String): List<Node> {
        val js = JsonSerialization.getStandardSerialization()
        languageExporter.knownLWLanguages().forEach {
            js.conceptResolver.registerLanguage(it)
        }
        js.nodeInstantiator.enableDynamicNodes()
//        languageExporter.getConceptsToKolasuClassesMapping().forEach { entry ->
//            val lwFeaturesContainer = entry.key
//            js.nodeInstantiator.registerCustomUnserializer(lwFeaturesContainer.id, object : NodeInstantiator<>)
//        }
        return js.unserializeToNodes(json)
    }
}
