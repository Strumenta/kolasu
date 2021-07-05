package com.smeup.rpgparser.parsing.ast

import com.strumenta.kolasu.model.Position

// *IN01..*IN99
data class PredefinedIndicatorExpr(val index: Int, override val specifiedPosition: Position? = null) :
    AssignableExpression(specifiedPosition) {
    init {
        require(index in 1..99) { "Indicator not in range 01 to 99 at $specifiedPosition" }
    }
    override fun size(): Int = 1
}

// *IN
data class PredefinedGlobalIndicatorExpr(override val specifiedPosition: Position? = null) :
    AssignableExpression(specifiedPosition) {
    override fun size(): Int {
        TODO("not implemented") // To change body of created functions use File | Settings | File Templates.
    }
}

data class DataWrapUpIndicatorExpr(
    val dataWrapUpChoice: DataWrapUpChoice,
    override val specifiedPosition: Position? = null
) :
    AssignableExpression(specifiedPosition) {
    override fun size(): Int = 1
}
