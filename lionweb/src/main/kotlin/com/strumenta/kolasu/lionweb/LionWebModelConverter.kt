package com.strumenta.kolasu.lionweb

import com.strumenta.kolasu.ids.IDGenerationException
import com.strumenta.kolasu.ids.NodeIdProvider
import com.strumenta.kolasu.ids.SimpleSourceIdProvider
import com.strumenta.kolasu.ids.SourceShouldBeSetException
import com.strumenta.kolasu.language.Feature
import com.strumenta.kolasu.language.KolasuLanguage
import com.strumenta.kolasu.language.StarLasuLanguage
import com.strumenta.kolasu.model.FileSource
import com.strumenta.kolasu.model.Multiplicity
import com.strumenta.kolasu.model.NodeDestination
import com.strumenta.kolasu.model.NodeLike
import com.strumenta.kolasu.model.NodeOrigin
import com.strumenta.kolasu.model.PossiblyNamed
import com.strumenta.kolasu.model.Range
import com.strumenta.kolasu.model.Source
import com.strumenta.kolasu.model.allFeatures
import com.strumenta.kolasu.model.asContainment
import com.strumenta.kolasu.model.assignParents
import com.strumenta.kolasu.model.containingContainment
import com.strumenta.kolasu.model.indexInContainingProperty
import com.strumenta.kolasu.model.isAttribute
import com.strumenta.kolasu.model.isContainment
import com.strumenta.kolasu.model.isReference
import com.strumenta.kolasu.model.nodeOriginalProperties
import com.strumenta.kolasu.parsing.KolasuToken
import com.strumenta.kolasu.parsing.ParsingResult
import com.strumenta.kolasu.transformation.FailingASTTransformation
import com.strumenta.kolasu.transformation.MissingASTTransformation
import com.strumenta.kolasu.transformation.PlaceholderASTTransformation
import com.strumenta.kolasu.traversing.walk
import com.strumenta.kolasu.validation.Issue
import com.strumenta.kolasu.validation.IssueSeverity
import com.strumenta.kolasu.validation.IssueType
import io.lionweb.lioncore.java.language.Classifier
import io.lionweb.lioncore.java.language.Concept
import io.lionweb.lioncore.java.language.Containment
import io.lionweb.lioncore.java.language.Enumeration
import io.lionweb.lioncore.java.language.EnumerationLiteral
import io.lionweb.lioncore.java.language.Language
import io.lionweb.lioncore.java.language.LionCoreBuiltins
import io.lionweb.lioncore.java.language.PrimitiveType
import io.lionweb.lioncore.java.language.Property
import io.lionweb.lioncore.java.language.Reference
import io.lionweb.lioncore.java.model.AnnotationInstance
import io.lionweb.lioncore.java.model.Node
import io.lionweb.lioncore.java.model.impl.AbstractClassifierInstance
import io.lionweb.lioncore.java.model.impl.DynamicAnnotationInstance
import io.lionweb.lioncore.java.model.impl.DynamicNode
import io.lionweb.lioncore.java.model.impl.EnumerationValue
import io.lionweb.lioncore.java.model.impl.EnumerationValueImpl
import io.lionweb.lioncore.java.model.impl.ProxyNode
import io.lionweb.lioncore.java.serialization.AbstractSerialization
import io.lionweb.lioncore.java.serialization.JsonSerialization
import io.lionweb.lioncore.java.serialization.SerializationProvider
import io.lionweb.lioncore.java.utils.CommonChecks
import io.lionweb.lioncore.kotlin.BaseNode
import io.lionweb.lioncore.kotlin.MetamodelRegistry
import io.lionweb.lioncore.kotlin.getChildrenByContainmentName
import io.lionweb.lioncore.kotlin.getOnlyChildByContainmentName
import java.util.IdentityHashMap
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass
import kotlin.reflect.KMutableProperty
import kotlin.reflect.KParameter
import kotlin.reflect.full.primaryConstructor
import com.strumenta.kolasu.model.ReferenceValue as KReferenceValue
import io.lionweb.lioncore.java.language.Feature as LWFeature
import io.lionweb.lioncore.java.model.ReferenceValue as LWReferenceValue

interface PrimitiveValueSerialization<E> {
    fun serialize(value: E): String

    fun deserialize(serialized: String): E
}

interface NodeResolver {
    fun resolve(nodeID: String): KNode?
}

class DummyNodeResolver : NodeResolver {
    override fun resolve(nodeID: String): KNode? = null
}

/**
 * This class is able to convert between Kolasu and LionWeb models, tracking the mapping.
 *
 * This class is thread-safe.
 *
 * @param nodeIdProvider logic to be used to associate IDs to Kolasu nodes when exporting them to LionWeb
 */
class LionWebModelConverter(
    var nodeIdProvider: NodeIdProvider = StructuralLionWebNodeIdProvider(),
    initialLanguageConverter: LionWebLanguageConverter = LionWebLanguageConverter(),
) {
    companion object {
        private val kFeaturesCache = mutableMapOf<Class<*>, Map<String, Feature>>()
        private val lwFeaturesCache = mutableMapOf<Classifier<*>, Map<String, LWFeature<*>>>()

        fun lwFeatureByName(
            classifier: Classifier<*>,
            featureName: String,
        ): LWFeature<*>? {
            return lwFeaturesCache.getOrPut(classifier) {
                classifier.allFeatures().associateBy { it.name!! }
            }[featureName]
        }
    }

    private val languageConverter = initialLanguageConverter

    /**
     * We mostly map Kolasu Nodes to LionWeb Nodes, but we also map things that are not Kolasu Nodes but are nodes
     * for LionWeb (this used to be the case for Positions and Points).
     */
    private val nodesMapping = BiMap<Any, LWNode>(usingIdentity = true)
    private val primitiveValueSerializations = ConcurrentHashMap<KClass<*>, PrimitiveValueSerialization<*>>()

    var externalNodeResolver: NodeResolver = DummyNodeResolver()

    fun clearNodesMapping() {
        nodesMapping.clear()
    }

    fun <E : Any> registerPrimitiveValueSerialization(
        kClass: KClass<E>,
        primitiveValueSerialization: PrimitiveValueSerialization<E>,
    ) {
        primitiveValueSerializations[kClass] = primitiveValueSerialization
    }

    fun correspondingLanguage(kolasuLanguage: KolasuLanguage): Language {
        synchronized(languageConverter) {
            return languageConverter.correspondingLanguage(kolasuLanguage)
        }
    }

    fun exportLanguageToLionWeb(kolasuLanguage: KolasuLanguage): Language {
        synchronized(languageConverter) {
            return languageConverter.exportToLionWeb(kolasuLanguage)
        }
    }

    fun exportLanguageToLionWeb(starLasuLanguage: StarLasuLanguage): Language {
        synchronized(languageConverter) {
            return languageConverter.exportToLionWeb(starLasuLanguage)
        }
    }

    fun associateLanguages(
        lwLanguage: Language,
        kolasuLanguage: KolasuLanguage,
    ) {
        synchronized(languageConverter) {
            this.languageConverter.associateLanguages(lwLanguage, kolasuLanguage)
        }
    }

    fun exportModelToLionWeb(
        kolasuTree: KNode,
        nodeIdProvider: NodeIdProvider = this.nodeIdProvider,
        considerParent: Boolean = true,
    ): LWNode {
        kolasuTree.assignParents()
        val myIDManager =
            object {
                private val cache = IdentityHashMap<KNode, String>()

                fun nodeId(kNode: KNode): String {
                    return cache.getOrPut(kNode) {
                        val id = nodeIdProvider.id(kNode)
                        if (!CommonChecks.isValidID(id)) {
                            throw RuntimeException("We got an invalid Node ID from $nodeIdProvider for $id")
                        }
                        id
                    }
                }

                override fun toString(): String = "Caching ID Manager in front of $nodeIdProvider"
            }

        if (!nodesMapping.containsA(kolasuTree)) {
            kolasuTree.walk().forEach { kNode ->
                if (!nodesMapping.containsA(kNode)) {
                    val nodeID = myIDManager.nodeId(kNode)
                    if (!CommonChecks.isValidID(nodeID)) {
                        throw RuntimeException(
                            "We generated an invalid Node ID, using $myIDManager in $kNode. Node ID: $nodeID",
                        )
                    }
                    val lwNode = DynamicNode(nodeID, findConcept(kNode))
                    associateNodes(kNode, lwNode)
                }
            }
            kolasuTree.walk().forEach { kNode ->
                val lwNode = nodesMapping.byA(kNode)!!
                if (!CommonChecks.isValidID(lwNode.id)) {
                    throw RuntimeException(
                        "Cannot export AST to LionWeb as we got an invalid Node ID: ${lwNode.id}. " +
                            "It was produced while exporting this Kolasu Node: $kNode",
                    )
                }
                val kFeatures =
                    kFeaturesCache.getOrPut(kNode.javaClass) {
                        kNode
                            .javaClass
                            .kotlin
                            .allFeatures()
                            .associateBy { it.name }
                    }
                val lwFeatures =
                    lwFeaturesCache.getOrPut(lwNode.classifier) {
                        lwNode.classifier.allFeatures().associateBy { it.name!! }
                    }
                lwFeatures.values.forEach { feature ->
                    when (feature) {
                        is Property -> {
                            if (feature == StarLasuLWLanguage.ASTNodeRange) {
                                lwNode.setPropertyValue(StarLasuLWLanguage.ASTNodeRange, kNode.range)
                            } else {
                                val kAttribute =
                                    kFeatures[feature.name]
                                        as? com.strumenta.kolasu.language.Property
                                        ?: throw IllegalArgumentException(
                                            "Property ${feature.name} not found in $kNode",
                                        )
                                val kValue = kNode.getPropertySimpleValue<Any>(kAttribute)
                                if (kValue is Enum<*>) {
                                    setEnumProperty(lwNode, feature, kValue)
                                } else {
                                    lwNode.setPropertyValue(feature, kValue)
                                }
                            }
                        }

                        is Containment -> {
                            try {
                                val kContainment =
                                    (
                                        kFeatures[feature.name] ?: throw IllegalStateException(
                                            "Cannot find containment for ${feature.name} when considering " +
                                                "node $kNode",
                                        )
                                    )
                                        as com.strumenta.kolasu.language.Containment
                                val kValue = kNode.getChildren(kContainment)
                                kValue.forEach { kChild ->
                                    val lwChild = nodesMapping.byA(kChild)!!
                                    lwNode.addChild(feature, lwChild)
                                }
                            } catch (e: Exception) {
                                throw RuntimeException("Issue while processing containment ${feature.name}", e)
                            }
                        }
                        is Reference -> {
                            if (feature == StarLasuLWLanguage.ASTNodeOriginalNode) {
                                val origin = kNode.origin
                                if (origin is NodeOrigin) {
                                    val targetID = myIDManager.nodeId(origin.node)
                                    setOriginalNode(lwNode, targetID)
                                } else if (origin is PlaceholderASTTransformation) {
                                    if (lwNode is AbstractClassifierInstance<*>) {
                                        val instance =
                                            DynamicAnnotationInstance(
                                                "${lwNode.id}_placeholder_annotation",
                                                StarLasuLWLanguage.PlaceholderNode,
                                            )
                                        if (origin.origin is NodeOrigin) {
                                            val targetID = myIDManager.nodeId((origin.origin as NodeOrigin).node)
                                            setOriginalNode(lwNode, targetID)
                                        }
                                        setPlaceholderNodeType(instance, origin.javaClass.kotlin)
                                        instance.setPropertyValue(
                                            StarLasuLWLanguage.PlaceholderNodeMessageProperty,
                                            origin.message,
                                        )
                                        lwNode.addAnnotation(instance)
                                    } else {
                                        throw Exception(
                                            "MissingASTTransformation origin not supported on nodes " +
                                                "that are not AbstractClassifierInstances: $lwNode",
                                        )
                                    }
                                }
                            } else if (feature == StarLasuLWLanguage.ASTNodeTranspiledNodes) {
                                val destinationNodes = mutableListOf<KNode>()
                                kNode.destinations.forEach { kd ->
                                    if (kd is NodeDestination) {
                                        destinationNodes.add(kd.node)
                                    }
                                }
                                val referenceValues =
                                    destinationNodes.map { destinationNode ->
                                        val targetID = myIDManager.nodeId(destinationNode)
                                        LWReferenceValue(ProxyNode(targetID), null)
                                    }
                                lwNode.setReferenceValues(StarLasuLWLanguage.ASTNodeTranspiledNodes, referenceValues)
                            } else {
                                val kReference =
                                    kFeatures[feature.name]
                                        as com.strumenta.kolasu.language.Reference
                                val kValue = kNode.getReference(kReference)
                                if (kValue == null) {
                                    lwNode.addReferenceValue(feature, null)
                                } else {
                                    when {
                                        kValue.isRetrieved -> {
                                            val kReferred =
                                                (
                                                    kValue.referred ?: throw IllegalStateException(
                                                        "Reference " +
                                                            "retrieved but referred is empty",
                                                    )
                                                ) as KNode
                                            // We may have a reference to a Kolasu Node that we are not exporting, and for
                                            // which we have therefore no LionWeb node. In that case, if we have the
                                            // identifier, we can produce a ProxyNode instead
                                            val lwReferred: Node =
                                                nodesMapping.byA(kReferred) ?: ProxyNode(
                                                    kValue.identifier
                                                        ?: throw java.lang.IllegalStateException(
                                                            "Identifier of reference target " +
                                                                "value not set. Referred: $kReferred, " +
                                                                "reference holder: $kNode",
                                                        ),
                                                )
                                            lwNode.addReferenceValue(feature, LWReferenceValue(lwReferred, kValue.name))
                                        }
                                        kValue.isResolved -> {
                                            // This is tricky, as we need to set a LW Node, but we have just an identifier...
                                            val lwReferred: Node =
                                                DynamicNode(kValue.identifier!!, LionCoreBuiltins.getNode())
                                            lwNode.addReferenceValue(feature, LWReferenceValue(lwReferred, kValue.name))
                                        }
                                        else -> {
                                            lwNode.addReferenceValue(feature, LWReferenceValue(null, kValue.name))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        val result = nodesMapping.byA(kolasuTree)!!
        if (considerParent && kolasuTree.parent != null) {
            val parentNodeId =
                try {
                    nodeIdProvider.id(kolasuTree.parent!!)
                } catch (e: IDGenerationException) {
                    throw IDGenerationException(
                        "Cannot produce an ID for ${kolasuTree.parent}, which was needed to " +
                            "create a ProxyNode",
                        e,
                    )
                }
            (result as DynamicNode).parent = ProxyNode(parentNodeId)
        }
        return result
    }

    private fun setEnumProperty(
        lwNode: LWNode,
        feature: Property,
        kValue: Enum<*>,
    ) {
        val kClass: EnumKClass = kValue::class
        val enumeration =
            languageConverter.getKolasuClassesToEnumerationsMapping()[kClass]
                ?: throw IllegalStateException("No enumeration for enum class $kClass")
        val enumerationLiteral =
            enumeration.literals.find { it.name == kValue.name }
                ?: throw IllegalStateException(
                    "No enumeration literal with name ${kValue.name} " +
                        "in enumeration $enumeration",
                )
        lwNode.setPropertyValue(feature, EnumerationValueImpl(enumerationLiteral))
    }

    private fun setOriginalNode(
        lwNode: LWNode,
        targetID: String,
    ) {
        lwNode.setReferenceValues(
            StarLasuLWLanguage.ASTNodeOriginalNode,
            listOf(
                LWReferenceValue(ProxyNode(targetID), null),
            ),
        )
    }

    private fun setPlaceholderNodeType(
        placeholderAnnotation: AnnotationInstance,
        kClass: KClass<out PlaceholderASTTransformation>,
    ) {
        val enumerationLiteral: EnumerationLiteral =
            when (kClass) {
                MissingASTTransformation::class ->
                    StarLasuLWLanguage.PlaceholderNodeType.literals.find {
                        it.name == "MissingASTTransformation"
                    }!!
                FailingASTTransformation::class ->
                    StarLasuLWLanguage.PlaceholderNodeType.literals.find {
                        it.name == "FailingASTTransformation"
                    }!!
                else -> TODO()
            }

        placeholderAnnotation.setPropertyValue(
            StarLasuLWLanguage.PlaceholderNodeTypeProperty,
            EnumerationValueImpl(enumerationLiteral),
        )
    }

    fun importModelFromLionWeb(lwTree: LWNode): Any {
        val referencesPostponer = ReferencesPostponer()
        lwTree.thisAndAllDescendants().reversed().forEach { lwNode ->
            val kClass =
                synchronized(languageConverter) {
                    languageConverter.correspondingKolasuClass(lwNode.classifier)
                        ?: throw RuntimeException(
                            "We do not have StarLasu AST class for " +
                                "LIonWeb Concept ${lwNode.classifier}",
                        )
                }
            try {
                val instantiated = instantiate(kClass, lwNode, referencesPostponer)
                if (instantiated is KNode) {
                    instantiated.assignParents()
                    nodeIdProvider.registerMapping(instantiated, lwNode.id!!)
                }
                associateNodes(instantiated, lwNode)
            } catch (e: RuntimeException) {
                throw RuntimeException("Issue instantiating $kClass from LionWeb node $lwNode", e)
            }
        }
        val placeholderNodes = mutableMapOf<KNode, (KNode) -> Unit>()
        lwTree.thisAndAllDescendants().forEach { lwNode ->
            val kNode = nodesMapping.byB(lwNode)!!
            if (kNode is KNode) {
                val lwRange = lwNode.getPropertyValue(StarLasuLWLanguage.ASTNodeRange)
                if (lwRange != null) {
                    kNode.range = lwRange as Range
                }
                val originalNodes = lwNode.getReferenceValues(StarLasuLWLanguage.ASTNodeOriginalNode)
                if (originalNodes.isNotEmpty()) {
                    require(originalNodes.size == 1)
                    val originalNode = originalNodes.first()
                    val originalNodeID = originalNode.referredID
                    require(originalNodeID != null)
                    referencesPostponer.registerPostponedOriginReference(kNode, originalNodeID)
                }
                val placeholderNodeAnnotation =
                    lwNode.annotations.find {
                        it.classifier == StarLasuLWLanguage.PlaceholderNode
                    }
                if (placeholderNodeAnnotation != null) {
                    val placeholderType =
                        (
                            placeholderNodeAnnotation.getPropertyValue(
                                StarLasuLWLanguage.PlaceholderNodeTypeProperty,
                            ) as EnumerationValue
                        ).enumerationLiteral
                    val placeholderMessage =
                        placeholderNodeAnnotation.getPropertyValue(
                            StarLasuLWLanguage.PlaceholderNodeMessageProperty,
                        ) as String
                    when (placeholderType.name) {
                        "MissingASTTransformation" -> {
                            placeholderNodes[kNode] = { kNode ->
                                kNode.origin =
                                    MissingASTTransformation(
                                        origin = kNode.origin,
                                        transformationSource = kNode.origin as? KNode,
                                        expectedType = null,
                                    )
                            }
                        }
                        "FailingASTTransformation" -> {
                            placeholderNodes[kNode] = { kNode ->
                                kNode.origin =
                                    FailingASTTransformation(
                                        origin = kNode.origin,
                                        message = placeholderMessage,
                                    )
                            }
                        }
                        else -> TODO()
                    }
                }
                val transpiledNodes = lwNode.getReferenceValues(StarLasuLWLanguage.ASTNodeTranspiledNodes)
                if (transpiledNodes.isNotEmpty()) {
                    val transpiledNodeIDs = transpiledNodes.map { it.referredID!! }
                    referencesPostponer.registerPostponedTranspiledReference(kNode, transpiledNodeIDs)
                }
            }
        }
        referencesPostponer.populateReferences(nodesMapping, externalNodeResolver)
        // We want to handle the origin for placeholder nodes AFTER references, to override the origins
        // set during the population of references
        placeholderNodes.entries.forEach { entry ->
            entry.value.invoke(entry.key)
        }
        return nodesMapping.byB(lwTree)!!
    }

    fun prepareSerialization(
        serialization: AbstractSerialization =
            SerializationProvider.getStandardJsonSerialization(),
    ): AbstractSerialization {
        StarLasuLWLanguage
        MetamodelRegistry.prepareJsonSerialization(serialization)
        synchronized(languageConverter) {
            languageConverter.knownLWLanguages().forEach {
                serialization.primitiveValuesSerialization.registerLanguage(it)
                serialization.classifierResolver.registerLanguage(it)
            }
            languageConverter.knownKolasuLanguages().forEach { kolasuLanguage ->
                kolasuLanguage.primitiveClasses.forEach { primitiveClass ->
                    if (primitiveValueSerializations.containsKey(primitiveClass)) {
                        val lwPrimitiveType: PrimitiveType =
                            languageConverter
                                .getKolasuClassesToPrimitiveTypesMapping()[primitiveClass]
                                ?: throw IllegalStateException(
                                    "No Primitive Type found associated to primitive value class " +
                                        "${primitiveClass.qualifiedName}",
                                )
                        val serializer =
                            primitiveValueSerializations[primitiveClass]!!
                                as PrimitiveValueSerialization<Any>
                        serialization.primitiveValuesSerialization.registerSerializer(
                            lwPrimitiveType.id!!,
                        ) { value -> serializer.serialize(value) }

                        serialization.primitiveValuesSerialization.registerDeserializer(
                            lwPrimitiveType.id!!,
                        ) { serialized -> serializer.deserialize(serialized) }
                    }
                }
            }
        }
        return serialization
    }

    /**
     * Deserialize nodes, taking into accaount the known languages.
     */
    fun deserializeToNodes(
        json: String,
        useDynamicNodesIfNeeded: Boolean = true,
    ): List<LWNode> {
        val js = prepareSerialization() as JsonSerialization
        if (useDynamicNodesIfNeeded) {
            js.enableDynamicNodes()
        }
        return js.deserializeToNodes(json)
    }

    fun knownLWLanguages(): Set<LWLanguage> {
        synchronized(languageConverter) {
            return languageConverter.knownLWLanguages()
        }
    }

    fun knownKolasuLanguages(): Set<KolasuLanguage> {
        synchronized(languageConverter) {
            return languageConverter.knownKolasuLanguages()
        }
    }

    fun getKolasuClassesToClassifiersMapping(): Map<KClass<*>, Classifier<*>> {
        synchronized(languageConverter) {
            return languageConverter.getKolasuClassesToClassifiersMapping()
        }
    }

    fun getClassifiersToKolasuClassesMapping(): Map<Classifier<*>, KClass<*>> {
        synchronized(languageConverter) {
            return languageConverter.getClassifiersToKolasuClassesMapping()
        }
    }

    /**
     * Track reference values, so that we can populate them once the nodes are instantiated.
     */
    private class ReferencesPostponer {
        private val values = IdentityHashMap<com.strumenta.kolasu.model.ReferenceValue<PossiblyNamed>, LWNode?>()
        private val originValues = IdentityHashMap<KNode, String>()
        private val destinationValues = IdentityHashMap<KNode, List<String>>()

        fun registerPostponedReference(
            referenceValue: com.strumenta.kolasu.model.ReferenceValue<PossiblyNamed>,
            referred: LWNode?,
        ) {
            values[referenceValue] = referred
        }

        fun populateReferences(
            nodesMapping: BiMap<Any, LWNode>,
            externalNodeResolver: NodeResolver,
        ) {
            values.forEach { entry ->
                if (entry.value == null) {
                    entry.key.referred = null
                } else {
                    if (entry.value is ProxyNode) {
                        entry.key.identifier = (entry.value as ProxyNode).id
                    } else {
                        entry.key.referred = nodesMapping.byB(entry.value as LWNode)!! as PossiblyNamed
                        entry.key.identifier = entry.value!!.id!!
                    }
                }
            }
            originValues.forEach { entry ->
                val lwNode = nodesMapping.bs.find { it.id == entry.value }
                if (lwNode != null) {
                    val correspondingKNode = nodesMapping.byB(lwNode) as KNode
                    // TODO keep also position
                    entry.key.origin = NodeOrigin(correspondingKNode)
                } else {
                    val correspondingKNode =
                        externalNodeResolver.resolve(entry.value) ?: throw IllegalStateException(
                            "Unable to resolve node with ID ${entry.value}",
                        )
                    // TODO keep also position
                    entry.key.origin = NodeOrigin(correspondingKNode)
                }
            }
            destinationValues.forEach { entry ->
                val values =
                    entry.value.map { targetID ->
                        val lwNode = nodesMapping.bs.find { it.id == targetID }
                        val targetNode =
                            if (lwNode != null) {
                                nodesMapping.byB(lwNode) as KNode
                            } else {
                                externalNodeResolver.resolve(targetID) ?: throw IllegalStateException(
                                    "Unable to resolve node with ID $targetID",
                                )
                            }
                        NodeDestination(targetNode)
                    }
                entry.key.destinations.addAll(values)
            }
        }

        fun registerPostponedOriginReference(
            kNode: KNode,
            originalNodeID: String,
        ) {
            originValues[kNode] = originalNodeID
        }

        fun registerPostponedTranspiledReference(
            kNode: KNode,
            transpiledNodeIDs: List<String>,
        ) {
            destinationValues[kNode] = transpiledNodeIDs
        }
    }

    private fun attributeValue(
        data: LWNode,
        property: Property,
    ): Any? {
        val propValue = data.getPropertyValue(property)
        val value =
            if (property.type is Enumeration && propValue != null) {
                val enumerationLiteral =
                    if (propValue is EnumerationValue) {
                        propValue.enumerationLiteral
                    } else {
                        throw java.lang.IllegalStateException(
                            "Property value of property of enumeration type is " +
                                "not an EnumerationValue. It is instead " + propValue,
                        )
                    }
                val enumKClass =
                    synchronized(languageConverter) {
                        languageConverter
                            .getEnumerationsToKolasuClassesMapping()[enumerationLiteral.enumeration]
                            ?: throw java.lang.IllegalStateException(
                                "Cannot find enum class for enumeration " +
                                    "${enumerationLiteral.enumeration?.name}",
                            )
                    }
                val entries = enumKClass.java.enumConstants
                entries.find { it.name == enumerationLiteral.name }
                    ?: throw IllegalStateException(
                        "Cannot find enum constant named ${enumerationLiteral.name} in enum " +
                            "class ${enumKClass.qualifiedName}",
                    )
            } else {
                propValue
            }
        return value
    }

    private fun containmentValue(
        data: LWNode,
        containment: Containment,
    ): Any? {
        val lwChildren = data.getChildren(containment)
        if (containment.isMultiple) {
            val kChildren = lwChildren.map { nodesMapping.byB(it)!! }
            return kChildren
        } else {
            // Given we navigate the tree in reverse the child should have been already
            // instantiated
            val lwChild: Node? =
                when (lwChildren.size) {
                    0 -> {
                        null
                    }

                    1 -> {
                        lwChildren.first()
                    }

                    else -> {
                        throw IllegalStateException()
                    }
                }
            val kChild =
                if (lwChild == null) {
                    return null
                } else {
                    (
                        return nodesMapping.byB(lwChild)
                            ?: throw IllegalStateException(
                                "Unable to find Kolasu Node corresponding to $lwChild",
                            )
                    )
                }
        }
    }

    private fun referenceValue(
        data: LWNode,
        reference: Reference,
        referencesPostponer: ReferencesPostponer,
        currentValue: KReferenceValue<PossiblyNamed>? = null,
    ): Any? {
        val referenceValues = data.getReferenceValues(reference)
        return when {
            referenceValues.size > 1 -> {
                throw IllegalStateException()
            }
            referenceValues.size == 0 -> {
                null
            }
            referenceValues.size == 1 -> {
                val rf = referenceValues.first()
                val referenceByName = currentValue ?: KReferenceValue<PossiblyNamed>(rf.resolveInfo!!, null)
                referencesPostponer.registerPostponedReference(referenceByName, rf.referred)
                referenceByName
            }
            else -> throw java.lang.IllegalStateException()
        }
    }

    private fun <T : Any> instantiate(
        kClass: KClass<T>,
        data: Node,
        referencesPostponer: ReferencesPostponer,
    ): T {
        val specialObject = maybeInstantiateSpecialObject(kClass, data)
        if (specialObject != null) {
            return specialObject as T
        }
        val constructor =
            when {
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
            val feature = lwFeatureByName(data.classifier, param.name!!)
            if (feature == null) {
                throw java.lang.IllegalStateException(
                    "We could not find a feature named as the parameter ${param.name} " +
                        "on class $kClass",
                )
            } else {
                when (feature) {
                    is Property -> {
                        params[param] = attributeValue(data, feature)
                    }

                    is Reference -> {
                        params[param] = referenceValue(data, feature, referencesPostponer)
                    }

                    is Containment -> {
                        params[param] = containmentValue(data, feature)
                    }

                    else -> throw IllegalStateException()
                }
            }
        }

        val kNode =
            try {
                constructor.callBy(params) as T
            } catch (e: Exception) {
                throw RuntimeException(
                    "Issue instantiating using constructor $constructor with params " +
                        "${params.map { "${it.key.name}=${it.value}" }}",
                    e,
                )
            }

        val propertiesNotSetAtConstructionTime =
            kClass.nodeOriginalProperties.filter { prop ->
                params.keys.none { param ->
                    param.name == prop.name
                }
            }
        propertiesNotSetAtConstructionTime.forEach { property ->
            val feature = lwFeatureByName(data.classifier, property.name)
            if (property !is KMutableProperty<*>) {
                if (property.isContainment() && property.asContainment().multiplicity == Multiplicity.MANY) {
                    val currentValue = property.get(kNode) as MutableList<KNode>
                    currentValue.clear()
                    val valueToSet = containmentValue(data, feature as Containment) as List<KNode>
                    currentValue.addAll(valueToSet)
                } else if (property.isReference()) {
                    val currentValue = property.get(kNode) as KReferenceValue<PossiblyNamed>
                    val valueToSet =
                        referenceValue(data, feature as Reference, referencesPostponer, currentValue)
                            as KReferenceValue<PossiblyNamed>
                    currentValue.name = valueToSet.name
                    currentValue.referred = valueToSet.referred
                    currentValue.identifier = valueToSet.identifier
                } else {
                    throw java.lang.IllegalStateException(
                        "Cannot set this property, as it is immutable: ${property.name} on $kNode. " +
                            "The properties set at construction time are: " +
                            params.keys.joinToString(", ") { it.name ?: "<UNNAMED>" },
                    )
                }
            } else {
                when {
                    property.isAttribute() -> {
                        val value = attributeValue(data, feature as Property)
                        property.setter.call(kNode, value)
                    }
                    property.isReference() -> {
                        val valueToSet =
                            referenceValue(data, feature as Reference, referencesPostponer)
                                as KReferenceValue<PossiblyNamed>
                        property.setter.call(kNode, valueToSet)
                    }
                    property.isContainment() -> {
                        try {
                            val valueToSet = containmentValue(data, feature as Containment)
                            property.setter.call(kNode, valueToSet)
                        } catch (e: java.lang.Exception) {
                            throw RuntimeException("Unable to set containment $feature on node $kNode", e)
                        }
                    }
                }
            }
        }

        return kNode
    }

    /**
     * We treat some Kolasu classes that are not Nodes specially, such as Issue or ParsingResult.
     * This method checks if we are to instantiate one of those, and returns the instance with all properties filled;
     * or it returns null when it detects that we're going to instantiate a proper Node.
     */
    private fun maybeInstantiateSpecialObject(
        kClass: KClass<*>,
        data: Node,
    ): Any? {
        return when (kClass) {
            Issue::class -> {
                Issue(
                    attributeValue(data, data.classifier.getPropertyByName(Issue::type.name)!!) as IssueType,
                    attributeValue(data, data.classifier.getPropertyByName(Issue::message.name)!!) as String,
                    attributeValue(data, data.classifier.getPropertyByName(Issue::severity.name)!!) as IssueSeverity,
                    attributeValue(data, data.classifier.getPropertyByName(Issue::range.name)!!) as Range?,
                )
            }
            ParsingResult::class -> {
                val root = data.getOnlyChildByContainmentName(ParsingResult<*>::root.name)
                val tokens =
                    data.getPropertyValue(
                        data.classifier.getPropertyByName(ParsingResultWithTokens<*>::tokens.name)!!,
                    ) as TokensList?
                ParsingResultWithTokens(
                    data.getChildrenByContainmentName(ParsingResult<*>::issues.name).map {
                        importModelFromLionWeb(it) as Issue
                    },
                    if (root != null) importModelFromLionWeb(root) as KNode else null,
                    tokens?.tokens ?: listOf(),
                )
            }
            else -> {
                null
            }
        }
    }

    private fun findConcept(kNode: NodeLike): Concept {
        return synchronized(languageConverter) { languageConverter.correspondingConcept(kNode.nodeType) }
    }

    private fun nodeID(
        kNode: NodeLike,
        customSourceId: String? = null,
    ): String {
        return "${customSourceId ?: kNode.source.id}_${kNode.positionalID}"
    }

    private fun associateNodes(
        kNode: Any,
        lwNode: LWNode,
    ) {
        nodesMapping.associate(kNode, lwNode)
    }

    fun exportIssueToLionweb(issue: Issue): IssueNode {
        val issueNode = IssueNode()
        issueNode.setPropertyValue(StarLasuLWLanguage.Issue.getPropertyByName(Issue::message.name)!!, issue.message)
        issueNode.setPropertyValue(StarLasuLWLanguage.Issue.getPropertyByName(Issue::range.name)!!, issue.range)
        setEnumProperty(issueNode, StarLasuLWLanguage.Issue.getPropertyByName(Issue::severity.name)!!, issue.severity)
        setEnumProperty(issueNode, StarLasuLWLanguage.Issue.getPropertyByName(Issue::type.name)!!, issue.type)
        return issueNode
    }

    fun exportParsingResultToLionweb(
        pr: ParsingResult<*>,
        tokens: List<KolasuToken> = listOf(),
    ): ParsingResultNode {
        val resultNode = ParsingResultNode(pr.source)
        resultNode.setPropertyValue(
            StarLasuLWLanguage.ParsingResult.getPropertyByName(ParsingResult<*>::code.name)!!,
            pr.code,
        )
        val root = if (pr.root != null) exportModelToLionWeb(pr.root!!, considerParent = false) else null
        root?.let {
            resultNode.addChild(
                StarLasuLWLanguage.ParsingResult.getContainmentByName(ParsingResult<*>::root.name)!!,
                root,
            )
        }
        val issuesContainment = StarLasuLWLanguage.ParsingResult.getContainmentByName(ParsingResult<*>::issues.name)!!
        pr.issues.forEach {
            resultNode.addChild(issuesContainment, exportIssueToLionweb(it))
        }
        resultNode.setPropertyValue(
            StarLasuLWLanguage.ParsingResult.getPropertyByName(ParsingResultWithTokens<*>::tokens.name)!!,
            TokensList(tokens),
        )
        return resultNode
    }
}

class ParsingResultWithTokens<RootNode : KNode>(
    issues: List<Issue>,
    root: RootNode?,
    val tokens: List<KolasuToken>,
    code: String? = null,
    incompleteNode: com.strumenta.kolasu.model.Node? = null,
    time: Long? = null,
    source: Source? = null,
) : ParsingResult<RootNode>(issues, root, code, incompleteNode, time, source)

class IssueNode : BaseNode() {
    var type: EnumerationValue? by property(Issue::type.name)
    var message: String? by property(Issue::message.name)
    var severity: EnumerationValue? by property(Issue::severity.name)
    var range: Range? by property(Issue::range.name)

    override fun getClassifier(): Concept {
        return StarLasuLWLanguage.Issue
    }
}

class ParsingResultNode(
    val source: Source?,
) : BaseNode() {
    override fun calculateID(): String? {
        return try {
            SimpleSourceIdProvider().sourceId(source) + "_ParsingResult"
        } catch (_: SourceShouldBeSetException) {
            super.calculateID()
        }
    }

    override fun getClassifier(): Concept {
        return StarLasuLWLanguage.ParsingResult
    }
}

private val KNode.positionalID: String
    get() {
        return if (this.parent == null) {
            "root"
        } else {
            val cp = this.containingContainment()!!
            val postfix = if (cp.isMultiple) "${cp.name}_${this.indexInContainingProperty()!!}" else cp.name
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
