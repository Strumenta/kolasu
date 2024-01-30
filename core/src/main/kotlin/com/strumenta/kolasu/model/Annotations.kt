package com.strumenta.kolasu.model

abstract class JVMSingleAnnotation : SingleAnnotation() {
    override val annotationType: String
        get() = this.javaClass.canonicalName
}

abstract class JVMMultipleAnnotation : MultipleAnnotation() {
    override val annotationType: String
        get() = this.javaClass.canonicalName
}
