package com.smeup.rpgparser.parsing.ast

import com.strumenta.kolasu.emf.rpgast.isInt
import com.strumenta.kolasu.model.Position

// *IN01..*IN99 and *INLR *INRT

data class IndicatorExpr(val index: IndicatorKey, override var specifiedPosition: Position? = null) :
    AssignableExpression(specifiedPosition) {

    constructor(dataWrapUpChoice: DataWrapUpChoice, specifiedPosition: Position? = null) :
        this(index = dataWrapUpChoice.name.toIndicatorKey(), specifiedPosition = specifiedPosition)

    override fun size(): Int = 1
}

// *IN

data class GlobalIndicatorExpr(override var specifiedPosition: Position? = null) :
    AssignableExpression(specifiedPosition) {
    override fun size(): Int {
        TODO("not implemented") // To change body of created functions use File | Settings | File Templates.
    }
}

typealias IndicatorKey = Int

/**
 * Managed indicator types
 * */
enum class IndicatorType(val range: IntRange) {
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
                values().filter { indicatorType ->
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
fun String.toIndicatorKey(): IndicatorKey {
    return when {
        this.isInt() -> this.let {
            require(IndicatorType.Predefined.range.contains(it.toInt()))
            it.toInt()
        }
        else -> IndicatorType.valueOf(this).range.first
    }
}

data class IndicatorCondition(val key: IndicatorKey, val negate: Boolean)

data class ContinuedIndicator(val key: IndicatorKey, val negate: Boolean, val level: String)
