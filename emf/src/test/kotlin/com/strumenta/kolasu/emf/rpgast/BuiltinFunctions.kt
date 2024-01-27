package com.smeup.rpgparser.parsing.ast

import com.strumenta.kolasu.ast.Range

// %LOOKUP
// To be supported:
// * %LOOKUPLT
// * %LOOKUPLE
// * %LOOKUPGT
// * %LOOKUPGE

data class LookupExpr(
    var searchedValued: Expression,
    val array: Expression,
    val specifiedRange: Range? = null,
) : Expression(specifiedRange)

// %SCAN

data class ScanExpr(
    var value: Expression,
    val src: Expression,
    val start: Expression? = null,
    val specifiedRange: Range? = null,
) : Expression(specifiedRange)

// %XLATE

data class TranslateExpr(
    var from: Expression,
    var to: Expression,
    var string: Expression,
    val startPos: Expression,
    val specifiedRange: Range? = null,
) : Expression(specifiedRange)

// %TRIM

data class TrimExpr(
    var value: Expression,
    val charactersToTrim: Expression? = null,
    val specifiedRange: Range? = null,
) : Expression(specifiedRange) {
    override fun render(): String {
        val toTrim = if (this.charactersToTrim != null) ": ${this.charactersToTrim.render()}" else ""
        return "%TRIM(${this.value.render()} $toTrim)"
    }
}

// %TRIMR

data class TrimrExpr(
    var value: Expression,
    val charactersToTrim: Expression? = null,
    val specifiedRange: Range? = null,
) : Expression(specifiedRange) {
    override fun render(): String {
        val toTrim = if (this.charactersToTrim != null) ": ${this.charactersToTrim.render()}" else ""
        return "%TRIMR(${this.value.render()} $toTrim)"
    }
}

// %TRIML

data class TrimlExpr(
    var value: Expression,
    val charactersToTrim: Expression? = null,
    val specifiedRange: Range? = null,
) : Expression(specifiedRange) {
    override fun render(): String {
        val toTrim = if (this.charactersToTrim != null) ": ${this.charactersToTrim.render()}" else ""
        return "%TRIMR(${this.value.render()} $toTrim)"
    }
}

// %SUBST

data class SubstExpr(
    var string: Expression,
    val start: Expression,
    val length: Expression? = null,
    val specifiedRange: Range? = null,
) : AssignableExpression(specifiedRange) {
    override fun render(): String {
        val len = if (length != null) ": ${length.render()}" else ""
        return "%SUBST(${this.string.render()} : ${start.render()} $len)"
    }

    override fun size(): Int {
        TODO("size")
    }
}

// %LEN

data class LenExpr(
    var value: Expression,
    val specifiedRange: Range? = null,
) : Expression(specifiedRange) {
    override fun render(): String = "%LEN(${this.value.render()})"
}

// %REM

data class RemExpr(
    val dividend: Expression,
    val divisor: Expression,
    val specifiedRange: Range? = null,
) : Expression(specifiedRange)

// %DEC

data class DecExpr(
    var value: Expression,
    var intDigits: Expression,
    val decDigits: Expression,
    val specifiedRange: Range? = null,
) : Expression(specifiedRange) {
    override fun render(): String = "${this.value.render()}"
}

// %INT

data class IntExpr(
    var value: Expression,
    val specifiedRange: Range? = null,
) : Expression(specifiedRange) {
    override fun render(): String = "${this.value.render()}"
}

// %SQRT

data class SqrtExpr(
    var value: Expression,
    val specifiedRange: Range? = null,
) : Expression(specifiedRange) {
    override fun render(): String = "${this.value.render()}"
}

// %EDITC
// TODO add other parameters

data class EditcExpr(
    var value: Expression,
    val format: Expression,
    val specifiedRange: Range? = null,
) : Expression(specifiedRange)

// %EDITW
// TODO add other parameters

data class EditwExpr(
    var value: Expression,
    val format: Expression,
    val specifiedRange: Range? = null,
) : Expression(specifiedRange)

// %FOUND

data class FoundExpr(
    var name: String? = null,
    val specifiedRange: Range? = null,
) : Expression(specifiedRange)

// %EOF

data class EofExpr(
    var name: String? = null,
    val specifiedRange: Range? = null,
) : Expression(specifiedRange)

// %EQUAL

data class EqualExpr(
    var name: String? = null,
    val specifiedRange: Range? = null,
) : Expression(specifiedRange)

// %ABS

data class AbsExpr(
    var value: Expression,
    val specifiedRange: Range? = null,
) : Expression(specifiedRange)

// %CHAR

data class CharExpr(
    var value: Expression,
    val format: String?,
    val specifiedRange: Range? = null,
) : Expression(specifiedRange) {
    override fun render(): String = "%CHAR(${value.render()})"
}

// %TIMESTAMP

data class TimeStampExpr(
    val value: Expression?,
    val specifiedRange: Range? = null,
) : Expression(specifiedRange)

// %DIFF

data class DiffExpr(
    var value1: Expression,
    var value2: Expression,
    val durationCode: DurationCode,
    val specifiedRange: Range? = null,
) : Expression(specifiedRange)

// %REPLACE

data class ReplaceExpr(
    val replacement: Expression,
    val src: Expression,
    val start: Expression? = null,
    val length: Expression? = null,
    val specifiedRange: Range? = null,
) : Expression(specifiedRange)

// TODO Move and handle different types of duration
// TODO document what a duration code is

sealed class DurationCode

object DurationInMSecs : DurationCode()

object DurationInDays : DurationCode()
