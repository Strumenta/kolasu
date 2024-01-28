package com.strumenta.kolasu.ast

abstract class JVMSingleAnnotation : SingleAnnotation() {
    override val annotationType: String
        get() = this.javaClass.canonicalName
}

abstract class JVMMultipleAnnotation : MultipleAnnotation() {
    override val annotationType: String
        get() = this.javaClass.canonicalName
}
