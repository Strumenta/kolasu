package com.strumenta.kolasu.model

data class AttributeValue<V>(
    var value: V?,
    var range: Range? = null,
)
