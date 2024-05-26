package com.strumenta.kolasu.model

data class PropertyValue<V>(
    var value: V?,
    var range: Range? = null,
)
