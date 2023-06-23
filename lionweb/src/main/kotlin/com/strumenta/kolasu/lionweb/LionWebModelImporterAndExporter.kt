package com.strumenta.kolasu.lionweb

import com.strumenta.kolasu.model.FileSource
import com.strumenta.kolasu.model.PossiblyNamed
import com.strumenta.kolasu.model.ReferenceByName
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
import java.lang.IllegalStateException
import java.util.IdentityHashMap
import kotlin.reflect.KClass
import kotlin.reflect.KParameter

private val com.strumenta.kolasu.model.Node.positionalID: String
    get() {
        return if (this.parent == null) {
            "root"
        } else {
            val cp = this.containingProperty()!!
            val postfix = if (cp.multiple) "${cp.name}_${this.indexInContainingProperty()!!}" else cp.name
            "${this.parent!!.positionalID}_$postfix"
        }
    }
private val Source?.id: String
    get() {
        return if (this == null) {
            "UNKNOWN_SOURCE"
        } else if (this is FileSource) {
            "file_${this.file.path.replace('.', '-').replace('/', '-')}"
        } else {
            TODO("Unable to generate ID for Source $this (${this.javaClass.canonicalName})")
        }
    }

class LionWebModelImporterAndExporter {

    private val languageExporter = LionWebLanguageExporter()
    private val kolasuToLWNodesMapping = mutableMapOf<com.strumenta.kolasu.model.Node, Node>()
    private val lwToKolasuNodesMapping = mutableMapOf<Node, com.strumenta.kolasu.model.Node>()

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

    private class ReferencesPostponer {

        private val values = IdentityHashMap<ReferenceByName<PossiblyNamed>, Node?>()
        fun registerPostponedReference(referenceByName: ReferenceByName<PossiblyNamed>, referred: Node?) {
            values[referenceByName] = referred
        }

        fun populateReferences(lwToKolasuNodesMapping: Map<Node, com.strumenta.kolasu.model.Node>) {
            values.forEach { entry ->
                if (entry.value == null) {
                    entry.key.referred = null
                } else {
                    entry.key.referred = lwToKolasuNodesMapping[entry.value]!! as PossiblyNamed
                }
            }
        }
    }

    private fun instantiate(kClass: KClass<*>, data: Node, referencesPostponer: ReferencesPostponer):
        com.strumenta.kolasu.model.Node {
        if (kClass.constructors.size == 1) {
            val constructor = kClass.constructors.first()
            val params = mutableMapOf<KParameter, Any?>()
            constructor.parameters.forEach { param ->
                val feature = data.concept.getFeatureByName(param.name!!)
                if (feature == null) {
                    TODO()
                } else {
                    when (feature) {
                        is Property -> {
                            params[param] = data.getPropertyValue(feature)
                        }
                        is Reference -> {
                            val referenceValues = data.getReferenceValues(feature)
                            when {
                                referenceValues.size > 1 -> {
                                    throw IllegalStateException()
                                }
                                referenceValues.size == 0 -> {
                                    params[param] = null
                                }
                                referenceValues.size == 1 -> {
                                    val rf = referenceValues.first()
                                    val referenceByName = ReferenceByName<PossiblyNamed>(rf.resolveInfo!!, null)
                                    referencesPostponer.registerPostponedReference(referenceByName, rf.referred)
                                    params[param] = referenceByName
                                }
                            }
                        }
                        is Containment -> {
                            val lwChildren = data.getChildren(feature)
                            if (feature.isMultiple) {
                                val kChildren = lwChildren.map { lwToKolasuNodesMapping[it]!! }
                                params[param] = kChildren
                            } else {
                                // Given we navigate the tree in reverse the child should have been already
                                // instantiated
                                val lwChild: Node? = if (lwChildren.size == 0) {
                                    null
                                } else if (lwChildren.size == 1) {
                                    lwChildren.first()
                                } else {
                                    throw IllegalStateException()
                                }
                                val kChild = if (lwChild == null) null else (
                                    lwToKolasuNodesMapping[lwChild]
                                        ?: throw IllegalStateException(
                                            "Unable to find Kolasu Node corresponding to $lwChild"
                                        )
                                    )
                                params[param] = kChild
                            }
                        }
                        else -> throw IllegalStateException()
                    }
                }
            }
            return constructor.callBy(params) as com.strumenta.kolasu.model.Node
        } else {
            TODO()
        }
    }

    fun import(lwTree: Node): com.strumenta.kolasu.model.Node {
        val referencesPostponer = ReferencesPostponer()
        lwTree.thisAndAllDescendants().reversed().forEach { lwNode ->
            val kClass = languageExporter.getConceptsToKolasuClassesMapping()[lwNode.concept]!!
            val kNode: com.strumenta.kolasu.model.Node = instantiate(kClass, lwNode, referencesPostponer)
            registerMapping(kNode, lwNode)
        }
        lwTree.thisAndAllDescendants().forEach { lwNode ->
            val kNode: com.strumenta.kolasu.model.Node = lwToKolasuNodesMapping[lwNode]!!
            // TODO populate values not already set at construction time
        }
        referencesPostponer.populateReferences(lwToKolasuNodesMapping)
        return lwToKolasuNodesMapping[lwTree]!!
    }

    private fun findConcept(kNode: com.strumenta.kolasu.model.Node): Concept {
        return languageExporter.toConcept(kNode.javaClass.kotlin)
    }

    private fun nodeID(kNode: com.strumenta.kolasu.model.Node): String {
        return "${kNode.source.id}_${kNode.positionalID}"
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
