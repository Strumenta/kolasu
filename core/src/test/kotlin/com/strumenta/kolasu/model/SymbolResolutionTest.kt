package com.strumenta.kolasu.model

import com.strumenta.kolasu.traversing.findAncestorOfType
import org.junit.Test

// symbols

data class FunctionSymbol(override var name: String) : Symbol, Node()

data class VariableSymbol(override var name: String) : Symbol, Node()

// nodes
sealed class StatementNode : Node()

class FunctionDeclaration(
    var name: String,
    var statements: MutableList<StatementNode> = mutableListOf(),
) : StatementNode(), SymbolProvider {
    override val symbols: List<Symbol> by lazy {
        listOf(FunctionSymbol(name = this.name))
    }
}

data class VariableDeclaration(var name: String) : StatementNode(), SymbolProvider {
    override val symbols: List<VariableSymbol> by lazy {
        listOf(VariableSymbol(name = this.name))
    }
}

data class PropertyRefExpression(
    val context: StatementNode? = null,
    val property: ReferenceByName<VariableSymbol>,
) : StatementNode()

data class VariableAssignment(var variable: ReferenceByName<VariableSymbol>) : StatementNode()

interface ExpressionNode

data class PropertyAccessExpr(
    val context: ExpressionNode? = null,
    val property: ReferenceByName<VariableSymbol>,
) : ExpressionNode

val exampleScopeProvider = declarativeScopeProvider {

    scopeFor(VariableAssignment::variable) { context: VariableAssignment ->
        println("VariableAssignment::variable(VariableAssignment)")
        Scope(symbols = mutableMapOf("example" to mutableListOf(VariableSymbol(name = "Example"))))
    }

    scopeFor(VariableAssignment::variable) { context: FunctionDeclaration ->
        val parentScope = context.findAncestorOfType(FunctionDeclaration::class.java)
            ?.let { getScope(it, VariableAssignment::variable) }
        val scope = Scope(parent = parentScope)
        context.statements.filterIsInstance<VariableDeclaration>()
            .map { variableDeclaration -> VariableSymbol(name = variableDeclaration.name) }
            .forEach { variableSymbol -> scope.define(variableSymbol) }
        scope
    }

    scopeFor(VariableAssignment::variable) { context: Node -> Scope() }
}

class SymbolResolutionTest {

    @Test
    fun example() {
        val functionDeclaration = FunctionDeclaration(name = "exampleFunction")

        val variableDeclaration = VariableDeclaration(name = "exampleVariable")
        functionDeclaration.statements.add(variableDeclaration)

        val innerFunctionDeclaration = FunctionDeclaration(name = "innerFunction")
        functionDeclaration.statements.add(innerFunctionDeclaration)

        val variableReference = VariableAssignment(variable = ReferenceByName("exampleVariable"))
        innerFunctionDeclaration.statements.add(variableReference)

        functionDeclaration.assignParents()

        val scope = exampleScopeProvider.getScope(variableReference.parent!!, VariableAssignment::variable)

        println(scope)
    }
}
