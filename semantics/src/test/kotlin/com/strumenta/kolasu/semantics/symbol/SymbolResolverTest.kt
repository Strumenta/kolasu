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

    // todo without any symbol reference
    private val firstToDo = Todo(name = "first", description = "the first todo")

    // todo with an internal symbol reference
    private val secondToDo = Todo(
        name = "second",
        description = "the secondo todo",
        prerequisite = ReferenceByName(name = "first")
    )

    // todo with an external symbol reference
    private val thirdToDo = Todo(
        name = "third",
        description = "the third todo",
        prerequisite = ReferenceByName(name = "first")
    )

    // todo with an internal symbol reference
    private val fourthTodo = Todo(
        name = "fourth",
        description = "the fourth todo",
        prerequisite = ReferenceByName(name = "third")
    )

    private val firstProject = TodoProject(name = "firstProject", todos = mutableListOf(firstToDo, secondToDo))
        .apply(Node::assignParents)

    private val secondProject = TodoProject(name = "secondProject", todos = mutableListOf(thirdToDo, fourthTodo))
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
        assertNull(firstToDo.prerequisite)
        // verify second - un-resolved internal reference to first
        assertNotNull(secondToDo.prerequisite)
        assertFalse(secondToDo.prerequisite.resolved)
        assertFalse(secondToDo.prerequisite.retrieved)
        assertNull(secondToDo.prerequisite.referred)
        assertNull(secondToDo.prerequisite.identifier)
        // verify third - un-resolved and un-retrieved external reference to first
        assertNotNull(thirdToDo.prerequisite)
        assertFalse(thirdToDo.prerequisite.resolved)
        assertFalse(thirdToDo.prerequisite.retrieved)
        assertNull(thirdToDo.prerequisite.referred)
        assertNull(thirdToDo.prerequisite.identifier)
        // verify fourth - resolved and retrieved internal reference to second
        assertNotNull(fourthTodo.prerequisite)
        assertTrue(fourthTodo.prerequisite.resolved)
        assertTrue(fourthTodo.prerequisite.retrieved)
        assertEquals(thirdToDo, fourthTodo.prerequisite.referred)
        assertNull(fourthTodo.prerequisite.identifier)
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
        assertNull(firstToDo.prerequisite)
        // verify second - un-resolved internal reference to first
        assertNotNull(secondToDo.prerequisite)
        assertFalse(secondToDo.prerequisite.resolved)
        assertFalse(secondToDo.prerequisite.retrieved)
        assertNull(secondToDo.prerequisite.referred)
        assertNull(secondToDo.prerequisite.identifier)
        // verify third - resolved but un-retrieved external reference to first
        assertNotNull(thirdToDo.prerequisite)
        assertTrue(thirdToDo.prerequisite.resolved)
        assertFalse(thirdToDo.prerequisite.retrieved)
        assertNull(thirdToDo.prerequisite.referred)
        assertEquals(todosNodeIdProvider.id(firstToDo), thirdToDo.prerequisite.identifier)
        // verify fourth - resolved and retrieved internal reference to second
        assertNotNull(fourthTodo.prerequisite)
        assertTrue(fourthTodo.prerequisite.resolved)
        assertTrue(fourthTodo.prerequisite.retrieved)
        assertEquals(thirdToDo, fourthTodo.prerequisite.referred)
        assertNull(fourthTodo.prerequisite.identifier)
    }
}
