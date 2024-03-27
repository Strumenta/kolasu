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

// //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

abstract class SemanticsProvider<OutputType : Any, RuleType : SemanticsProviderRule<*, OutputType>>(
    val rules: MutableMap<KClass<out Node>,
        (Node, SemanticsProvider<OutputType, *>, MutableList<Issue>) -> OutputType?> = mutableMapOf()
) {
    fun <NodeType : Node> from(
        node: NodeType,
        issues: MutableList<Issue> = mutableListOf(),
        typedAs: KClass<in NodeType>? = null
    ): OutputType? {
        return this.rules.keys
            .filter { it.isSuperclassOf(typedAs ?: node::class) }
            .sortBySubclassesFirst()
            .firstOrNull()?.let { this.rules[it]?.invoke(node, this, issues) }
    }
}

// //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

abstract class SemanticsProviderConfigurator<
    ProviderType : SemanticsProvider<OutputType, RuleType>,
    RuleType : SemanticsProviderRule<*, OutputType>,
    OutputType : Any>(
    protected val provider: ProviderType
) {
    fun <InputType : Node> rule(
        inputType: KClass<InputType>,
        init: RuleType.(Triple<InputType, SemanticsProvider<OutputType, *>, MutableList<Issue>>) -> Unit
    ) {
        this.provider.rules[inputType] = { node, provider, issues ->
            require(inputType.isSuperclassOf(node::class)) {
                "Rule execution error: incompatible input received" +
                    " (received: ${node::class.qualifiedName}, expected: ${inputType.qualifiedName})"
            }
            val input = inputType.cast(node)
            val rule = this.createRule(inputType).apply { init(Triple(input, provider, issues)) }
            SemanticsProviderRuleEvaluator(rule).evaluate(input, provider, issues)
        }
    }

    protected abstract fun <InputType : Node> createRule(nodeType: KClass<InputType>): RuleType
}

// //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

private class SemanticsProviderRuleEvaluator<InputType : Node, OutputType : Any>(
    private val rule: SemanticsProviderRule<InputType, OutputType>
) {
    fun evaluate(
        node: Node,
        provider: SemanticsProvider<OutputType, *>,
        issues: MutableList<Issue>
    ) = this.rule.evaluate(node as InputType, provider, issues)
}

// //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

abstract class SemanticsProviderRule<InputType : Node, OutputType : Any> {

    private val delayedActions: MutableList<(
            Triple<InputType,
                SemanticsProvider<OutputType, *>, MutableList<Issue>>
        ) -> Unit> = mutableListOf()

    fun MutableList<Issue>.error(message: String, node: Node? = null) {
        this.issue(message, ERROR, node)
    }

    fun MutableList<Issue>.warning(message: String, node: Node? = null) {
        this.issue(message, WARNING, node)
    }

    fun MutableList<Issue>.info(message: String, node: Node? = null) {
        this.issue(message, INFO, node)
    }

    protected fun runBeforeEvaluation(
        action: (
            Triple<InputType,
                SemanticsProvider<OutputType, *>, MutableList<Issue>>
        ) -> Unit
    ) {
        this.delayedActions.add(action)
    }

    protected abstract fun getOutput(
        input: InputType,
        provider: SemanticsProvider<OutputType, *>,
        issues: MutableList<Issue>
    ): OutputType?

    internal fun evaluate(
        input: InputType,
        provider: SemanticsProvider<OutputType, *>,
        issues: MutableList<Issue>
    ): OutputType? {
        Triple(input, provider, issues).let { this.delayedActions.forEach { action -> action(it) } }
        return this.getOutput(input, provider, issues)
    }

    private fun MutableList<Issue>.issue(message: String, severity: IssueSeverity, node: Node? = null) {
        this.add(Issue.semantic(message, severity, node?.position))
    }
}

// //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
