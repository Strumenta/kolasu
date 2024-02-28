package com.strumenta.kolasu.exceptions

expect class IllegalStateException : Throwable {
    constructor(message: String)
}
