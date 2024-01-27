package com.smeup.rpgparser.parsing.ast

import com.strumenta.kolasu.ast.Range
import com.strumenta.kolasu.emf.rpgast.isInt

// *IN01..*IN99 and *INLR *INRT

data class IndicatorExpr(
    val index: IndicatorKey,
    val specifiedRange: Range? = null,
) : AssignableExpression(specifiedRange) {
    constructor(dataWrapUpChoice: DataWrapUpChoice, specifiedRange: Range? = null) :
        this(index = dataWrapUpChoice.name.toIndicatorKey(), specifiedRange = specifiedRange)

    override fun size(): Int = 1
}

// *IN

data class GlobalIndicatorExpr(
    val specifiedRange: Range? = null,
) : AssignableExpression(specifiedRange) {
    override fun size(): Int {
        TODO("not implemented") // To change body of created functions use File | Settings | File Templates.
    }
}

typealias IndicatorKey = Int

/**
 * Managed indicator types
 * */
enum class IndicatorType(
    val range: IntRange,
) {
    Predefined(1..99),
    LR(100..100),
    RT(101..101),
    OC(102..102),
    OF(103..103),
    OV(104..104),
    ;

    companion object {
        val STATELESS_INDICATORS: List<IndicatorKey> by lazy {
            arrayListOf<IndicatorKey>().apply {
                values()
                    .filter { indicatorType ->
                        indicatorType.range.last > 99
                    }.map { indicatorType ->
                        indicatorType.range.forEach {
                            add(it)
                        }
                    }
            }
        }
    }
}

/**
 * Convert a string in format [0-9] [0-9] or [a-zA-Z] [a-zA-Z] to IndicatorKey
 * */
fun String.toIndicatorKey(): IndicatorKey =
    when {
        this.isInt() ->
            this.let {
                require(IndicatorType.Predefined.range.contains(it.toInt()))
                it.toInt()
            }

        else -> IndicatorType.valueOf(this).range.first
    }

data class IndicatorCondition(
    val key: IndicatorKey,
    val negate: Boolean,
)

data class ContinuedIndicator(
    val key: IndicatorKey,
    val negate: Boolean,
    val level: String,
)
