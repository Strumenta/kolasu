package com.strumenta.kolasu.semantics.common

import com.strumenta.kolasu.ids.nodeIdProvider
import com.strumenta.kolasu.model.Named
import com.strumenta.kolasu.model.Node
import com.strumenta.kolasu.model.ReferenceByName
import com.strumenta.kolasu.semantics.scope.provider.scopeProvider
import com.strumenta.kolasu.semantics.symbol.provider.symbolProvider
import com.strumenta.kolasu.semantics.symbol.repository.SymbolRepository
import java.util.*

class TodoProject(override var name: String, val todos: MutableList<Todo>) : Node(), Named

class Todo(
    override var name: String,
    var description: String,
    val prerequisite: ReferenceByName<Todo>? = null
) : Node(), Named

val todosNodeIdProvider = nodeIdProvider {
    val identifiers: MutableMap<Node, String> = mutableMapOf()
    idFor(Node::class) {
        identifiers.getOrPut(it.node) { "${it.node::class.qualifiedName}::${UUID.randomUUID()}" }
    }
}

fun todosScopeProvider(symbolRepository: SymbolRepository? = null) = scopeProvider {
    rule(TodoProject::class) { (node) ->
        node.todos.forEach(this::include)
        parent {
            symbolRepository
                ?.all { it.type.isSuperTypeOf(Todo::class) }
                ?.forEach(this::include)
        }
    }
}

val todosSymbolProvider = symbolProvider(todosNodeIdProvider) {
    rule(Todo::class) { (todo) ->
        include("name", todo.name)
    }
}
