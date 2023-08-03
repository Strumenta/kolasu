package com.strumenta.kolasu.lionweb

import com.strumenta.kolasu.language.KolasuLanguage
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
import io.lionweb.lioncore.java.model.impl.DynamicEnumerationValue
import io.lionweb.lioncore.java.model.impl.DynamicNode
import io.lionweb.lioncore.java.serialization.JsonSerialization
import java.lang.ClassCastException
import java.lang.IllegalArgumentException
import java.util.IdentityHashMap
import kotlin.IllegalStateException
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.KParameter
import kotlin.reflect.KProperty1
import kotlin.reflect.full.memberProperties
import kotlin.reflect.full.primaryConstructor

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

/**
 * This class is able to convert between Kolasu and LionWeb models, tracking the mapping.
 */
class LionWebModelConverter {

    private val languageExporter = LionWebLanguageConverter()
    private val kolasuToLWNodesMapping = mutableMapOf<com.strumenta.kolasu.model.Node, Node>()
    private val lwToKolasuNodesMapping = mutableMapOf<Node, com.strumenta.kolasu.model.Node>()

    private fun registerMapping(kNode: com.strumenta.kolasu.model.Node, lwNode: Node) {
        kolasuToLWNodesMapping[kNode] = lwNode
        lwToKolasuNodesMapping[lwNode] = kNode
    }

    fun correspondingLanguage(kolasuLanguage: KolasuLanguage): Language {
        return languageExporter.correspondingLanguage(kolasuLanguage)
    }

    fun recordLanguage(kolasuLanguage: KolasuLanguage): Language {
        return languageExporter.export(kolasuLanguage)
    }

    fun importLanguages(lwLanguage: Language, kolasuLanguage: KolasuLanguage) {
        this.languageExporter.importLanguages(lwLanguage, kolasuLanguage)
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
        val constructor: KFunction<Any> = when {
            kClass.constructors.size == 1 -> {
                kClass.constructors.first()
            }
            kClass.primaryConstructor != null -> {
                kClass.primaryConstructor!!
            }
            else -> {
                TODO()
            }
        }
        val params = mutableMapOf<KParameter, Any?>()
        constructor.parameters.forEach { param ->
            val feature = data.concept.getFeatureByName(param.name!!)
            if (feature == null) {
                TODO()
            } else {
                when (feature) {
                    is Property -> {
                        val propValue = data.getPropertyValue(feature)
                        if (propValue is DynamicEnumerationValue) {
                            val enumeration = propValue.enumeration
                            val kClass: KClass<out Enum<*>>? = languageExporter
                                .getEnumerationsToKolasuClassesMapping()[enumeration] as? KClass<out Enum<*>>
                            if (kClass == null) {
                                throw IllegalStateException("Cannot find Kolasu class for Enumeration $enumeration")
                            }
                            val entries = kClass.java.methods.find {
                                it.name == "getEntries"
                            }!!.invoke(null) as List<Any>
                            // val entriesProp = kClass.memberProperties.find { it.name == "entries" }
                            // val fields = kClass.java.declaredFields.filter { Modifier.isStatic(it.modifiers) }.map { it.get(null) }
                            val nameProp = kClass.memberProperties.find { it.name == "name" }!! as KProperty1<Any, *>
                            val namesToFields = entries.associate { nameProp.invoke(it) as String to it }
                            // val entries = kClass.staticProperties.first().get()
                            val nameToSearch = propValue.serializedValue.split("/").last()
                            params[param] = namesToFields[nameToSearch]!!
                        } else {
                            params[param] = propValue
                        }
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
        try {
            return constructor.callBy(params) as com.strumenta.kolasu.model.Node
        } catch (e: ClassCastException) {
            throw RuntimeException("Issue instantiating using constructor $constructor with params $params", e)
        }
    }

    fun import(lwTree: Node): com.strumenta.kolasu.model.Node {
        val referencesPostponer = ReferencesPostponer()
        lwTree.thisAndAllDescendants().reversed().forEach { lwNode ->
            val kClass = languageExporter.matchingKClass(lwNode.concept)
                ?: throw RuntimeException("We do not have StarLasu AST class for LIonWeb Concept ${lwNode.concept}")
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
