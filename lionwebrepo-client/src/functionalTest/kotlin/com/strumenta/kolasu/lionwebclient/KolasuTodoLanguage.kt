package com.strumenta.kolasu.lionwebclient

import com.strumenta.kolasu.ids.NodeIdProvider
import com.strumenta.kolasu.language.StarLasuLanguage
import com.strumenta.kolasu.language.explore
import com.strumenta.kolasu.lionweb.addConcept
import com.strumenta.kolasu.lionweb.addContainment
import com.strumenta.kolasu.lionweb.lwLanguage
import com.strumenta.kolasu.model.ASTRoot
import com.strumenta.kolasu.model.LanguageAssociation
import com.strumenta.kolasu.model.Multiplicity
import com.strumenta.kolasu.model.Named
import com.strumenta.kolasu.model.Node
import com.strumenta.kolasu.model.ReferenceValue
import com.strumenta.kolasu.semantics.scope.provider.declarative.DeclarativeScopeProvider
import com.strumenta.kolasu.semantics.scope.provider.declarative.scopeFor
import com.strumenta.kolasu.semantics.symbol.provider.declarative.DeclarativeSymbolProvider
import com.strumenta.kolasu.semantics.symbol.provider.declarative.symbolFor
import com.strumenta.kolasu.semantics.symbol.repository.SymbolRepository
import io.lionweb.lioncore.java.language.LionCoreBuiltins
import io.lionweb.lioncore.java.model.impl.DynamicNode

val todoAccountLanguage =
    lwLanguage("todoAccountLanguage").apply {
        addConcept("TodoAccount").apply {
            addContainment("projects", LionCoreBuiltins.getNode(), Multiplicity.MANY)
        }
    }

val todoAccountConcept by lazy {
    todoAccountLanguage.getConceptByName("TodoAccount")
}

class TodoAccount(
    id: String,
) : DynamicNode(id, todoAccountConcept!!)

@LanguageAssociation(TodoStarLasuLanguageInstance::class)
@ASTRoot
data class TodoProject(
    override var name: String,
    val todos: MutableList<Todo> = mutableListOf(),
) : Node(),
    Named

@LanguageAssociation(TodoStarLasuLanguageInstance::class)
data class Todo(
    override var name: String,
    var description: String,
    val prerequisite: ReferenceValue<Todo>? = null,
) : Node(),
    Named {
    constructor(name: String) : this(name, name)
}

object TodoStarLasuLanguageInstance : StarLasuLanguage("Todo") {
    init {
        explore(Todo::class, TodoProject::class)
    }
}

class TodoSymbolProvider(
    nodeIdProvider: NodeIdProvider,
) : DeclarativeSymbolProvider(
        nodeIdProvider,
        symbolFor<Todo> {
            this.name(it.node.name)
        },
    )

class TodoScopeProvider(
    val sri: SymbolRepository,
) : DeclarativeScopeProvider(
        scopeFor(Todo::prerequisite) {
            // We first consider local todos, as they may shadow todos from other projects
            (it.node.parent as TodoProject).todos.forEach {
                define(it)
            }
            // We then consider all symbols from the sri. Note that nodes of the current project
            // appear both as nodes and as symbols
            sri.find(Todo::class).forEach {
                define(it.name!!, it)
            }
        },
    )
