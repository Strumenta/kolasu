package com.strumenta.kolasu.lionweb

import com.strumenta.kolasu.ids.NodeIdProvider
import com.strumenta.kolasu.language.Attribute
import com.strumenta.kolasu.language.KolasuLanguage
import com.strumenta.kolasu.model.FileSource
import com.strumenta.kolasu.model.NodeLike
import com.strumenta.kolasu.model.Point
import com.strumenta.kolasu.model.PossiblyNamed
import com.strumenta.kolasu.model.Range
import com.strumenta.kolasu.model.ReferenceByName
import com.strumenta.kolasu.model.Source
import com.strumenta.kolasu.model.allFeatures
import com.strumenta.kolasu.model.assignParents
import com.strumenta.kolasu.model.containingProperty
import com.strumenta.kolasu.model.indexInContainingProperty
import com.strumenta.kolasu.traversing.walk
import io.lionweb.lioncore.java.language.Classifier
import io.lionweb.lioncore.java.language.Concept
import io.lionweb.lioncore.java.language.Containment
import io.lionweb.lioncore.java.language.Enumeration
import io.lionweb.lioncore.java.language.Language
import io.lionweb.lioncore.java.language.LionCoreBuiltins
import io.lionweb.lioncore.java.language.PrimitiveType
import io.lionweb.lioncore.java.language.Property
import io.lionweb.lioncore.java.language.Reference
import io.lionweb.lioncore.java.model.Node
import io.lionweb.lioncore.java.model.ReferenceValue
import io.lionweb.lioncore.java.model.impl.DynamicEnumerationValue
import io.lionweb.lioncore.java.model.impl.DynamicNode
import io.lionweb.lioncore.java.model.impl.ProxyNode
import io.lionweb.lioncore.java.serialization.JsonSerialization
import java.lang.IllegalArgumentException
import java.util.IdentityHashMap
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.KParameter
import kotlin.reflect.KProperty1
import kotlin.reflect.full.memberProperties
import kotlin.reflect.full.primaryConstructor

interface PrimitiveValueSerialization<E> {
    fun serialize(value: E): String

    fun deserialize(serialized: String): E
}

/**
 * This class is able to convert between Kolasu and LionWeb models, tracking the mapping.
 *
 * @param nodeIdProvider logic to be used to associate IDs to Kolasu nodes when exporting them to LionWeb
 */
class LionWebModelConverter(
    var nodeIdProvider: NodeIdProvider = StructuralLionWebNodeIdProvider(),
) {
    private val languageConverter = LionWebLanguageConverter()

    /**
     * We mostly map Kolasu Nodes to LionWeb Nodes, but we also map things that are not Kolasu Nodes such
     * as instances of Position and Point.
     */
    private val nodesMapping = BiMap<Any, LWNode>(usingIdentity = true)
    private val primitiveValueSerializations = mutableMapOf<KClass<*>, PrimitiveValueSerialization<*>>()

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
        return languageConverter.correspondingLanguage(kolasuLanguage)
    }

    fun exportLanguageToLionWeb(kolasuLanguage: KolasuLanguage): Language {
        return languageConverter.exportToLionWeb(kolasuLanguage)
    }

    fun associateLanguages(
        lwLanguage: Language,
        kolasuLanguage: KolasuLanguage,
    ) {
        this.languageConverter.associateLanguages(lwLanguage, kolasuLanguage)
    }

    fun exportModelToLionWeb(
        kolasuTree: KNode,
        nodeIdProvider: NodeIdProvider = this.nodeIdProvider,
        considerParent: Boolean = true,
    ): LWNode {
        if (!nodesMapping.containsA(kolasuTree)) {
            kolasuTree.walk().forEach { kNode ->
                if (!nodesMapping.containsA(kNode)) {
                    val lwNode = DynamicNode(nodeIdProvider.id(kNode), findConcept(kNode))
                    associateNodes(kNode, lwNode)
                }
            }
            kolasuTree.walk().forEach { kNode ->
                val lwNode = nodesMapping.byA(kNode)!!
                val kFeatures = kNode.javaClass.kotlin.allFeatures()
                lwNode.concept.allFeatures().forEach { feature ->
                    when (feature) {
                        is Property -> {
                            val kAttribute =
                                kFeatures.find { it.name == feature.name }
                                    as? Attribute
                                    ?: throw IllegalArgumentException("Property ${feature.name} not found in $kNode")
                            val kValue = kNode.getAttributeValue(kAttribute)
                            lwNode.setPropertyValue(feature, kValue)
                        }

                        is Containment -> {
                            try {
                                if (feature == StarLasuLWLanguage.ASTNodeRange) {
                                    if (kNode.range != null) {
                                        val lwPositionStartValue =
                                            DynamicNode(
                                                lwNode.id + "_range_start",
                                                StarLasuLWLanguage.Point,
                                            )
                                        lwPositionStartValue.setPropertyValueByName("line", kNode.range!!.start.line)
                                        lwPositionStartValue.setPropertyValueByName(
                                            "column",
                                            kNode.range!!.start.column,
                                        )

                                        val lwPositionEndValue =
                                            DynamicNode(
                                                lwNode.id + "_range_end",
                                                StarLasuLWLanguage.Point,
                                            )
                                        lwPositionEndValue.setPropertyValueByName("line", kNode.range!!.end.line)
                                        lwPositionEndValue.setPropertyValueByName("column", kNode.range!!.end.column)

                                        val lwPositionValue =
                                            DynamicNode(lwNode.id + "_range", StarLasuLWLanguage.Range)
                                        lwPositionValue.addChild(StarLasuLWLanguage.RangeStart, lwPositionStartValue)
                                        lwPositionValue.addChild(StarLasuLWLanguage.RangeEnd, lwPositionEndValue)

                                        lwNode.addChild(StarLasuLWLanguage.ASTNodeRange, lwPositionValue)
                                    }
                                } else {
                                    val kContainment =
                                        kFeatures.find { it.name == feature.name }
                                            as com.strumenta.kolasu.language.Containment
                                    val kValue = kNode.getChildren(kContainment)
                                    kValue.forEach { kChild ->
                                        val lwChild = nodesMapping.byA(kChild)!!
                                        lwNode.addChild(feature, lwChild)
                                    }
                                }
                            } catch (e: Exception) {
                                throw RuntimeException("Issue while processing containment ${feature.name}", e)
                            }
                        }
                        is Reference -> {
                            val kReference =
                                kFeatures.find { it.name == feature.name }
                                    as com.strumenta.kolasu.language.Reference
                            val kValue = kNode.getReference(kReference)
                            if (kValue == null) {
                                lwNode.addReferenceValue(feature, null)
                            } else {
                                when {
                                    kValue.isRetrieved -> {
                                        val lwReferred: Node = nodesMapping.byA(kValue.referred!! as KNode)!!
                                        lwNode.addReferenceValue(feature, ReferenceValue(lwReferred, kValue.name))
                                    }
                                    kValue.isResolved -> {
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

        val result = nodesMapping.byA(kolasuTree)!!
        if (considerParent && kolasuTree.parent != null) {
            (result as DynamicNode).parent = ProxyNode(nodeIdProvider.id(kolasuTree.parent!!))
        }
        return result
    }

    fun importModelFromLionWeb(lwTree: LWNode): Any {
        val referencesPostponer = ReferencesPostponer()
        lwTree.thisAndAllDescendants().reversed().forEach { lwNode ->
            val kClass =
                languageConverter.correspondingKolasuClass(lwNode.concept)
                    ?: throw RuntimeException("We do not have StarLasu AST class for LIonWeb Concept ${lwNode.concept}")
            try {
                val instantiated = instantiate(kClass, lwNode, referencesPostponer)
                if (instantiated is KNode) {
                    instantiated.assignParents()
                }
                associateNodes(instantiated, lwNode)
            } catch (e: RuntimeException) {
                throw RuntimeException("Issue instantiating $kClass from LionWeb node $lwNode", e)
            }
        }
        lwTree.thisAndAllDescendants().forEach { lwNode ->
            val kNode = nodesMapping.byB(lwNode)!!
            // TODO populate values not already set at construction time
            if (kNode is KNode) {
                val lwRange = lwNode.getOnlyChildByContainmentName("range")
                if (lwRange != null) {
                    kNode.range = nodesMapping.byB(lwRange) as Range
                }
            }
        }
        referencesPostponer.populateReferences(nodesMapping)
        return nodesMapping.byB(lwTree)!!
    }

    fun prepareJsonSerialization(
        jsonSerialization: JsonSerialization =
            JsonSerialization.getStandardSerialization(),
    ): JsonSerialization {
        jsonSerialization.primitiveValuesSerialization.registerSerializer(
            StarLasuLWLanguage.char.id,
        ) { value -> "$value" }
        jsonSerialization.primitiveValuesSerialization.registerDeserializer(
            StarLasuLWLanguage.char.id,
        ) { serialized -> serialized[0] }
        languageConverter.knownLWLanguages().forEach {
            jsonSerialization.classifierResolver.registerLanguage(it)
        }
        languageConverter.knownKolasuLanguages().forEach { kolasuLanguage ->
            val lionwebLanguage = languageConverter.correspondingLanguage(kolasuLanguage)
            kolasuLanguage.enumClasses.forEach { enumClass ->
                val enumeration =
                    lionwebLanguage.elements.filterIsInstance<Enumeration>().find { it.name == enumClass.simpleName }!!
                val ec = enumClass
                jsonSerialization.primitiveValuesSerialization.registerSerializer(
                    enumeration.id!!,
                ) { value -> (value as Enum<*>).name }
                val values = ec.members.find { it.name == "values" }!!.call() as Array<Enum<*>>
                jsonSerialization.primitiveValuesSerialization.registerDeserializer(
                    enumeration.id!!,
                ) { serialized ->
                    if (serialized == null) {
                        null
                    } else {
                        values.find { it.name == serialized }
                            ?: throw RuntimeException(
                                "Cannot find enumeration value for $serialized (enum ${enumClass.qualifiedName})",
                            )
                    }
                }
            }
            kolasuLanguage.primitiveClasses.forEach { primitiveClass ->
                if (primitiveValueSerializations.containsKey(primitiveClass)) {
                    val lwPrimitiveType: PrimitiveType =
                        languageConverter
                            .getKolasuClassesToPrimitiveTypesMapping()[primitiveClass]
                            ?: throw IllegalStateException(
                                "No Primitive Type found associated to primitive value class " +
                                    "${primitiveClass.qualifiedName}",
                            )
                    val serializer = primitiveValueSerializations[primitiveClass]!! as PrimitiveValueSerialization<Any>
                    jsonSerialization.primitiveValuesSerialization.registerSerializer(
                        lwPrimitiveType.id!!,
                    ) { value -> serializer.serialize(value) }

                    jsonSerialization.primitiveValuesSerialization.registerDeserializer(
                        lwPrimitiveType.id!!,
                    ) { serialized -> serializer.deserialize(serialized) }
                }
            }
        }
        return jsonSerialization
    }

    /**
     * Deserialize nodes, taking into accaount the known languages.
     */
    fun deserializeToNodes(
        json: String,
        useDynamicNodesIfNeeded: Boolean = true,
    ): List<LWNode> {
        val js = prepareJsonSerialization()
        if (useDynamicNodesIfNeeded) {
            js.enableDynamicNodes()
        }
        return js.deserializeToNodes(json)
    }

    fun knownLWLanguages(): Set<LWLanguage> {
        return languageConverter.knownLWLanguages()
    }

    fun knownKolasuLanguages(): Set<KolasuLanguage> {
        return languageConverter.knownKolasuLanguages()
    }

    fun getKolasuClassesToClassifiersMapping(): Map<KClass<*>, Classifier<*>> {
        return languageConverter.getKolasuClassesToClassifiersMapping()
    }

    fun getClassifiersToKolasuClassesMapping(): Map<Classifier<*>, KClass<*>> {
        return languageConverter.getClassifiersToKolasuClassesMapping()
    }

    /**
     * Track reference values, so that we can populate them once the nodes are instantiated.
     */
    private class ReferencesPostponer {
        private val values = IdentityHashMap<ReferenceByName<PossiblyNamed>, LWNode?>()

        fun registerPostponedReference(
            referenceByName: ReferenceByName<PossiblyNamed>,
            referred: LWNode?,
        ) {
            values[referenceByName] = referred
        }

        fun populateReferences(nodesMapping: BiMap<Any, LWNode>) {
            values.forEach { entry ->
                if (entry.value == null) {
                    entry.key.referred = null
                } else {
                    if (entry.value is ProxyNode) {
                        entry.key.identifier = (entry.value as ProxyNode).id
                    } else {
                        entry.key.referred = nodesMapping.byB(entry.value as LWNode)!! as PossiblyNamed
                    }
                }
            }
        }
    }

    private fun <T : Any> instantiate(
        kClass: KClass<T>,
        data: LWNode,
        referencesPostponer: ReferencesPostponer,
    ): T {
        if (kClass == Range::class) {
            val start =
                instantiate(
                    Point::class,
                    data.getOnlyChildByContainmentName("start")!!,
                    referencesPostponer,
                )
            val end =
                instantiate(
                    Point::class,
                    data.getOnlyChildByContainmentName("end")!!,
                    referencesPostponer,
                )
            return Range(start, end) as T
        }
        if (kClass == Point::class) {
            return Point(
                data.getPropertyValueByName("line") as Int,
                data.getPropertyValueByName("column") as Int,
            ) as T
        }

        val constructor: KFunction<Any> =
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
            val feature = data.concept.getFeatureByName(param.name!!)
            if (feature == null) {
                TODO()
            } else {
                when (feature) {
                    is Property -> {
                        val propValue = data.getPropertyValue(feature)
                        if (propValue is DynamicEnumerationValue) {
                            val enumeration = propValue.enumeration
                            val kClass: KClass<out Enum<*>>? =
                                languageConverter
                                    .getEnumerationsToKolasuClassesMapping()[enumeration] as? KClass<out Enum<*>>
                            if (kClass == null) {
                                throw IllegalStateException("Cannot find Kolasu class for Enumeration $enumeration")
                            }
                            val entries =
                                kClass
                                    .java
                                    .methods
                                    .find {
                                        it.name == "getEntries"
                                    }!!
                                    .invoke(null) as List<Any>
                            val nameProp = kClass.memberProperties.find { it.name == "name" }!! as KProperty1<Any, *>
                            val namesToFields = entries.associate { nameProp.invoke(it) as String to it }
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
                            val kChildren = lwChildren.map { nodesMapping.byB(it)!! }
                            params[param] = kChildren
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
                                    null
                                } else {
                                    (
                                        nodesMapping.byB(lwChild)
                                            ?: throw IllegalStateException(
                                                "Unable to find Kolasu Node corresponding to $lwChild",
                                            )
                                    )
                                }
                            params[param] = kChild
                        }
                    }

                    else -> throw IllegalStateException()
                }
            }
        }
        try {
            return constructor.callBy(params) as T
        } catch (e: Exception) {
            throw RuntimeException(
                "Issue instantiating using constructor $constructor with params " +
                    "${params.map { "${it.key.name}=${it.value}" }}",
                e,
            )
        }
    }

    private fun findConcept(kNode: NodeLike): Concept {
        return languageConverter.correspondingConcept(kNode.javaClass.kotlin)
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
}

private val KNode.positionalID: String
    get() {
        return if (this.parent == null) {
            "root"
        } else {
            val cp = this.containingProperty()!!
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
