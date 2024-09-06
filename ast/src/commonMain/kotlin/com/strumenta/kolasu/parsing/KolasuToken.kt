package com.strumenta.kolasu.parsing

import com.strumenta.kolasu.model.Range

/**
 * A token is a portion of text that has been assigned a category.
 */
open class KolasuToken(
    open val category: TokenCategory,
    open val range: Range,
    open val text: String?,
)
