package com.strumenta.kolasu.lionwebclient

import com.strumenta.kolasu.ids.CommonNodeIdProvider
import com.strumenta.kolasu.ids.NodeIdProvider
import com.strumenta.kolasu.ids.caching
import com.strumenta.kolasu.language.KolasuLanguage
import com.strumenta.kolasu.language.StarLasuLanguage
import com.strumenta.kolasu.lionweb.KNode
import com.strumenta.kolasu.lionweb.LWLanguage
import com.strumenta.kolasu.lionweb.LWNode
import com.strumenta.kolasu.lionweb.LionWebModelConverter
import com.strumenta.kolasu.lionweb.LionWebSource
import com.strumenta.kolasu.lionweb.PerformanceLogger
import com.strumenta.kolasu.lionweb.PrimitiveValueSerialization
import com.strumenta.kolasu.lionweb.ProxyBasedNodeResolver
import com.strumenta.kolasu.model.ASTRoot
import com.strumenta.kolasu.model.Node
import com.strumenta.kolasu.model.assignParents
import com.strumenta.kolasu.traversing.walkDescendants
import io.lionweb.lioncore.java.language.Concept
import io.lionweb.lioncore.java.model.HasSettableParent
import io.lionweb.lioncore.java.serialization.JsonSerialization
import io.lionweb.lioncore.java.serialization.SerializationProvider
import io.lionweb.lioncore.java.serialization.UnavailableNodePolicy
import io.lionweb.lioncore.kotlin.repoclient.ClassifierResult
import io.lionweb.lioncore.kotlin.repoclient.LionWebClient
import io.lionweb.lioncore.kotlin.repoclient.RetrievalMode
import io.lionweb.lioncore.kotlin.repoclient.SerializationDecorator
import io.lionweb.lioncore.kotlin.repoclient.debugFileHelper
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1

/**
 * Partitions are top level entries in the repository. While they are still nodes, they must be dealt with through
 * specific methods.
 *
 * The main aspect to consider is how IDs are attributed to nodes. We distinguish two cases:
 * - Dependent ID Nodes (DIN): these are nodes which ID depends on their position in the tree (i.e., looking
 *   at the parents and nodes above). In other words, changing their parent or any ancestor of their parent, will
 *   change their ID.
 * - Independent ID Nodes (IIN) Nodes which can get an ID, based purely on themselves (i.e., without considering
 *   their parent or any ancestor
 *
 * "Container" nodes such as partitions or other nodes created to organize ASTs (e.g., to represent files and
 * directories) should be made as IIN. Also, the roots of ASTs should be made as IIN. All other nodes can be treated
 * as DIN.
 *
 * The problem with IIN is that we must create the conditions so that we get the same ID for each of them when we store
 * them and when we retrieve them from the LionWeb Repository, irrespectively of the fact that we store them directly
 * (or we store their parent or any ancestor) and that we retrieve them directly (or we retrieve their parent or any
 * ancestor).
 *
 * For a node to be IIN it should either (i) be a partition, (ii) being reported as being a source base node type,
 * or (iii) implement IDLogic.
 */
class KolasuClient(
    val hostname: String = "localhost",
    val port: Int = 3005,
    val debug: Boolean = false,
    val connectTimeOutInSeconds: Long = 60,
    val callTimeoutInSeconds: Long = 60,
    val authorizationToken: String? = null,
    val idProvider: NodeIdProvider = CommonNodeIdProvider().caching(),
) {
    /**
     * Exposed for testing purposes
     */
    val nodeConverter =
        LionWebModelConverter(idProvider).apply {
            externalNodeResolver = ProxyBasedNodeResolver
        }

    val lionWebClient =
        LionWebClient(
            hostname,
            port,
            debug = debug,
            jsonSerializationProvider = { this.jsonSerialization },
            connectTimeOutInSeconds = connectTimeOutInSeconds,
            callTimeoutInSeconds = callTimeoutInSeconds,
            authorizationToken = authorizationToken,
        )

    private val serializationDecorators = mutableListOf<SerializationDecorator>()

    init {
        registerSerializationDecorator {
            it.apply {
                enableDynamicNodes()
                unavailableParentPolicy = UnavailableNodePolicy.NULL_REFERENCES
                unavailableReferenceTargetPolicy = UnavailableNodePolicy.PROXY_NODES
            }
            nodeConverter.prepareSerialization(
                it,
            ) as JsonSerialization
        }
    }

    /**
     * Exposed for testing purposes
     */
    var jsonSerialization: JsonSerialization = calculateSerialization()
        private set

    fun updateSerialization() {
        this.jsonSerialization = calculateSerialization()
        lionWebClient.updateJsonSerialization()
    }

    private fun calculateSerialization(): JsonSerialization {
        val jsonSerialization = SerializationProvider.getStandardJsonSerialization()
        serializationDecorators.forEach { serializationDecorator -> serializationDecorator.invoke(jsonSerialization) }
        return jsonSerialization
    }

    //
    // Configuration
    //

    fun registerLanguage(starLasuLanguage: StarLasuLanguage) {
        val lionwebLanguage = nodeConverter.exportLanguageToLionWeb(starLasuLanguage)
        lionWebClient.registerLanguage(lionwebLanguage)
    }

    fun registerLanguage(kolasuLanguage: KolasuLanguage) {
        val lionwebLanguage = nodeConverter.exportLanguageToLionWeb(kolasuLanguage)
        lionWebClient.registerLanguage(lionwebLanguage)
    }

    fun registerLanguage(lionWebLanguage: LWLanguage) {
        lionWebClient.registerLanguage(lionWebLanguage)
    }

    fun <E : Any> registerPrimitiveValueSerialization(
        kClass: KClass<E>,
        primitiveValueSerialization: PrimitiveValueSerialization<E>,
    ) {
        nodeConverter.registerPrimitiveValueSerialization(kClass, primitiveValueSerialization)
    }

    //
    // Operation on partitions
    //

    fun getPartitionIDs(): List<String> {
        return lionWebClient.getPartitionIDs()
    }

    fun partitionExist(lwPartition: LWNode): Boolean {
        return partitionExist(lwPartition.id ?: throw java.lang.IllegalStateException("Node $lwPartition has no ID"))
    }

    fun partitionExist(partitionID: String): Boolean {
        return getPartitionIDs().contains(partitionID)
    }

    /**
     * We create the partition and returns its ID.
     *
     * The node specified should be of partition type.
     *
     * The node should not have children. If you want to create a partition with children, first create it without
     * children and then call updatePartition.
     *
     * The partition should not already exist.
     */
    fun createPartition(partition: LWNode): String {
        lionWebClient.createPartition(partition)
        return partition.id!!
    }

    /**
     * Consider this will retrieve the partition and all the roots it contains.
     * This may mean a very large amount of data.
     */
    fun retrievePartition(
        nodeID: String,
        retrievalMode: RetrievalMode = RetrievalMode.ENTIRE_SUBTREE,
    ): LWNode {
        val lwNode = lionWebClient.retrieve(nodeID, retrievalMode = retrievalMode)
        return lwNode
    }

    fun deletePartition(partitionId: String) {
        require(partitionExist(partitionId)) {
            "Partition does not exist"
        }
        lionWebClient.deletePartition(partitionId)
    }

    fun deletePartition(partition: LWNode) {
        deletePartition(partition.id!!)
    }

    //
    // Operation on ASTs
    //

    fun astNodeExist(kNode: KNode): Boolean {
        return nodeExist(idFor(kNode))
    }

    fun astNodeExistWithExplanation(kNode: KNode): String? {
        return nodeExistWithExplanation(idFor(kNode))
    }

    fun attachAST(
        kNode: Node,
        containerID: String,
        containmentName: String,
        containmentIndex: Int,
    ): String {
        require(kNode::class.annotations.any { it is ASTRoot }) {
            "The class of root of the passed is not marked as ASTRoot (root: $kNode)"
        }
        val lwTreeToAppend = toLionWeb(kNode, containerID, containmentName, containmentIndex)
        considerLogging("attachAST - prepared lwTreeToAppend")
        debugFile("createNode-${lwTreeToAppend.id}.json") {
            (nodeConverter.prepareSerialization() as JsonSerialization).serializeTreesToJsonString(lwTreeToAppend)
        }
        considerLogging("attachAST - debug file prepared")
        lionWebClient.appendTree(lwTreeToAppend, containerID, containmentName, containmentIndex)
        considerLogging("attachAST - actual lionweb appending done")
        return lwTreeToAppend.id!!
    }

    var performanceLogging: Boolean = false

    fun attachAST(
        kNode: Node,
        containerID: String,
        containmentName: String,
    ): String {
        val containmentIndex = lionWebClient.childrenInContainment(containerID, containmentName).size
        considerLogging("got containment index")
        return attachAST(kNode, containerID, containmentName, containmentIndex)
    }

    private fun considerLogging(message: String) {
        if (performanceLogging) {
            PerformanceLogger.log(message)
        }
    }

    fun attachAST(
        kNode: Node,
        container: LWNode,
        containment: KProperty1<*, *>,
    ): String {
        return attachAST(kNode, container.id ?: throw java.lang.IllegalStateException(), containment.name)
    }

    fun attachAST(
        kNode: Node,
        container: LWNode,
        containmentName: String,
    ): String {
        return attachAST(kNode, container.id ?: throw java.lang.IllegalStateException(), containmentName)
    }

    fun attachAST(
        kNode: Node,
        containerID: String,
        containment: KProperty1<*, *>,
    ): String {
        val containmentIndex = lionWebClient.childrenInContainment(containerID, containment.name).size
        return attachAST(kNode, containerID, containment.name, containmentIndex)
    }

    fun attachAST(
        kNode: Node,
        containerID: String,
        containment: KProperty1<*, *>,
        containmentIndex: Int,
    ): String {
        return attachAST(kNode, containerID, containment.name, containmentIndex)
    }

    fun updateAST(kNode: KNode): String {
        require(kNode::class.annotations.any { it is ASTRoot })
        val msg = nodeExistWithExplanation(idFor(kNode))
        require(msg == null) {
            "We can only update existing nodes. While this is not a valid node because: $msg"
        }
        kNode.assignParents()
        val lwNode = toLionWeb(kNode)
        require(lwNode is HasSettableParent) // In the future we may relax that
        // Now, if the parent of this node is null we need to find the real parent from the model repository
        // Otherwise we need to be sure to set the parent anyway
        if (lwNode.parent == null) {
            val parentIdOnServer = lionWebClient.getParentId(lwNode.id!!)
            (lwNode as HasSettableParent).setParentID(parentIdOnServer)
        }
        lionWebClient.storeTree(lwNode)
        return lwNode.id!!
    }

    /**
     * While we can add or update only entire ASTs, we can retrieve any node we want.
     */
    fun getAST(nodeID: String): KNode {
        val lwNode = lionWebClient.retrieve(nodeID)
        return toKolasuNode(lwNode)
    }

    fun toKolasuNode(lwNode: LWNode): KNode {
        val result = nodeConverter.importModelFromLionWeb(lwNode) as KNode
        // We actually do not know if this is a source or not...
        result.source = LionWebSource(lwNode.id!!)
        return result
    }

    //
    // Operation on LionWeb Nodes which are NOT partitions
    //

    fun nodeExist(lwNode: LWNode): Boolean {
        return nodeExist(lwNode.id ?: throw java.lang.IllegalStateException())
    }

    fun nodeExist(nodeId: String): Boolean {
        return nodeExistWithExplanation(nodeId) == null
    }

    fun nodeExistWithExplanation(lwNode: LWNode): String? {
        return nodeExistWithExplanation(lwNode.id ?: throw java.lang.IllegalStateException())
    }

    fun nodeExistWithExplanation(nodeId: String): String? {
        if (!lionWebClient.isNodeExisting(nodeId)) {
            return "Node with ID $nodeId not found"
        }
        val parentId = lionWebClient.getParentId(nodeId)
        return if (parentId == null) {
            "Node with ID $nodeId has null parent, so it is a partition and not a normal node"
        } else {
            null
        }
    }

    fun getLionWebNode(
        nodeID: String,
        withProxyParent: Boolean = false,
    ): LWNode {
        return lionWebClient.retrieve(nodeID, withProxyParent)
    }

    fun attachLionWebChild(
        child: LWNode,
        parent: LWNode,
        property: KProperty1<*, *>,
        skipRetrievalOfParent: Boolean = false,
    ): String {
        if (skipRetrievalOfParent) {
            return attachLionWebChild(child, property.name!!, parent)
        }
        return attachLionWebChild(child, parent.id!!, property.name!!)
    }

    fun attachLionWebChild(
        child: LWNode,
        parentID: String,
        property: KProperty1<*, *>,
    ): String {
        return attachLionWebChild(child, parentID, property.name!!)
    }

    fun attachLionWebChild(
        child: LWNode,
        parentID: String,
        propertyName: String,
    ): String {
        val updatedParent =
            lionWebClient.retrieve(
                parentID,
                withProxyParent = true,
                retrievalMode = RetrievalMode.SINGLE_NODE,
            )
        return attachLionWebChild(child, updatedParent, propertyName)
    }

    fun attachLionWebChild(
        child: LWNode,
        propertyName: String,
        providedUpdatedParent: LWNode,
    ): String {
        return attachLionWebChild(child, providedUpdatedParent, propertyName)
    }

    fun attachLionWebChild(
        child: LWNode,
        parent: LWNode,
        propertyName: String,
    ): String {
        parent.addChild(parent.classifier.requireContainmentByName(propertyName), child)
        lionWebClient.storeTree(parent)
        (child as HasSettableParent).setParentID(parent.id)
        return child.id!!
    }

    fun updateLionWebNode(lwNode: LWNode) {
        return lionWebClient.storeTree(lwNode)
    }

    fun getShallowLionWebNode(
        nodeID: String,
        withProxyParent: Boolean = false,
    ): LWNode {
        return lionWebClient.retrieve(nodeID, withProxyParent, retrievalMode = RetrievalMode.SINGLE_NODE)
    }

    fun getShallowLionWebNodes(
        nodeIDs: List<String>,
        withProxyParent: Boolean = false,
    ): List<LWNode> {
        return lionWebClient.retrieve(nodeIDs, withProxyParent, retrievalMode = RetrievalMode.SINGLE_NODE)
    }

    //
    // Other operations
    //

    /**
     * Return the Node ID associated to the Node. If the Client has already "seen"
     * the Node before associated to a particular Node ID (either during insertion or retrieval)
     * such Node ID will be returned. Otherwise the Node ID will be calculated based on the Node itself.
     *
     * Note that if you call this method on a Node before inserting it on the repository, we will not know
     * _where_ in the repository you will insert it, therefore the ID you will get would be the one for the
     * Node as "dangling in the void". The Node ID obtained after the insertion could be different!
     */
    fun idFor(kNode: KNode): String {
        return idProvider.id(kNode)
    }

    fun nodesByConcept(): Map<KClass<*>, ClassifierResult> {
        val lionwebResult = lionWebClient.nodesByClassifier()
        val kolasuResult =
            lionwebResult
                .map { entry ->
                    val languageKey = entry.key.languageKey
                    val lionWebLanguage = nodeConverter.knownLWLanguages().find { it.key == languageKey }
                    if (lionWebLanguage == null) {
                        null
                    } else {
                        val lionWebClassifier = lionWebLanguage.elements.find { it.key == entry.key.classifierKey }
                        if (lionWebClassifier is Concept) {
                            val kolasuClass = nodeConverter.getClassifiersToKolasuClassesMapping()[lionWebClassifier]
                            if (kolasuClass == null) {
                                null
                            } else {
                                kolasuClass to entry.value
                            }
                        } else {
                            throw IllegalStateException(
                                "Classifier $lionWebClassifier is unexpected, as it is not a Concept",
                            )
                        }
                    }
                }.filterNotNull()
                .toMap()
        return kolasuResult
    }

    //
    // Private methods
    //

    private fun toLionWeb(kNode: KNode): LWNode {
        require(kNode.javaClass.annotations.any { it is ASTRoot })
        kNode.assignParents()
        kNode.walkDescendants().forEach { descendant ->
            if (descendant.source == null) {
                descendant.source = kNode.source
            }
        }
        nodeConverter.clearNodesMapping()
        return nodeConverter.exportModelToLionWeb(kNode, idProvider, considerParent = true)
    }

    fun toLionWeb(
        kNode: Node,
        containerID: String,
        containmentName: String,
        containmentIndex: Int,
    ): LWNode {
        require(kNode.javaClass.annotations.any { it is ASTRoot })
        nodeConverter.clearNodesMapping()
        return nodeConverter.exportModelToLionWeb(
            kNode,
            idProvider,
            considerParent = true,
        )
    }

    private fun debugFile(
        relativePath: String,
        text: () -> String,
    ) {
        debugFileHelper(debug, relativePath, text)
    }

    fun registerSerializationDecorator(decorator: SerializationDecorator) {
        // We do not need to specify them also for the lionWebClient, as it uses ours version of JsonSerialization
        serializationDecorators.add(decorator)
    }
}
