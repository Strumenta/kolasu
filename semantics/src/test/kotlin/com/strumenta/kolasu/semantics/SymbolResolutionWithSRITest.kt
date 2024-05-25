package com.strumenta.kolasu.semantics

import com.strumenta.kolasu.model.ASTRoot
import com.strumenta.kolasu.model.Named
import com.strumenta.kolasu.model.Node
import com.strumenta.kolasu.model.ReferenceByName
import com.strumenta.kolasu.model.assignParents
import com.strumenta.kolasu.semantics.indexing.InMemoryIndex
import com.strumenta.kolasu.semantics.indexing.Index
import com.strumenta.kolasu.semantics.indexing.IndexWriter
import com.strumenta.kolasu.semantics.indexing.indexWriter
import com.strumenta.kolasu.semantics.linking.ReferenceResolver
import com.strumenta.kolasu.semantics.linking.referenceResolver
import com.strumenta.kolasu.semantics.scoping.ScopeComputer
import com.strumenta.kolasu.semantics.scoping.scope
import com.strumenta.kolasu.semantics.scoping.scopeComputer
import com.strumenta.kolasu.semantics.services.SymbolIndexer
import com.strumenta.kolasu.semantics.services.SymbolResolver
import com.strumenta.kolasu.traversing.findAncestorOfType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Representation of a project.
 * @property name the name of the project
 * @property todos the tasks of the project
 **/
@ASTRoot
class TodoProject(
    override var name: String,
    val todos: MutableList<LocalTodo>
) : Node(), Named

/**
 * Abstract representation of a task.
 * @property name the name of the task
 **/
sealed class Todo(
    override var name: String
) : Node(), Named

/**
 * Local representation of a task.
 * @property name the name of the task
 * @property description the description of the task
 * @property prerequisite optional reference to tasks it depends on
 **/
data class LocalTodo(
    override var name: String,
    var description: String,
    val prerequisite: ReferenceByName<Todo>? = null
) : Todo(name)

/**
 * External representation of a task
 * @property name the name of the task
 **/
data class ExternalTodo(
    override var name: String
) : Todo(name)

/**
 * Creates a scope computer for todos.
 **/
fun createTodoScopeComputer(index: Index = InMemoryIndex()): ScopeComputer {
    return scopeComputer(index) {
        scopeFrom(TodoProject::class) { todoProject ->
            scope {
                // all tasks contained in the project
                todoProject.todos.forEach { this.define(it.name, it) }
                // all tasks contained in external projects (as parent scope)
                parents += scope {
                    indexReader.findAll<ExternalTodo>().forEach { this.define(it.name, it) }
                }
            }
        }
    }
}

/**
 * Creates a reference resolver for todos.
 **/
fun createTodoReferenceResolver(scopeComputer: ScopeComputer, index: Index = InMemoryIndex()): ReferenceResolver {
    return referenceResolver(index) {
        resolve(LocalTodo::prerequisite) { (todo, name) ->
            todo.findAncestorOfType(TodoProject::class.java)
                ?.let(scopeComputer::scopeFrom)
                ?.entries<Todo> { (currentName) -> name == currentName }
                ?.firstOrNull()
        }
    }
}

/**
 * Creates an index writer for todos.
 **/
fun createTodoIndexWriter(index: Index = InMemoryIndex()): IndexWriter {
    return indexWriter(index) {
        write(LocalTodo::class) { localTodo ->
            ExternalTodo(name = localTodo.name)
        }
    }
}

class SymbolResolverWithSRITest {

    @Test
    fun symbolResolutionPointingToNodes() {
        val todo1 = LocalTodo("todo1", "stuff to do 1")
        val todo2 = LocalTodo("todo2", "stuff to do 2", prerequisite = ReferenceByName("todo1"))
        val todo3 = LocalTodo("todo3", "stuff to do 3")
        val todoProject = TodoProject("Personal", mutableListOf(todo1, todo2, todo3))
        todoProject.assignParents()

        assertEquals(false, todo2.prerequisite!!.resolved)

        // resolve all references in 'Personal' project
        val todoScopeProvider = createTodoScopeComputer()
        val todoReferenceResolver = createTodoReferenceResolver(todoScopeProvider)
        val todoSymbolResolver = SymbolResolver(todoReferenceResolver)
        todoSymbolResolver.resolveTree(todoProject)

        assertTrue(todo2.prerequisite.retrieved)
        assertEquals(todo1, todo2.prerequisite.referred)
    }

    @Test
    fun symbolResolutionPointingToSymbols() {
        val todo1 = LocalTodo("todo1", "stuff to do 1")
        val todo2 = LocalTodo("todo2", "stuff to do 2")
        val todo3 = LocalTodo("todo3", "stuff to do 3")
        val todoProjectPersonal = TodoProject("Personal", mutableListOf(todo1, todo2, todo3))
        todoProjectPersonal.assignParents()

        val todo4 = LocalTodo("todo4", "Some stuff to do", ReferenceByName("todo2"))
        val todoProjectErrands = TodoProject("Errands", mutableListOf(todo4))
        todoProjectErrands.assignParents()

        // setup index and indexer
        val index = InMemoryIndex()
        val indexWriter = createTodoIndexWriter(index)
        val symbolIndexer = SymbolIndexer(indexWriter)

        // index all symbols from 'Project' project
        symbolIndexer.indexTree(todoProjectPersonal)

        // verify that todo4.prerequisite is currently unresolved
        assertNotNull(todo4.prerequisite)
        assertFalse(todo4.prerequisite.retrieved)
        assertNull(todo4.prerequisite.referred)

        // resolve all references in 'Errands' project
        val todoScopeProvider = createTodoScopeComputer(index)
        val todoReferenceResolver = createTodoReferenceResolver(todoScopeProvider, index)
        val todoSymbolResolver = SymbolResolver(todoReferenceResolver)
        todoSymbolResolver.resolveTree(todoProjectErrands)

        // verify that todo4.prerequisite now points to the external representation of todo2
        assertNotNull(todo4.prerequisite)
        assertTrue(todo4.prerequisite.retrieved)
        assertIs<ExternalTodo>(todo4.prerequisite.referred)
        // the reference points to the external representation of todo2
        assertEquals(indexWriter.write(todo2), todo4.prerequisite.referred)
    }
}
