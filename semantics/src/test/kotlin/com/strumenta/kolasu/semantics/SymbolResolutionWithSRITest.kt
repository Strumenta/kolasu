package com.strumenta.kolasu.semantics

import com.strumenta.kolasu.ids.StructuralNodeIdProvider
import com.strumenta.kolasu.model.Node
import com.strumenta.kolasu.model.Point
import com.strumenta.kolasu.model.Position
import com.strumenta.kolasu.model.ReferenceByName
import com.strumenta.kolasu.model.SimpleOrigin
import com.strumenta.kolasu.model.SyntheticSource
import com.strumenta.kolasu.model.assignParents
import com.strumenta.kolasu.semantics.common.Todo
import com.strumenta.kolasu.semantics.common.TodoProject
import com.strumenta.kolasu.semantics.scope.provider.scopeProvider
import com.strumenta.kolasu.semantics.symbol.description.SymbolDescription
import com.strumenta.kolasu.semantics.symbol.importer.SymbolImporter
import com.strumenta.kolasu.semantics.symbol.provider.SymbolProvider
import com.strumenta.kolasu.semantics.symbol.provider.symbolProvider
import com.strumenta.kolasu.semantics.symbol.repository.InMemorySymbolRepository
import com.strumenta.kolasu.semantics.symbol.repository.SymbolRepository
import com.strumenta.kolasu.traversing.walk
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import com.strumenta.kolasu.semantics.symbol.resolver.SymbolResolver as SR

class SymbolResolutionWithSRITest {

    @Test
    fun symbolResolutionPointingToNodes() {
        val todo1 = Todo("todo1", "stuff to do 1")
        val todo2 = Todo("todo2", "stuff to do 2", prerequisite = ReferenceByName("todo1"))
        val todo3 = Todo("todo3", "stuff to do 3")
        val todoProject = TodoProject("Personal", mutableListOf(todo1, todo2, todo3))
        todoProject.assignParents()

        val scopeProvider = scopeProvider {
            rule(TodoProject::class) { (node) ->
                node.todos.forEach(this::include)
            }
        }

        val symbolResolver = SR(scopeProvider)

        assertNotNull(todo2.prerequisite)
        assertFalse(todo2.prerequisite.resolved)

        symbolResolver.resolveTree(todoProject)

        assertTrue(todo2.prerequisite.resolved)
        assertEquals(todo1, todo2.prerequisite.referred)
    }

    @Test
    fun symbolResolutionPointingToNodesWithCustomIdProvider() {
        val todo1 = Todo("todo1", "stuff to do 1")
        val todo2 = Todo("todo2", "stuff to do 2", prerequisite = ReferenceByName("todo1"))
        val todo3 = Todo("todo3", "stuff to do 3")
        val todoProject = TodoProject("Personal", mutableListOf(todo1, todo2, todo3))
        todoProject.assignParents()

        val scopeProvider = scopeProvider {
            rule(TodoProject::class) { (node) ->
                node.todos.forEach(this::include)
            }
        }
        val symbolResolver = SR(scopeProvider)

        assertNotNull(todo2.prerequisite)
        assertFalse(todo2.prerequisite.resolved)

        symbolResolver.resolveTree(todoProject)

        assertTrue(todo2.prerequisite.resolved)
        assertEquals(todo1, todo2.prerequisite.referred)
    }

    @Test
    fun symbolResolutionPointingToSymbols() {
        val todo1 = Todo("todo1", "stuff to do 1")
        val todo2 = Todo("todo2", "stuff to do 2")
        val todo3 = Todo("todo3", "stuff to do 3")
        val todoProjectPersonal = TodoProject("Personal", mutableListOf(todo1, todo2, todo3))
        val dummyPoint = Point(1, 0)
        val source1 = SyntheticSource("Personal-Source")
        todoProjectPersonal.assignParents()
        todoProjectPersonal.walk().forEach {
            it.origin = SimpleOrigin(Position(dummyPoint, dummyPoint, source1))
            assertEquals(source1, it.source)
        }

        val todo4 = Todo("todo4", "Some stuff to do", ReferenceByName("todo2"))
        val todoProjectErrands = TodoProject("Errands", mutableListOf(todo4))
        val source2 = SyntheticSource("Errands-Source")
        todoProjectErrands.assignParents()
        todoProjectErrands.walk().forEach {
            it.origin = SimpleOrigin(Position(dummyPoint, dummyPoint, source2))
            assertEquals(source2, it.source)
        }

        val nodeIdProvider = StructuralNodeIdProvider()

        val symbolProvider = symbolProvider(nodeIdProvider) {
            rule(Todo::class) { (node) ->
                include("name", node.name)
            }
        }

        val symbolRepository = InMemorySymbolRepository()

        val symbolImporter = SymbolImporter(symbolProvider, symbolRepository)
        symbolImporter.importTree(todoProjectPersonal)
        symbolImporter.importTree(todoProjectErrands)

        val scopeProvider = scopeProvider {
            rule(TodoProject::class) { (node) ->
                node.todos.forEach(this::include)
                parent {
                    symbolRepository.findAll(Todo::class).forEach(this::include)
                }
            }
        }

        // We can now resolve _only_ the nodes in the current AST, so we do not specify other ASTs
        val symbolResolver = SR(scopeProvider)

        assertNotNull(todo4.prerequisite)
        assertFalse(todo4.prerequisite.resolved)

        symbolResolver.resolveTree(todoProjectErrands)

        assertTrue(todo4.prerequisite.resolved)
        assertEquals("synthetic_Personal-Source_root_todos_1", todo4.prerequisite.identifier)
    }
}

class ASTsSymbolRepository(
    private val symbolProvider: SymbolProvider,
    private vararg val roots: Node
) : SymbolRepository {

    override fun store(symbol: SymbolDescription) {
        TODO("Not yet implemented")
    }

    override fun load(identifier: String): SymbolDescription? {
        roots.forEach { root ->
            root.walk().forEach { node ->
                val symbol = symbolProvider.from(node)
                if (symbol != null && symbol.identifier == identifier) {
                    return symbol
                }
            }
        }
        return null
    }

    override fun delete(identifier: String): Boolean {
        TODO("Not yet implemented")
    }

    override fun loadAll(filter: (SymbolDescription) -> Boolean): Sequence<SymbolDescription> {
        return sequence {
            roots.forEach { root ->
                root.walk().forEach { node ->
                    symbolProvider.from(node)
                        ?.takeIf(filter)
                        ?.let { yield(it) }
                }
            }
        }
    }

    override fun clear() {
        TODO("Not yet implemented")
    }
}
