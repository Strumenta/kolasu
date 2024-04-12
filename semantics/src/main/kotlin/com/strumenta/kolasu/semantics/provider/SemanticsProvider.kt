package com.strumenta.kolasu.semantics.provider

import com.strumenta.kolasu.model.Node
import com.strumenta.kolasu.utils.sortBySubclassesFirst
import com.strumenta.kolasu.validation.Issue
import com.strumenta.kolasu.validation.IssueSeverity
import com.strumenta.kolasu.validation.IssueSeverity.ERROR
import com.strumenta.kolasu.validation.IssueSeverity.INFO
import com.strumenta.kolasu.validation.IssueSeverity.WARNING
import kotlin.reflect.KClass
import kotlin.reflect.cast
import kotlin.reflect.full.isSuperclassOf

/**
 * Base class for the query-side of semantic enrichment components.
 * It should be extended when defining new components navigating AST
 * nodes and producing a value for each of these using its set of [rules].
 *
 * @param OutputTy the output type produced by rules
 * @param RuleTy the rule type used to produce outputs
 * @property rules the rules used by the component to provide a value
 *
 * @author Lorenzo Addazi <lorenzo.addazi@strumenta.com>
 **/
abstract class SemanticsProvider<OutputTy : Any, RuleTy : SemanticsProviderRule<*, OutputTy>>(
    val rules: MutableMap<KClass<out Node>,
        (Node, SemanticsProvider<OutputTy, *>, MutableList<Issue>) -> OutputTy?> = mutableMapOf()
) {
    /**
     * Retrieve a value for the given [node] - optionally cast to [typedAs].
     *
     * @param node the node from which to produce the value
     * @param issues a list of issues where to attach possible errors, warnings and infos
     * @param typedAs optional class to cast the [node]
     *
     * @return the [OutputTy] instance produced by the rule for [node]
     **/
    fun <NodeTy : Node> getFor(
        node: NodeTy,
        issues: MutableList<Issue> = mutableListOf(),
        typedAs: KClass<in NodeTy>? = null
    ): OutputTy? {
        return this.rules.keys
            .filter { it.isSuperclassOf(typedAs ?: node::class) }
            .sortBySubclassesFirst()
            .firstOrNull()?.let { this.rules[it]?.invoke(node, this, issues) }
    }
}

/**
 * Base class for the configuration-side of semantic enrichment components.
 * It should be extended to specify the configuration API for components
 * navigating AST nodes and producing a corresponding value.
 *
 * @param ProviderTy the [SemanticsProvider] to configure
 * @param RuleTy the [SemanticsProviderRule] used by the provider
 * @param OutputTy the type of output produced when evaluating a rule
 * @property provider the configured [SemanticsProvider]
 *
 * @author Lorenzo Addazi <lorenzo.addazi@strumenta.com>
 **/
abstract class SemanticsProviderConfigurator<
    ProviderTy : SemanticsProvider<OutputTy, RuleTy>,
    RuleTy : SemanticsProviderRule<*, OutputTy>,
    OutputTy : Any>(
    protected val provider: ProviderTy
) {
    /**
     * Specify the rule for the given [nodeType].
     *
     * @param nodeType the input type of the rule
     * @param init the configuration rule
     **/
    fun <NodeTy : Node> rule(
        nodeType: KClass<NodeTy>,
        init: RuleTy.(Triple<NodeTy, SemanticsProvider<OutputTy, *>, MutableList<Issue>>) -> Unit
    ) {
        this.provider.rules[nodeType] = { node, provider, issues ->
            require(nodeType.isSuperclassOf(node::class)) {
                "Rule execution error: incompatible input received" +
                    " (received: ${node::class.qualifiedName}, expected: ${nodeType.qualifiedName})"
            }
            val input = nodeType.cast(node)
            val rule = this.createRule(nodeType).apply { init(Triple(input, provider, issues)) }
            SemanticsProviderRuleEvaluator(rule).evaluate(input, provider, issues)
        }
    }

    /**
     * Create an instance of the [SemanticsProviderRule] used by this component.
     * @param nodeType the input type of rule
     * @return the corresponding [RuleTy] instance
     **/
    protected abstract fun <NodeTy : Node> createRule(nodeType: KClass<NodeTy>): RuleTy
}

/**
 * Internal component responsible for the evaluation
 * of [SemanticsProviderRule] instances.
 *
 * @param NodeTy the input type of the rule
 * @param OutputTy the output type of the rule
 * @property rule the rule to evaluate
 *
 * @author Lorenzo Addazi <lorenzo.addazi@strumenta.com>
 **/
private class SemanticsProviderRuleEvaluator<NodeTy : Node, OutputTy : Any>(
    private val rule: SemanticsProviderRule<NodeTy, OutputTy>
) {
    /**
     * Evaluate the rule with the given [node], [provider] and [issues].
     *
     * @param node the node to use as input
     * @param provider the provider containing the rule
     * @param issues a list of issues where to attach errors, warnings, infos
     * @return the corresponding [OutputTy] instance
     **/
    fun evaluate(
        node: Node,
        provider: SemanticsProvider<OutputTy, *>,
        issues: MutableList<Issue>
    ) = this.rule.evaluate(node as NodeTy, provider, issues)
}

/**
 * Base class for the definition of a [SemanticsProvider] rule.
 *
 * @param NodeTy the type of the input
 * @param OutputTy the type of the output
 *
 * @author Lorenzo Addazi <lorenzo.addazi@strumenta.com>
 **/
abstract class SemanticsProviderRule<NodeTy : Node, OutputTy : Any> {

    /**
     * List of actions to execute right before the rule evaluation.
     **/
    private val delayedActions: MutableList<(
            Triple<NodeTy,
                SemanticsProvider<OutputTy, *>, MutableList<Issue>>
        ) -> Unit> = mutableListOf()

    /**
     * Utility extension method to add an error to the list of issues.
     *
     * @param message the message of the error
     * @param node the node associated to the error
     **/
    fun MutableList<Issue>.error(message: String, node: Node? = null) {
        this.issue(message, ERROR, node)
    }

    /**
     * Utility extension method to add a warning to the list of issues
     *
     * @param message the message of the warning
     * @param node the node associated to the warning
     **/
    fun MutableList<Issue>.warning(message: String, node: Node? = null) {
        this.issue(message, WARNING, node)
    }

    /**
     * Utility extension method to add an info to the list of issues
     *
     * @param message the info message
     * @param node the node associated to the info
     **/
    fun MutableList<Issue>.info(message: String, node: Node? = null) {
        this.issue(message, INFO, node)
    }

    /**
     * Utility method to defer actions on rule evaluation time.
     *
     * @param action the action to execute
     **/
    protected fun runBeforeEvaluation(
        action: (
            Triple<NodeTy,
                SemanticsProvider<OutputTy, *>, MutableList<Issue>>
        ) -> Unit
    ) {
        this.delayedActions.add(action)
    }

    /**
     * Produce the [OutputTy] instance when evaluating the rule
     * with [node], [provider] and [issues] as input.
     *
     * @param node the node input
     * @param provider the provider containing the rule
     * @param issues a list of issues where to attach errors, warnings and info
     *
     * @return the corresponding [OutputTy] instance
     **/
    protected abstract fun getOutput(
        node: NodeTy,
        provider: SemanticsProvider<OutputTy, *>,
        issues: MutableList<Issue>
    ): OutputTy?

    /**
     * Internal functions evaluating the rule.
     *
     * @param node the node input
     * @param provider the provider containing the rule
     * @param issues a list of issues where to attach errors, warnings and info
     *
     * @return the corresponding [OutputTy] instance
     **/
    internal fun evaluate(
        node: NodeTy,
        provider: SemanticsProvider<OutputTy, *>,
        issues: MutableList<Issue>
    ): OutputTy? {
        Triple(node, provider, issues).let { this.delayedActions.forEach { action -> action(it) } }
        return this.getOutput(node, provider, issues)
    }

    /**
     * Private method attaching an issues to the list of issues.
     *
     * @param message the issue message
     * @param severity the issue severity
     * @param node the node associated to the issue
     **/
    private fun MutableList<Issue>.issue(message: String, severity: IssueSeverity, node: Node? = null) {
        this.add(Issue.semantic(message, severity, node?.position))
    }
}
