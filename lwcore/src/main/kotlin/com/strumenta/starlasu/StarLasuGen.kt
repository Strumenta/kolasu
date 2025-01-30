package com.strumenta.starlasu

@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY, AnnotationTarget.FIELD,
    AnnotationTarget.CONSTRUCTOR, AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.CONSTRUCTOR)
@Retention(AnnotationRetention.RUNTIME)
annotation class StarLasuGen()
