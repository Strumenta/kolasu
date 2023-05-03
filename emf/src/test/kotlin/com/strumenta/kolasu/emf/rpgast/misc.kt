package com.strumenta.kolasu.emf.rpgast

import com.strumenta.kolasu.model.*

data class ToAstConfiguration(
    val considerRange: Boolean = true
)

fun List<ASTNode>.range(): Range? {
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
