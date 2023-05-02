package com.smeup.rpgparser.parsing.ast

import com.strumenta.kolasu.model.Range

// *IN01..*IN99
data class PredefinedIndicatorExpr(val index: Int) : AssignableExpression() {
    constructor(index: Int, range: Range) : this(index) {
        this.range = range
    }
    init {
        require(index in 1..99) { "Indicator not in range 01 to 99 at $range" }
    }
    override fun size(): Int = 1
}

// *IN
class PredefinedGlobalIndicatorExpr : AssignableExpression() {
    override fun size(): Int {
        TODO("not implemented") // To change body of created functions use File | Settings | File Templates.
    }
}

data class DataWrapUpIndicatorExpr(val dataWrapUpChoice: DataWrapUpChoice) :
    AssignableExpression() {
    override fun size(): Int = 1
}
