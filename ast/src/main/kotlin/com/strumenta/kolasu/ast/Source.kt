package com.strumenta.kolasu.ast

abstract class Source : Comparable<Source> {
    protected abstract fun stringDescription(): String

    override fun compareTo(other: Source): Int = this.stringDescription().compareTo(other.stringDescription())
}
