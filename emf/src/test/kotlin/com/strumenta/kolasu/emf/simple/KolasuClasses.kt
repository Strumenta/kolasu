package com.strumenta.kolasu.emf.simple

import com.strumenta.kolasu.model.Derived
import com.strumenta.kolasu.model.Link
import com.strumenta.kolasu.model.Named
import com.strumenta.kolasu.model.Node
import com.strumenta.kolasu.model.Range
import java.math.BigDecimal

abstract class Expression(
    specifiedRange: Range? = null,
) : Node(specifiedRange) {
    open fun render(): String = this.javaClass.simpleName
}

// /
// / Literals
// /

sealed class NumberLiteral(
    specifiedRange: Range? = null,
) : Expression(specifiedRange)

data class IntLiteral(
    val value: Long,
    val specifiedRange: Range? = null,
) : NumberLiteral(
        specifiedRange,
    ) {
    override fun render() = value.toString()
}

data class RealLiteral(
    val value: BigDecimal,
    val specifiedRange: Range? = null,
) : NumberLiteral(
        specifiedRange,
    ) {
    override fun render() = value.toString()
}

data class StringLiteral(
    val value: String,
    val specifiedRange: Range? = null,
) : Expression(
        specifiedRange,
    ) {
    override fun render() = "\"$value\""
}

data class DataDefinition(
    override val name: String,
    override val type: Type,
    var fields: List<FieldDefinition> = emptyList(),
    val initializationValue: Expression? = null,
    val inz: Boolean = false,
    val specifiedRange: Range? = null,
) : AbstractDataDefinition(name, type, specifiedRange)

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
    val specifiedRange: Range? = null,
    // true when the FieldDefinition contains a DIM keyword on its line
    val declaredArrayInLineOnThisField: Int? = null,
) : AbstractDataDefinition(name, type, specifiedRange)

abstract class AbstractDataDefinition(
    override val name: String,
    open val type: Type,
    specifiedRange: Range? = null,
    private val hashCode: Int = name.hashCode(),
) : Node(specifiedRange),
    Named

sealed class Type {
    @Derived
    abstract val size: Int
}

object FigurativeType : Type() {
    @Derived
    override val size: Int
        get() = 0
}
