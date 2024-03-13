package com.strumenta.kolasu.semantics

import com.strumenta.kolasu.ids.StructuralNodeIdProvider
import com.strumenta.kolasu.model.*
import com.strumenta.kolasu.semantics.scope.provider.ReferenceNode
import com.strumenta.kolasu.semantics.scope.provider.scopeProvider
import com.strumenta.kolasu.semantics.symbol.description.SymbolDescription
import com.strumenta.kolasu.semantics.symbol.provider.SymbolProvider
import com.strumenta.kolasu.semantics.symbol.provider.symbolProvider
import com.strumenta.kolasu.semantics.symbol.repository.SymbolRepository
import com.strumenta.kolasu.traversing.findAncestorOfType
import com.strumenta.kolasu.traversing.walk
import com.strumenta.kolasu.traversing.walkDescendants
import kotlin.reflect.KClass
import kotlin.test.Test
import kotlin.test.assertEquals
import com.strumenta.kolasu.semantics.symbol.resolver.SymbolResolver as SR

class TodoProject(override var name: String, val todos: MutableList<Todo>) : Node(), Named

class Todo(
    override var name: String,
    var description: String,
    val prerequisite: ReferenceNode<Todo>? = null
) : Node(), Named

class SymbolResolutionWithSRITest {

    @Test
    fun symbolResolutionPointingToNodes() {
        val todo1 = Todo("todo1", "stuff to do 1")
        val todo2 = Todo("todo2", "stuff to do 2", prerequisite = ReferenceNode("todo1", Todo::class))
        val todo3 = Todo("todo3", "stuff to do 3")
        val todoProject = TodoProject("Personal", mutableListOf(todo1, todo2, todo3))
        todoProject.assignParents()

        val scopeProvider = scopeProvider {
            scopeFor(Todo::class) {
                it.node.findAncestorOfType(TodoProject::class.java)?.todos?.forEach(this::defineLocalSymbol)
            }
        }

        val symbolResolver = SR(scopeProvider)

        assertEquals(false, todo2.prerequisite!!.reference.resolved)
        symbolResolver.resolveTree(todoProject)
        assertEquals(true, todo2.prerequisite!!.reference.resolved)
        assertEquals(todo1, todo2.prerequisite!!.reference.referred)
    }

    @Test
    fun symbolResolutionPointingToNodesWithCustomIdProvider() {
        val todo1 = Todo("todo1", "stuff to do 1")
        val todo2 = Todo("todo2", "stuff to do 2", prerequisite = ReferenceNode("todo1", Todo::class))
        val todo3 = Todo("todo3", "stuff to do 3")
        val todoProject = TodoProject("Personal", mutableListOf(todo1, todo2, todo3))
        todoProject.assignParents()

        val nodeIdProvider = StructuralNodeIdProvider("foo")

        val scopeProvider = scopeProvider {
            scopeFor(Todo::class) {
                it.node.findAncestorOfType(TodoProject::class.java)?.todos?.forEach(this::defineLocalSymbol)
            }
        }
        val symbolResolver = SR(scopeProvider)

        assertEquals(false, todo2.prerequisite!!.reference.resolved)
        symbolResolver.resolveTree(todoProject)
        assertEquals(true, todo2.prerequisite!!.reference.resolved)
        assertEquals(todo1, todo2.prerequisite!!.reference.referred)
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

        val todo4 = Todo("todo4", "Some stuff to do", ReferenceNode("todo2", type = Todo::class))
        val todoProjectErrands = TodoProject("Errands", mutableListOf(todo4))
        val source2 = SyntheticSource("Errands-Source")
        todoProjectErrands.assignParents()
        todoProjectErrands.walk().forEach {
            it.origin = SimpleOrigin(Position(dummyPoint, dummyPoint, source2))
            assertEquals(source2, it.source)
        }

        val nodeIdProvider = StructuralNodeIdProvider()

        val symbolProvider = symbolProvider(nodeIdProvider) {
            symbolFor(Todo::class) {
                include("name", it.node.name)
            }
        }

        val sri: SymbolRepository = ASTsSymbolRepository(
            symbolProvider,
            todoProjectPersonal,
            todoProjectErrands
        )
        val scopeProvider = scopeProvider {
            scopeFor(Todo::class) {
                it.node.findAncestorOfType(TodoProject::class.java)?.todos?.forEach(this::defineLocalSymbol)
                sri.find(Todo::class).forEach(this::defineExternalSymbol)
            }
        }

        // We can now resolve _only_ the nodes in the current AST, so we do not specify other ASTs
        val symbolResolver = SR(scopeProvider)

        assertEquals(false, todo4.prerequisite!!.reference.resolved)
        symbolResolver.resolveTree(todoProjectErrands)
        assertEquals(true, todo4.prerequisite!!.reference.resolved)
        assertEquals("synthetic_Personal-Source_root_todos_1", todo4.prerequisite.reference.identifier)
    }
}

class ASTsSymbolRepository(
    val symbolProvider: SymbolProvider,
    vararg val roots: Node
) : SymbolRepository {
    override fun load(identifier: String): SymbolDescription? {
        roots.forEach { root ->
            root.walk().forEach { node ->
                val symbol = symbolProvider.symbolFor(node)
                if (symbol != null && symbol.identifier == identifier) {
                    return symbol
                }
            }
        }
        return null
    }

    override fun store(symbol: SymbolDescription) {
        TODO("Not yet implemented")
    }

    override fun find(withType: KClass<out Node>): Sequence<SymbolDescription> {
        return sequence {
            roots.forEach { root ->
                root.walkDescendants(withType).forEach { node ->
                    yield(symbolProvider.symbolFor(node)!!)
                }
            }
        }
    }
}
