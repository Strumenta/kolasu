package com.strumenta.kolasu.ast

interface Origin {
    var range: Range?
    val sourceText: String?
    val source: Source?
        get() = range?.source
}
