package com.strumenta.kolasu.emf.simple

import com.strumenta.kolasu.model.*
import java.math.BigDecimal

abstract class Expression(specifiedPosition: Position? = null) : Node(specifiedPosition) {
    open fun render(): String = this.javaClass.simpleName
}

// /
// / Literals
// /

sealed class NumberLiteral(specifiedPosition: Position? = null) : Expression(specifiedPosition)

data class IntLiteral(val value: Long, val specifiedPosition: Position? = null) : NumberLiteral(
    specifiedPosition
) {
    override fun render() = value.toString()
}

data class RealLiteral(val value: BigDecimal, val specifiedPosition: Position? = null) : NumberLiteral(
    specifiedPosition
) {
    override fun render() = value.toString()
}

data class StringLiteral(val value: String, val specifiedPosition: Position? = null) : Expression(
    specifiedPosition
) {
    override fun render() = "\"$value\""
}

data class DataDefinition(
    override val name: String,
    override val type: Type,
    var fields: List<FieldDefinition> = emptyList(),
    val initializationValue: Expression? = null,
    val inz: Boolean = false,
    val specifiedPosition: Position? = null
) :
    AbstractDataDefinition(name, type, specifiedPosition)

data class FieldDefinition(
    override val name: String,
    override val type: Type,
    val explicitStartOffset: Int? = null,
    val explicitEndOffset: Int? = null,
    val calculatedStartOffset: Int? = null,
    val calculatedEndOffset: Int? = null,
    // In case of using LIKEDS we reuse a FieldDefinition, but specifying a different
    // container. We basically duplicate it
    @property:Link
    var overriddenContainer: DataDefinition? = null,
    val initializationValue: Expression? = null,
    val descend: Boolean = false,
    val specifiedPosition: Position? = null,

    // true when the FieldDefinition contains a DIM keyword on its line
    val declaredArrayInLineOnThisField: Int? = null
) :
    AbstractDataDefinition(name, type, specifiedPosition)

abstract class AbstractDataDefinition(
    override val name: String,
    open val type: Type,
    specifiedPosition: Position? = null,
    private val hashCode: Int = name.hashCode()

) : Node(specifiedPosition), Named

sealed class Type {
    @Derived
    abstract val size: Int
}

object FigurativeType : Type() {
    @Derived
    override val size: Int
        get() = 0
}
