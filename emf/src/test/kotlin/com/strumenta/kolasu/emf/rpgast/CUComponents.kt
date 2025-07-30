package com.smeup.rpgparser.parsing.ast

import com.strumenta.kolasu.emf.rpgast.AbstractDataDefinition
import com.strumenta.kolasu.emf.rpgast.DataDefinition
import com.strumenta.kolasu.emf.rpgast.FileDefinition
import com.strumenta.kolasu.emf.rpgast.InStatementDataDefinition
import com.strumenta.kolasu.model.Derived
import com.strumenta.kolasu.model.Named
import com.strumenta.kolasu.model.Node
import com.strumenta.kolasu.model.Position

// This file contains the AST nodes at the highest level:
// from the CompilationUnit (which represents the whole file)
// to its main components

data class CompilationUnit(
    val fileDefinitions: List<FileDefinition>,
    val dataDefinitions: List<DataDefinition>,
    val main: MainBody,
    val subroutines: List<Subroutine>,
    val compileTimeArrays: List<CompileTimeArray>,
    val directives: List<Directive>,
    val specifiedPosition: Position??
) : Node(specifiedPosition) {

    companion object {
        fun empty() = CompilationUnit(
            emptyList(),
            emptyList(),
            MainBody(emptyList(), null),
            emptyList(),
            emptyList(),
            emptyList(),
            null
        )
    }

    val entryPlist: PlistStmt?
        get() = main.stmts.plist()
            ?: subroutines.mapNotNull { it.stmts.plist() }.firstOrNull()

    private val inStatementsDataDefinitions = mutableListOf<InStatementDataDefinition>()

    fun addInStatementDataDefinitions(dataDefinitions: List<InStatementDataDefinition>) {
        inStatementsDataDefinitions.addAll(dataDefinitions)
    }

    private var _allDataDefinitions = mutableListOf<AbstractDataDefinition>()

    @Derived
    val allDataDefinitions: List<AbstractDataDefinition>
        get() {
            if (_allDataDefinitions.isEmpty()) {
                _allDataDefinitions.addAll(dataDefinitions)
                // Adds DS sub-fields
                dataDefinitions.forEach { it.fields.let { _allDataDefinitions.addAll(it) } }
                _allDataDefinitions.addAll(inStatementsDataDefinitions)
                _allDataDefinitions = checkDuplicatedDataDefinition(_allDataDefinitions).toMutableList()
            }
            return _allDataDefinitions
        }

    private fun checkDuplicatedDataDefinition(
        dataDefinitions: List<AbstractDataDefinition>
    ): List<AbstractDataDefinition> {
        val dataDefinitionMap = mutableMapOf<String, AbstractDataDefinition>()
        return dataDefinitions.filter {
            val dataDefinition = dataDefinitionMap[it.name]
            if (dataDefinition == null) {
                dataDefinitionMap[it.name] = it
                true
            } else {
                require(dataDefinition.type == it.type) {
                    "Incongruous definitions of ${it.name}: ${dataDefinition.type} vs ${it.type}"
                }
                false
            }
        }
    }

    fun hasDataDefinition(name: String) = dataDefinitions.any { it.name.equals(name, ignoreCase = true) }

    fun getDataDefinition(name: String) = dataDefinitions.firstOrNull { it.name.equals(name, ignoreCase = true) }
        ?: throw IllegalArgumentException("Data definition $name was not found")

    fun getDataOrFieldDefinition(name: String) = dataDefinitions.firstOrNull {
        it.name.equals(name, ignoreCase = true)
    }
        ?: dataDefinitions.mapNotNull { it.fields.find { it.name.equals(name, ignoreCase = true) } }.firstOrNull()
        ?: throw IllegalArgumentException("Data or field definition $name was not found")

    fun hasAnyDataDefinition(name: String) = allDataDefinitions.any { it.name.equals(name, ignoreCase = true) }

    fun getAnyDataDefinition(name: String) = allDataDefinitions.first { it.name.equals(name, ignoreCase = true) }

    fun compileTimeArray(name: String): CompileTimeArray {
        fun firstCompileTimeArray() = if (compileTimeArrays.isNotEmpty()) {
            compileTimeArrays[0]
        } else {
            CompileTimeArray("", emptyList())
        }
        return compileTimeArrays.firstOrNull { it.name.equals(name, ignoreCase = true) } ?: firstCompileTimeArray()
    }

    fun hasFileDefinition(name: String) = fileDefinitions.any { it.name.equals(name, ignoreCase = true) }

    fun getFileDefinition(name: String) = fileDefinitions.first { it.name.equals(name, ignoreCase = true) }
}

data class MainBody(val stmts: List<Statement>, val specifiedPosition: Position? = null) : Node(
    specifiedPosition
)

data class Subroutine(
    override val name: String,
    val stmts: List<Statement>,
    val tag: String? = null,
    val specifiedPosition: Position? = null
) : Named, Node(specifiedPosition)

data class Function(override val name: String, val specifiedPosition: Position? = null) : Named, Node(
    specifiedPosition
)

data class CompileTimeArray(
    override val name: String,
    val lines: List<String>,
    val specifiedPosition: Position? = null
) : Named, Node(specifiedPosition)

enum class DataWrapUpChoice {
    LR,
    RT
}

// A PList is a list of parameters
fun List<Statement>.plist(): PlistStmt? = this.asSequence().mapNotNull { it as? PlistStmt }.firstOrNull { it.isEntry }
