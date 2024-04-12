package com.strumenta.kolasu.semantics.symbol

import com.strumenta.kolasu.model.Node
import com.strumenta.kolasu.model.ReferenceByName
import com.strumenta.kolasu.model.assignParents
import com.strumenta.kolasu.semantics.common.Todo
import com.strumenta.kolasu.semantics.common.TodoProject
import com.strumenta.kolasu.semantics.common.todosNodeIdProvider
import com.strumenta.kolasu.semantics.common.todosScopeProvider
import com.strumenta.kolasu.semantics.common.todosSymbolProvider
import com.strumenta.kolasu.semantics.symbol.importer.SymbolImporter
import com.strumenta.kolasu.semantics.symbol.repository.InMemorySymbolRepository
import com.strumenta.kolasu.semantics.symbol.resolver.SymbolResolver
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SymbolResolverTest {

    private val todoWithoutSymbolReferences = Todo(name = "first", description = "the first todo")

    private val todoWithLocalSymbolReference = Todo(
        name = "second",
        description = "the secondo todo",
        prerequisite = ReferenceByName(name = "first")
    )

    private val todoWithExternalSymbolReference = Todo(
        name = "third",
        description = "the third todo",
        prerequisite = ReferenceByName(name = "first")
    )

    private val anotherTodoWithInternalSymbolReference = Todo(
        name = "fourth",
        description = "the fourth todo",
        prerequisite = ReferenceByName(name = "third")
    )

    private val firstProject = TodoProject(
        name = "firstProject",
        todos = mutableListOf(todoWithoutSymbolReferences, todoWithLocalSymbolReference)
    )
        .apply(Node::assignParents)

    private val secondProject = TodoProject(
        name = "secondProject",
        todos = mutableListOf(todoWithExternalSymbolReference, anotherTodoWithInternalSymbolReference)
    )
        .apply(Node::assignParents)

    @Test
    fun testLocalSymbolResolution() {
        // create scope provider instance
        val scopeProvider = todosScopeProvider()
        // create symbol resolver instance
        val symbolResolver = SymbolResolver(scopeProvider)
        // resolve all references in the second project only
        symbolResolver.resolveTree(secondProject)
        // verify first - no prerequisites
        assertNull(todoWithoutSymbolReferences.prerequisite)
        // verify second - un-resolved internal reference to first
        assertNotNull(todoWithLocalSymbolReference.prerequisite)
        assertFalse(todoWithLocalSymbolReference.prerequisite.resolved)
        assertFalse(todoWithLocalSymbolReference.prerequisite.retrieved)
        assertNull(todoWithLocalSymbolReference.prerequisite.referred)
        assertNull(todoWithLocalSymbolReference.prerequisite.identifier)
        // verify third - un-resolved and un-retrieved external reference to first
        assertNotNull(todoWithExternalSymbolReference.prerequisite)
        assertFalse(todoWithExternalSymbolReference.prerequisite.resolved)
        assertFalse(todoWithExternalSymbolReference.prerequisite.retrieved)
        assertNull(todoWithExternalSymbolReference.prerequisite.referred)
        assertNull(todoWithExternalSymbolReference.prerequisite.identifier)
        // verify fourth - resolved and retrieved internal reference to second
        assertNotNull(anotherTodoWithInternalSymbolReference.prerequisite)
        assertTrue(anotherTodoWithInternalSymbolReference.prerequisite.resolved)
        assertTrue(anotherTodoWithInternalSymbolReference.prerequisite.retrieved)
        assertEquals(todoWithExternalSymbolReference, anotherTodoWithInternalSymbolReference.prerequisite.referred)
        assertNull(anotherTodoWithInternalSymbolReference.prerequisite.identifier)
    }

    @Test
    fun testGlobalSymbolResolution() {
        // create symbol repository instance
        val symbolRepository = InMemorySymbolRepository()
        // import data into symbol repository
        val symbolImporter = SymbolImporter(todosSymbolProvider, symbolRepository).apply {
            // import all nodes from the first project
            importTree(firstProject)
            // no need to import nodes from the second project
        }
        symbolImporter.importTree(firstProject)
        symbolImporter.importTree(secondProject)
        // create scope provider instance (with symbol repository)
        val scopeProvider = todosScopeProvider(symbolRepository)
        // create symbol resolver instance
        val symbolResolver = SymbolResolver(scopeProvider)
        // resolve all references in the second project only
        symbolResolver.resolveTree(secondProject)
        // verify first - no prerequisites
        assertNull(todoWithoutSymbolReferences.prerequisite)
        // verify second - un-resolved internal reference to first
        assertNotNull(todoWithLocalSymbolReference.prerequisite)
        assertFalse(todoWithLocalSymbolReference.prerequisite.resolved)
        assertFalse(todoWithLocalSymbolReference.prerequisite.retrieved)
        assertNull(todoWithLocalSymbolReference.prerequisite.referred)
        assertNull(todoWithLocalSymbolReference.prerequisite.identifier)
        // verify third - resolved but un-retrieved external reference to first
        assertNotNull(todoWithExternalSymbolReference.prerequisite)
        assertTrue(todoWithExternalSymbolReference.prerequisite.resolved)
        assertFalse(todoWithExternalSymbolReference.prerequisite.retrieved)
        assertNull(todoWithExternalSymbolReference.prerequisite.referred)
        assertEquals(
            todosNodeIdProvider.id(todoWithoutSymbolReferences),
            todoWithExternalSymbolReference.prerequisite.identifier
        )
        // verify fourth - resolved and retrieved internal reference to second
        assertNotNull(anotherTodoWithInternalSymbolReference.prerequisite)
        assertTrue(anotherTodoWithInternalSymbolReference.prerequisite.resolved)
        assertTrue(anotherTodoWithInternalSymbolReference.prerequisite.retrieved)
        assertEquals(todoWithExternalSymbolReference, anotherTodoWithInternalSymbolReference.prerequisite.referred)
        assertNull(anotherTodoWithInternalSymbolReference.prerequisite.identifier)
    }
}
