package com.strumenta.kolasu.exceptions

/**
 * Multi-platform IllegalStateException.
 */
expect class IllegalStateException : Throwable {
    constructor(message: String)
}
