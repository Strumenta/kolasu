package com.strumenta.kolasu.model

abstract class JVMSingleAnnotation : SingleAnnotation() {
    override val annotation: String
        get() = this.javaClass.canonicalName
}

abstract class JVMMultipleAnnotation : MultipleAnnotation() {
    override val annotation: String
        get() = this.javaClass.canonicalName
}
