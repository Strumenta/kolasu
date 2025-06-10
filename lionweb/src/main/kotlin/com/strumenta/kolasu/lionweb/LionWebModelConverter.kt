package com.strumenta.kolasu.lionweb

import com.strumenta.kolasu.ids.IDGenerationException
import com.strumenta.kolasu.ids.NodeIdProvider
import com.strumenta.kolasu.language.Feature
import com.strumenta.kolasu.language.KolasuLanguage
import com.strumenta.kolasu.model.CompositeDestination
import com.strumenta.kolasu.model.Multiplicity
import com.strumenta.kolasu.model.Position
import com.strumenta.kolasu.model.PossiblyNamed
import com.strumenta.kolasu.model.ReferenceByName
import com.strumenta.kolasu.model.allFeatures
import com.strumenta.kolasu.model.asContainment
import com.strumenta.kolasu.model.assignParents
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
import com.strumenta.starlasu.base.ASTLanguage
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
import io.lionweb.lioncore.java.model.ReferenceValue
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
import io.lionweb.lioncore.kotlin.MetamodelRegistry
import io.lionweb.lioncore.kotlin.getChildrenByContainmentName
import io.lionweb.lioncore.kotlin.getOnlyChildByContainmentName
import java.util.IdentityHashMap
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass
import kotlin.reflect.KMutableProperty
import kotlin.reflect.KParameter
import kotlin.reflect.KType
import kotlin.reflect.full.primaryConstructor
import io.lionweb.lioncore.java.language.Feature as LWFeature

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

private val ASTNodePosition = ASTLanguage.getASTNode().getPropertyByName("position")!!
private val ASTNodeOriginalNode = ASTLanguage.getASTNode().getReferenceByName("originalNode")!!
private val ASTNodeTranspiledNodes = ASTLanguage.getASTNode().getReferenceByName("transpiledNodes")!!

/**
 * This class is able to convert between Kolasu and LionWeb models, tracking the mapping.
 *
 * This class is thread-safe.
 *
 * @param nodeIdProvider logic to be used to associate IDs to Kolasu nodes when exporting them to LionWeb
 *                       it will be used to assign an ID to those elements which already do not have one
 *                       (through the HasID.id field).
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
     * We mostly map Kolasu Nodes to LionWeb Nodes, but we may also map things that are not Kolasu Nodes but are nodes
     * for LionWeb. For example, we could do that for Issues and Parsing Results.
     *
     * In the future we could rely on the HasID.id field to consider removing this field, which can grow significantly
     * and cause issues when nodes are cached and changes happening between two conversions are ignored.
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
                        // If a kNode has already an id, it should prevail and we should not attempt to generate
                        // an ID for it
                        require(kNode.id == null) {
                            "We should not generate an ID for a kNode which already has one"
                        }

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
                    val nodeID = kNode.id ?: myIDManager.nodeId(kNode)
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
                        kNode.javaClass.kotlin.allFeatures().associateBy { it.name }
                    }
                val lwFeatures =
                    lwFeaturesCache.getOrPut(lwNode.classifier) {
                        lwNode.classifier.allFeatures().associateBy { it.name!! }
                    }
                lwFeatures.values.forEach { feature ->
                    when (feature) {
                        is Property -> {
                            if (feature == ASTNodePosition) {
                                lwNode.setPropertyValue(ASTNodePosition, kNode.position)
                            } else {
                                val kAttribute =
                                    kFeatures[feature.name]
                                        as? com.strumenta.kolasu.language.Attribute
                                        ?: throw IllegalArgumentException(
                                            "Property ${feature.name} " +
                                                "not found in $kNode",
                                        )
                                val kValue = kNode.getAttributeValue(kAttribute)
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
                                            "Cannot find containment for ${feature.name} when considering node $kNode",
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
                            if (feature == ASTNodeOriginalNode) {
                                val origin = kNode.origin
                                if (origin is KNode) {
                                    val targetID = origin.id ?: myIDManager.nodeId(origin)
                                    setOriginalNode(lwNode, targetID)
                                } else if (origin is PlaceholderASTTransformation) {
                                    if (lwNode is AbstractClassifierInstance<*>) {
                                        val instance =
                                            DynamicAnnotationInstance(
                                                "${lwNode.id}_placeholder_annotation",
                                                StarLasuLWLanguage.PlaceholderNode,
                                            )
                                        val effectiveOrigin = origin.origin
                                        if (effectiveOrigin is KNode) {
                                            val targetID = effectiveOrigin.id ?: myIDManager.nodeId(effectiveOrigin)
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
                            } else if (feature == ASTNodeTranspiledNodes) {
                                val destinationNodes = mutableListOf<KNode>()
                                if (kNode.destination != null) {
                                    if (kNode.destination is KNode) {
                                        destinationNodes.add(kNode.destination as KNode)
                                    } else if (kNode.destination is CompositeDestination) {
                                        destinationNodes.addAll(
                                            (kNode.destination as CompositeDestination).elements
                                                .filterIsInstance<KNode>(),
                                        )
                                    }
                                }
                                val referenceValues =
                                    destinationNodes.map { destinationNode ->
                                        val targetID = destinationNode.id ?: myIDManager.nodeId(destinationNode)
                                        ReferenceValue(ProxyNode(targetID), null)
                                    }
                                lwNode.setReferenceValues(ASTNodeTranspiledNodes, referenceValues)
                            } else {
                                val kReference =
                                    (
                                        kFeatures[feature.name]
                                            ?: throw java.lang.IllegalStateException(
                                                "Cannot find feature ${feature.name} " +
                                                    "in Starlasu Node ${kNode.nodeType}",
                                            )
                                    )
                                        as com.strumenta.kolasu.language.Reference
                                val kValue = kNode.getReference(kReference)
                                if (kValue == null) {
                                    lwNode.addReferenceValue(feature, null)
                                } else {
                                    when {
                                        kValue.retrieved -> {
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
                                            require(lwReferred.id != null)
                                            lwNode.addReferenceValue(feature, ReferenceValue(lwReferred, kValue.name))
                                        }

                                        kValue.resolved -> {
                                            // This is tricky, as we need to set a LW Node, but we have just an identifier...
                                            val lwReferred: Node =
                                                DynamicNode(kValue.identifier!!, LionCoreBuiltins.getNode())
                                            lwNode.addReferenceValue(feature, ReferenceValue(lwReferred, kValue.name))
                                        }

                                        else -> {
                                            lwNode.addReferenceValue(feature, ReferenceValue(null, kValue.name))
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
            ASTNodeOriginalNode,
            listOf(
                ReferenceValue(ProxyNode(targetID), null),
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
        lwTree.thisAndAllDescendants().toList().myReversed().forEach { lwNode ->
            val kClass =
                synchronized(languageConverter) {
                    languageConverter.correspondingKolasuClass(lwNode.classifier)
                }
                    ?: throw RuntimeException(
                        "We do not have StarLasu AST class for LionWeb Concept " +
                            "${lwNode.classifier}",
                    )
            try {
                val instantiated = instantiate(kClass, lwNode, referencesPostponer)
                if (instantiated is KNode) {
                    instantiated.assignParents()
                    // This mapping will eventually become superfluous because we will store the ID directly in the
                    // instantiated kNode
                    nodeIdProvider.registerMapping(instantiated, lwNode.id!!)
                    instantiated.id = lwNode.id
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
                val lwPosition = lwNode.getPropertyValue(ASTNodePosition)
                if (lwPosition != null) {
                    kNode.position = lwPosition as Position
                }
                val originalNodes = lwNode.getReferenceValues(ASTNodeOriginalNode)
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
                        MissingASTTransformation::class.simpleName -> {
                            placeholderNodes[kNode] = { kNode ->
                                kNode.origin =
                                    MissingASTTransformation(
                                        origin = kNode.origin,
                                        transformationSource = kNode.origin as? KNode,
                                        expectedType = null,
                                    )
                            }
                        }
                        FailingASTTransformation::class.simpleName -> {
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
                val transpiledNodes = lwNode.getReferenceValues(ASTNodeTranspiledNodes)
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
            SerializationProvider.getStandardJsonSerialization(LIONWEB_VERSION_USED_BY_KOLASU),
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
        private val values = IdentityHashMap<ReferenceByName<PossiblyNamed>, LWNode?>()
        private val originValues = IdentityHashMap<KNode, String>()
        private val destinationValues = IdentityHashMap<KNode, List<String>>()

        fun registerPostponedReference(
            referenceByName: ReferenceByName<PossiblyNamed>,
            referred: LWNode?,
        ) {
            values[referenceByName] = referred
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
                        val target = entry.value as LWNode
                        val nodeMapping = nodesMapping.byB(target)
                        if (nodeMapping == null) {
                            // ?: throw IllegalStateException("No node mapping for $target (id: ${target.id}, classifier: ${target.classifier})")
                            entry.key.identifier = entry.value!!.id!!
                        } else {
                            entry.key.referred = nodeMapping as PossiblyNamed
                            entry.key.identifier = entry.value!!.id!!
                        }
                    }
                }
            }
            originValues.forEach { entry ->
                val lwNode = nodesMapping.bs.find { it.id == entry.value }
                if (lwNode != null) {
                    val correspondingKNode = nodesMapping.byB(lwNode) as KNode
                    // TODO keep also position
                    entry.key.origin = correspondingKNode
                } else {
                    val correspondingKNode =
                        externalNodeResolver.resolve(entry.value) ?: throw IllegalStateException(
                            "Unable to resolve node with ID ${entry.value}",
                        )
                    // TODO keep also position
                    entry.key.origin = correspondingKNode
                }
            }
            destinationValues.forEach { entry ->
                val values =
                    entry.value.map { targetID ->
                        val lwNode = nodesMapping.bs.find { it.id == targetID }
                        if (lwNode != null) {
                            nodesMapping.byB(lwNode) as KNode
                        } else {
                            externalNodeResolver.resolve(targetID) ?: throw IllegalStateException(
                                "Unable to resolve node with ID $targetID",
                            )
                        }
                    }
                if (values.size == 1) {
                    entry.key.destination = values.first()
                } else {
                    entry.key.destination = CompositeDestination(values)
                }
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

    private fun importEnumValue(propValue: EnumerationValue): Any {
        val enumerationLiteral = propValue.enumerationLiteral
        val enumKClass =
            synchronized(languageConverter) {
                languageConverter
                    .getEnumerationsToKolasuClassesMapping().entries.find {
                        it.key.id == enumerationLiteral.enumeration?.id
                    }?.value
                    ?: throw java.lang.IllegalStateException(
                        "Cannot find enum class for enumeration " +
                            "${enumerationLiteral.enumeration?.name}",
                    )
            }
        val entries = enumKClass.java.enumConstants
        return entries.find { it.name == enumerationLiteral.name }
            ?: throw IllegalStateException(
                "Cannot find enum constant named ${enumerationLiteral.name} in enum " +
                    "class ${enumKClass.qualifiedName}",
            )
    }

    private fun attributeValue(
        data: LWNode,
        property: Property,
    ): Any? {
        val propValue = data.getPropertyValue(property)
        val value =
            if (property.type is Enumeration && propValue != null) {
                importEnumValue(propValue as EnumerationValue)
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
        currentValue: ReferenceByName<PossiblyNamed>? = null,
    ): Any? {
        val referenceValues = data.getReferenceValues(reference)
        return when {
            referenceValues.size == 0 -> {
                null
            }
            referenceValues.size == 1 -> {
                val rf = referenceValues.first()
                val referenceByName = currentValue ?: ReferenceByName<PossiblyNamed>(rf.resolveInfo!!, null)
                referencesPostponer.registerPostponedReference(referenceByName, rf.referred)
                referenceByName
            }
            else -> {
                throw IllegalStateException()
            }
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
                        val value = attributeValue(data, feature)
                        if (!param.type.isAssignableBy(value)) {
                            throw RuntimeException(
                                "Cannot assign value $value to param ${param.name} of type ${param.type}",
                            )
                        }
                        params[param] = value
                    }
                    is Reference -> {
                        val value = referenceValue(data, feature, referencesPostponer)
                        if (!param.type.isAssignableBy(value)) {
                            throw RuntimeException()
                        }
                        params[param] = value
                    }
                    is Containment -> {
                        val value = containmentValue(data, feature)
                        if (!param.type.isAssignableBy(value)) {
                            throw RuntimeException()
                        }
                        params[param] = value
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
                    "Issue instantiating using constructor ${kClass.qualifiedName}.$constructor with params " +
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
                    val currentValue = property.get(kNode) as ReferenceByName<PossiblyNamed>
                    val valueToSet =
                        referenceValue(data, feature as Reference, referencesPostponer, currentValue)
                            as ReferenceByName<PossiblyNamed>
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
                                as ReferenceByName<PossiblyNamed>
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
                    attributeValue(data, data.classifier.getPropertyByName(Issue::position.name)!!) as Position?,
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

    private fun findConcept(kNode: com.strumenta.kolasu.model.Node): Concept {
        return synchronized(languageConverter) { languageConverter.correspondingConcept(kNode.nodeType) }
    }

    private fun associateNodes(
        kNode: Any,
        lwNode: LWNode,
    ) {
        if (kNode is KNode) {
            require(kNode.id == null || kNode.id == lwNode.id)
            kNode.id = lwNode.id
        }
        nodesMapping.associate(kNode, lwNode)
    }

    fun exportIssueToLionweb(
        issue: Issue,
        id: String? = null,
    ): IssueNode {
        val issueNode = IssueNode()
        id?.let { issueNode.id = it }
        issueNode.setPropertyValue(ASTLanguage.getIssue().getPropertyByName(Issue::message.name)!!, issue.message)
        issueNode.setPropertyValue(ASTLanguage.getIssue().getPropertyByName(Issue::position.name)!!, issue.position)
        setEnumProperty(issueNode, ASTLanguage.getIssue().getPropertyByName(Issue::severity.name)!!, issue.severity)
        setEnumProperty(issueNode, ASTLanguage.getIssue().getPropertyByName(Issue::type.name)!!, issue.type)
        return issueNode
    }

    fun importIssueFromLionweb(issueNode: IssueNode): Pair<String, Issue> {
        val issueType =
            importEnumValue(
                issueNode.getPropertyValue(
                    ASTLanguage.getIssue().getPropertyByName(Issue::type.name)!!,
                ) as EnumerationValue,
            ) as IssueType
        val message =
            issueNode.getPropertyValue(
                ASTLanguage.getIssue().getPropertyByName(Issue::message.name)!!,
            ) as String
        val severity =
            importEnumValue(
                issueNode.getPropertyValue(
                    ASTLanguage.getIssue().getPropertyByName(Issue::severity.name)!!,
                ) as EnumerationValue,
            ) as IssueSeverity
        val position =
            issueNode.getPropertyValue(
                ASTLanguage.getIssue().getPropertyByName(Issue::position.name)!!,
            ) as Position
        val issue = Issue(issueType, message, severity, position)
        return issueNode.id!! to issue
    }

    fun exportParsingResultToLionweb(
        pr: ParsingResult<*>,
        tokens: List<KolasuToken> = listOf(),
    ): ParsingResultNode {
        val resultNode = ParsingResultNode(pr.source)
        if (resultNode.id == null) {
            throw IllegalStateException("Parsing result has null ID")
        }
        resultNode.setPropertyValue(
            ASTLanguage.getParsingResult().getPropertyByName(ParsingResult<*>::code.name)!!,
            pr.code,
        )
        val root = if (pr.root != null) exportModelToLionWeb(pr.root!!, considerParent = false) else null
        root?.let {
            resultNode.addChild(
                ASTLanguage.getParsingResult().getContainmentByName(ParsingResult<*>::root.name)!!,
                root,
            )
        }
        val issuesContainment = ASTLanguage.getParsingResult().getContainmentByName(ParsingResult<*>::issues.name)!!
        pr.issues.forEachIndexed { index, issue ->
            val id = "${resultNode.id}-issue-$index"
            resultNode.addChild(issuesContainment, exportIssueToLionweb(issue, id))
        }
        resultNode.setPropertyValue(
            ASTLanguage.getParsingResult().getPropertyByName(ParsingResultWithTokens<*>::tokens.name)!!,
            TokensList(tokens),
        )
        resultNode.thisAndAllDescendants().forEach { lwNode ->
            require(lwNode.id != null) { "Node $lwNode should get a valid ID" }
        }
        return resultNode
    }
}

fun KType.isAssignableBy(value: Any?): Boolean {
    val classifier = this.classifier as? KClass<*> ?: return false

    if (value == null) {
        return this.isMarkedNullable
    }

    return classifier.isInstance(value)
}

/**
 * This avoids an issue when using JDK 21+. In JDK 21 java.util.List.asReversed was introduced
 * and cause a conflict with the Kotlin one.
 */
fun <T> Iterable<T>.myReversed(): List<T> {
    val result = mutableListOf<T>()
    for (item in this) {
        result.add(item)
    }
    result.reverse()
    return result
}
