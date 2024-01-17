package com.strumenta.kolasu.utils

fun String.capitalize() = if (this.isBlank()) this else this.replaceFirstChar { it.uppercase()}