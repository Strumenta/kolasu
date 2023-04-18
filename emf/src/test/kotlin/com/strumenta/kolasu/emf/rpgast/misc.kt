package com.strumenta.kolasu.emf.rpgast

import com.smeup.rpgparser.parsing.*
import com.smeup.rpgparser.parsing.ast.*
import com.smeup.rpgparser.parsing.ast.AssignmentOperator.*
import com.strumenta.kolasu.model.*
import java.util.*

data class ToAstConfiguration(
    val considerPosition: Boolean = true
)

fun List<Node>.position(): Range? {
    val start = this.asSequence().map { it.range?.start }.filterNotNull().sorted().toList()
    val end = this.asSequence().map { it.range?.end }.filterNotNull().sorted().toList()
    return if (start.isEmpty() || end.isEmpty()) {
        null
    } else {
        Range(start.first(), end.last())
    }
}

internal interface DataDefinitionProvider {
    fun isReady(): Boolean
    fun toDataDefinition(): DataDefinition
}
private data class DataDefinitionHolder(val dataDefinition: DataDefinition) : DataDefinitionProvider {
    override fun isReady() = true
    override fun toDataDefinition() = dataDefinition
}
private data class DataDefinitionCalculator(val calculator: () -> DataDefinition) : DataDefinitionProvider {
    override fun isReady() = false
    override fun toDataDefinition() = calculator()
}

internal fun String.isInt() = this.toIntOrNull() != null
