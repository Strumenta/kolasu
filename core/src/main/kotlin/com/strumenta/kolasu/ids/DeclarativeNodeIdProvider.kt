package com.strumenta.kolasu.ids

import com.strumenta.kolasu.model.Node
import com.strumenta.kolasu.validation.Issue
import com.strumenta.kolasu.validation.IssueSeverity
import com.strumenta.kolasu.validation.IssueSeverity.ERROR
import com.strumenta.kolasu.validation.IssueSeverity.INFO
import com.strumenta.kolasu.validation.IssueSeverity.WARNING
import kotlin.reflect.KClass
import kotlin.reflect.full.isSuperclassOf

// TODO merge into NodeIdProvider.kt file

/**
 * Utility function to define a [NodeIdProvider] for a specific language.
 * @param init the configuration rules for the [NodeIdProvider]
 * @return a [NodeIdProvider] instance with the configured rules
 **/
fun nodeIdProvider(
    init: NodeIdProviderConfigurationApi.() -> Unit
): NodeIdProvider {
    return ConfigurableNodeIdProvider().apply(init)
}

@DslMarker
annotation class NodeIdProviderDsl

/**
 * Defines the NodeId Provider Configuration Api
 * to use while configuring a [NodeIdProvider] instance.
 **/
@NodeIdProviderDsl
sealed interface NodeIdProviderConfigurationApi {
    /**
     * Define the [String] identifier for the given [nodeType]
     * using the given [rule].
     * @param nodeType the node type for which this rule applies
     * @param rule the node id provider rule
     **/
    fun <NodeTy : Node> idFor(
        nodeType: KClass<out NodeTy>,
        rule: NodeIdProviderConfigurationRuleApi.(NodeIdProviderConfigurationRuleContext<out NodeTy>) -> String
    )
}

/**
 * Defines the API when defining a node id provider
 * rule for a specific node type.
 **/
@NodeIdProviderDsl
sealed interface NodeIdProviderConfigurationRuleApi {
    /**
     * Add an information [message] during the rule execution.
     * @param message the information message content
     **/
    fun info(message: String)

    /**
     * Add a warning [message] during the rule execution
     **/
    fun warning(message: String)

    /**
     * Add an error [message] during the rule execution
     **/
    fun error(message: String)
}

private class ConfigurableNodeIdProvider : NodeIdProviderConfigurationApi, NodeIdProvider {

    private val rules: MutableList<ConfigurableNodeIdProviderRule<*>> = mutableListOf()

    override fun <NodeTy : Node> idFor(
        nodeType: KClass<out NodeTy>,
        rule: NodeIdProviderConfigurationRuleApi.(NodeIdProviderConfigurationRuleContext<out NodeTy>) -> String
    ) {
        this.rules.add(ConfigurableNodeIdProviderRule(nodeType, rule))
    }

    override fun id(kNode: Node): String {
        return this.rules.sorted()
            .firstOrNull { it.canBeInvokedWithReceiver(kNode::class) }
            ?.invoke(this, kNode, mutableListOf())
            ?: throw RuntimeException("Error while retrieving node id - no compatible rule found")
    }
}

private class ConfigurableNodeIdProviderRule<NodeTy : Node>(
    private val nodeType: KClass<NodeTy>,
    private val configuration: NodeIdProviderConfigurationRuleApi
    .(NodeIdProviderConfigurationRuleContext<NodeTy>) -> String
) : NodeIdProviderConfigurationRuleApi,
    (NodeIdProvider, Node, MutableList<Issue>) -> String,
    Comparable<ConfigurableNodeIdProviderRule<*>> {

    private lateinit var context: NodeIdProviderConfigurationRuleContext<NodeTy>
    private lateinit var issues: MutableList<Issue>

    override fun info(message: String) {
        this.issue(message, INFO)
    }

    override fun warning(message: String) {
        this.issue(message, WARNING)
    }

    override fun error(message: String) {
        this.issue(message, ERROR)
    }

    private fun issue(message: String, severity: IssueSeverity) {
        this.issues.add(Issue.semantic(message, severity, this.context.node.position))
    }

    override fun compareTo(other: ConfigurableNodeIdProviderRule<*>): Int {
        return when {
            this.nodeType.isSuperclassOf(other.nodeType) -> 1
            other.nodeType.isSuperclassOf(this.nodeType) -> -1
            else -> (this.nodeType.qualifiedName ?: "") compareTo (other.nodeType.qualifiedName ?: "")
        }
    }

    fun canBeInvokedWithReceiver(nodeType: KClass<*>): Boolean {
        return this.nodeType.isSuperclassOf(nodeType)
    }

    override fun invoke(nodeIdProvider: NodeIdProvider, node: Node, issues: MutableList<Issue>): String {
        check(this.canBeInvokedWithReceiver(node::class)) {
            "Error while running node id provider rule - incompatible node received"
        }
        this.context = NodeIdProviderConfigurationRuleContext(node as NodeTy, nodeIdProvider)
        this.issues = issues
        return this.configuration(this.context)
    }
}

/**
 * The execution context while evaluating a node id provider rule.
 * @property node the node for which the identifier is being computed
 * @property nodeIdProvider the node id provider that we are configuring (self-reference)
 **/
data class NodeIdProviderConfigurationRuleContext<NodeTy : Node>(
    val node: NodeTy,
    val nodeIdProvider: NodeIdProvider
)
