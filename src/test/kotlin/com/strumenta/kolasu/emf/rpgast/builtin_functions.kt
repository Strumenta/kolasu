package com.smeup.rpgparser.parsing.ast

import com.strumenta.kolasu.model.Position

// %LOOKUP
// To be supported:
// * %LOOKUPLT
// * %LOOKUPLE
// * %LOOKUPGT
// * %LOOKUPGE

data class LookupExpr(
    var searchedValued: Expression,
    val array: Expression,
    override val specifiedPosition: Position? = null
) : Expression(specifiedPosition)

// %SCAN

data class ScanExpr(
    var value: Expression,
    val source: Expression,
    val start: Expression? = null,
    override val specifiedPosition: Position? = null
) : Expression(specifiedPosition) {
    
}

// %XLATE

data class TranslateExpr(
    var from: Expression,
    var to: Expression,
    var string: Expression,
    val startPos: Expression,
    override val specifiedPosition: Position? = null
) : Expression(specifiedPosition) {
    
}

// %TRIM

data class TrimExpr(
    var value: Expression,
    val charactersToTrim: Expression? = null,
    override val specifiedPosition: Position? = null
) : Expression(specifiedPosition) {
    override fun render(): String {
        val toTrim = if (this.charactersToTrim != null) ": ${this.charactersToTrim.render()}" else ""
        return "%TRIM(${this.value.render()} $toTrim)"
    }
    
}

// %TRIMR

data class TrimrExpr(
    var value: Expression,
    val charactersToTrim: Expression? = null,
    override val specifiedPosition: Position? = null
) : Expression(specifiedPosition) {

    override fun render(): String {
        val toTrim = if (this.charactersToTrim != null) ": ${this.charactersToTrim.render()}" else ""
        return "%TRIMR(${this.value.render()} $toTrim)"
    }
    
}

// %TRIML

data class TrimlExpr(
    var value: Expression,
    val charactersToTrim: Expression? = null,
    override val specifiedPosition: Position? = null
) : Expression(specifiedPosition) {

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
    override val specifiedPosition: Position? = null
) :
        AssignableExpression(specifiedPosition) {
    override fun render(): String {
        val len = if (length != null) ": ${length.render()}" else ""
        return "%SUBST(${this.string.render()} : ${start.render()} $len)"
    }
    override fun size(): Int {
        TODO("size")
    }
    
}

// %LEN

data class LenExpr(var value: Expression, override val specifiedPosition: Position? = null) :
    Expression(specifiedPosition) {
    override fun render(): String {
        return "%LEN(${this.value.render()})"
    }
    
}

// %REM

data class RemExpr(
    val dividend: Expression,
    val divisor: Expression,
    override val specifiedPosition: Position? = null
) : Expression(specifiedPosition) {
    
}

// %DEC

data class DecExpr(
    var value: Expression,
    var intDigits: Expression,
    val decDigits: Expression,
    override val specifiedPosition: Position? = null
) : Expression(specifiedPosition) {
    override fun render(): String {
        return "${this.value.render()}"
    }
    
}

// %INT

data class IntExpr(
    var value: Expression,
    override val specifiedPosition: Position? = null
) :
    Expression(specifiedPosition) {
    override fun render(): String {
        return "${this.value.render()}"
    }
    
}

// %SQRT

data class SqrtExpr(var value: Expression, override val specifiedPosition: Position? = null) :
        Expression(specifiedPosition) {
    override fun render(): String {
        return "${this.value.render()}"
    }
    
}

// %EDITC
// TODO add other parameters

data class EditcExpr(
    var value: Expression,
    val format: Expression,
    override val specifiedPosition: Position? = null
) : Expression(specifiedPosition) {
    
}

// %EDITW
// TODO add other parameters

data class EditwExpr(
    var value: Expression,
    val format: Expression,
    override val specifiedPosition: Position? = null
) : Expression(specifiedPosition) {
    
}

// %FOUND

data class FoundExpr(
    var name: String? = null,
    override val specifiedPosition: Position? = null
) : Expression(specifiedPosition) {
    
}

// %EOF

data class EofExpr(
    var name: String? = null,
    override val specifiedPosition: Position? = null
) : Expression(specifiedPosition) {
    
}

// %EQUAL

data class EqualExpr(
    var name: String? = null,
    override val specifiedPosition: Position? = null
) : Expression(specifiedPosition) {
    
}

// %ABS

data class AbsExpr(
    var value: Expression,
    override val specifiedPosition: Position? = null
) : Expression(specifiedPosition) {
    
}

// %CHAR

data class CharExpr(var value: Expression, val format: String?, override val specifiedPosition: Position? = null) :
    Expression(specifiedPosition) {
    override fun render(): String {
        return "%CHAR(${value.render()})"
    }
    
}

// %TIMESTAMP

data class TimeStampExpr(val value: Expression?, override val specifiedPosition: Position? = null) :
    Expression(specifiedPosition) {
    
}

// %DIFF

data class DiffExpr(
    var value1: Expression,
    var value2: Expression,
    val durationCode: DurationCode,
    override val specifiedPosition: Position? = null
) :
    Expression(specifiedPosition) {
    
}

// %REPLACE

data class ReplaceExpr(
    val replacement: Expression,
    val source: Expression,
    val start: Expression? = null,
    val length: Expression? = null,
    override val specifiedPosition: Position? = null
) :
    Expression(specifiedPosition) {
    
}

// TODO Move and handle different types of duration
// TODO document what a duration code is

sealed class DurationCode

object DurationInMSecs : DurationCode()

object DurationInDays : DurationCode()
