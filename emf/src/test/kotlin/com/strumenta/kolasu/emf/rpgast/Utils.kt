package com.strumenta.kolasu.emf.rpgast

import com.strumenta.kolasu.ast.Range
import com.strumenta.kolasu.model.NodeLike

fun Any?.asNonNullString(): String = this?.toString() ?: ""

fun Range?.line() = this?.start?.line.asNonNullString()

fun Range?.atLine() = this?.start?.line?.let { "line $it " } ?: ""

fun NodeLike?.startLine() =
    this
        ?.range
        ?.start
        ?.line
        .asNonNullString()

fun NodeLike?.endLine() =
    this
        ?.range
        ?.end
        ?.line
        .asNonNullString()

enum class ComparisonOperator(
    val symbol: String,
) {
    EQ("="),
    NE("<>"),
    GT(">"),
    GE(">="),
    LT("<"),
    LE("<="),
}

fun String.repeatWithMaxSize(l: Int): String {
    val repetitions = (l / this.length) + 1
    return this.repeat(repetitions).take(l)
}

fun String.asLong(): Long = this.trim().toLong()

fun String.asInt(): Int = this.trim().toInt()

fun String?.isEmptyTrim() = this == null || this.trim().isEmpty()

fun String?.asIntOrNull(): Int? =
    try {
        this?.trim()?.toInt()
    } catch (e: Exception) {
        null
    }
