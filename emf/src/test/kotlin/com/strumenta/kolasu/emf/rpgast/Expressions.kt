package com.smeup.rpgparser.parsing.ast

import com.strumenta.kolasu.emf.rpgast.AbstractDataDefinition
import com.strumenta.kolasu.emf.rpgast.FieldDefinition
import com.strumenta.kolasu.emf.rpgast.atLine
import com.strumenta.kolasu.model.Node
import com.strumenta.kolasu.model.Range
import com.strumenta.kolasu.model.ReferenceValue
import java.math.BigDecimal

abstract class Expression(
    specifiedRange: Range? = null,
) : Node(specifiedRange) {
    open fun render(): String = this.javaClass.simpleName
}

// /
// / Literals
// /

abstract class NumberLiteral(
    specifiedRange: Range? = null,
) : Expression(specifiedRange)

data class IntLiteral(
    val value: Long,
) : NumberLiteral() {
    override fun render() = value.toString()
}

data class RealLiteral(
    val value: BigDecimal,
) : NumberLiteral() {
    override fun render() = value.toString()
}

data class StringLiteral(
    val value: String,
) : Expression() {
    override fun render() = "\"$value\""
}

// /
// / Figurative constants
// /

abstract class FigurativeConstantRef(
    specifiedRange: Range? = null,
) : Expression(specifiedRange)

data class BlanksRefExpr(
    val specifiedRange: Range? = null,
) : FigurativeConstantRef(specifiedRange)

data class OnRefExpr(
    val specifiedRange: Range? = null,
) : FigurativeConstantRef(specifiedRange)

data class OffRefExpr(
    val specifiedRange: Range? = null,
) : FigurativeConstantRef(specifiedRange)

data class HiValExpr(
    val specifiedRange: Range? = null,
) : FigurativeConstantRef(specifiedRange)

data class LowValExpr(
    val specifiedRange: Range? = null,
) : FigurativeConstantRef(specifiedRange)

data class ZeroExpr(
    val specifiedRange: Range? = null,
) : FigurativeConstantRef(specifiedRange)

data class AllExpr(
    val charsToRepeat: StringLiteral,
) : FigurativeConstantRef()

// /
// / Comparisons
// /

data class EqualityExpr(
    var left: Expression,
    var right: Expression,
) : Expression() {
    override fun render() = "${left.render()} = ${right.render()}"
}

data class AssignmentExpr(
    var target: AssignableExpression,
    var value: Expression,
) : Expression() {
    override fun render() = "${target.render()} = ${value.render()}"
}

data class GreaterThanExpr(
    var left: Expression,
    var right: Expression,
) : Expression() {
    override fun render() = "${left.render()} > ${right.render()}"
}

data class GreaterEqualThanExpr(
    var left: Expression,
    var right: Expression,
    val specifiedRange: Range? = null,
) : Expression(specifiedRange) {
    override fun render() = "${left.render()} >= ${right.render()}"
}

data class LessThanExpr(
    var left: Expression,
    var right: Expression,
) : Expression() {
    override fun render() = "${left.render()} < ${right.render()}"
}

data class LessEqualThanExpr(
    var left: Expression,
    var right: Expression,
    val specifiedRange: Range? = null,
) : Expression(specifiedRange) {
    override fun render() = "${left.render()} <= ${right.render()}"
}

data class DifferentThanExpr(
    var left: Expression,
    var right: Expression,
    val specifiedRange: Range? = null,
) : Expression(specifiedRange) {
    override fun render() = "${left.render()} <> ${right.render()}"
}

// /
// / Logical operations
// /

data class NotExpr(
    val base: Expression,
) : Expression()

data class LogicalOrExpr(
    var left: Expression,
    var right: Expression,
    val specifiedRange: Range? = null,
) : Expression(specifiedRange) {
    override fun render() = "${left.render()} || ${right.render()}"
}

data class LogicalAndExpr(
    var left: Expression,
    var right: Expression,
    val specifiedRange: Range? = null,
) : Expression(specifiedRange) {
    override fun render() = "${left.render()} && ${right.render()}"
}

// /
// / Arithmetic operations
// /

data class PlusExpr(
    var left: Expression,
    var right: Expression,
) : Expression() {
    override fun render() = "${left.render()} + ${right.render()}"
}

data class MinusExpr(
    var left: Expression,
    var right: Expression,
) : Expression() {
    override fun render() = "${left.render()} - ${right.render()}"
}

data class MultExpr(
    var left: Expression,
    var right: Expression,
) : Expression() {
    override fun render() = "${left.render()} * ${right.render()}"
}

data class DivExpr(
    var left: Expression,
    var right: Expression,
) : Expression() {
    override fun render() = "${left.render()} / ${right.render()}"
}

data class ExpExpr(
    var left: Expression,
    var right: Expression,
) : Expression() {
    override fun render() = "${left.render()} ** ${right.render()}"
}

// /
// / Misc
// /

abstract class AssignableExpression(
    specifiedRange: Range? = null,
) : Expression(specifiedRange) {
    abstract fun size(): Int
}

data class DataRefExpr(
    val variable: ReferenceValue<AbstractDataDefinition>,
    val specifiedRange: Range? = null,
) : AssignableExpression(specifiedRange) {
    init {
        require(!variable.name.startsWith("*")) {
            "This is not a valid variable name: '${variable.name}' - ${specifiedRange.atLine()}"
        }
        require(variable.name.isNotBlank()) {
            "The variable name should not blank - ${specifiedRange.atLine()}"
        }
        require(variable.name.trim() == variable.name) {
            "The variable name should not starts or ends with whitespace: " +
                "$variable.name - ${specifiedRange.atLine()}"
        }
        require(!variable.name.contains(".")) {
            "The variable name should not contain any dot: <${variable.name}> - ${specifiedRange.atLine()}"
        }
        require(!variable.name.contains("(") && !variable.name.contains(")")) {
            "The variable name should not contain any parenthesis: $variable.name - ${specifiedRange.atLine()}"
        }
    }

    override fun size(): Int = variable.referred!!.type.size

    override fun render() = variable.name
}

data class QualifiedAccessExpr(
    val container: Expression,
    val field: ReferenceValue<FieldDefinition>,
    val specifiedRange: Range? = null,
) : AssignableExpression(specifiedRange) {
    init {
        require(field.name.isNotBlank()) { "The field name should not blank" }
        require(field.name.trim() == field.name) {
            "The field name should not starts or ends with whitespace"
        }
    }

    override fun size(): Int {
        TODO()
    }

    override fun render() = "${container.render()}.${field.name}"
}

data class ArrayAccessExpr(
    val array: Expression,
    val index: Expression,
    val specifiedRange: Range? = null,
) : AssignableExpression(specifiedRange) {
    override fun render(): String = "${this.array.render()}(${index.render()}))"

    override fun size(): Int {
        TODO("size")
    }
}

// A Function call is not distinguishable from an array access
// TODO replace them in the AST during the resolution phase

data class FunctionCall(
    val function: ReferenceValue<Function>,
    val args: List<Expression>,
    val specifiedRange: Range? = null,
) : Expression(specifiedRange)

fun dataRefTo(dataDefinition: AbstractDataDefinition) = DataRefExpr(ReferenceValue(dataDefinition.name, dataDefinition))

data class NumberOfElementsExpr(
    val value: Expression,
) : Expression() {
    override fun render() = "%ELEM(${value.render()})"
}
