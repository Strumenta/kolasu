package com.strumenta.kolasu.model

import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

class ObservableProperty<CT,PT>(val initialValue: PT) {
    operator fun provideDelegate(thisRef: CT,
                                 prop: KProperty<*>
    ) : ReadWriteProperty<CT, PT>{
        return object : ReadWriteProperty<CT, PT> {
            private var value: PT = initialValue
            override fun getValue(thisRef: CT, property: KProperty<*>): PT {
                println("getValue for ${property.name} ${property.returnType}: ${this.value}")
                return this.value
            }

            override fun setValue(thisRef: CT, property: KProperty<*>, value: PT) {
                println("setValue for ${property.name} ${property.returnType}: ${this.value} -> $value")
                this.value = value
            }

        }
    }
}

class ObservableReference<CT,PT>(val initialValue: PT?) {
    constructor() : this(null)
    operator fun provideDelegate(thisRef: CT,
                                 prop: KProperty<*>
    ) : ReadWriteProperty<CT, PT?>{
        return object : ReadWriteProperty<CT, PT?> {
            private var value: PT? = initialValue
            override fun getValue(thisRef: CT, property: KProperty<*>): PT? {
                println("getValue for ${property.name} ${property.returnType}: ${this.value}")
                return this.value
            }

            override fun setValue(thisRef: CT, property: KProperty<*>, value: PT?) {
                println("setValue for ${property.name} ${property.returnType}: ${this.value} -> $value")
                this.value = value
            }

        }
    }
}

fun <CT,PT>obsProperty(initialValue: PT): ObservableProperty<CT, PT> {
    return ObservableProperty(initialValue)
}

fun <CT,PT:ASTNode>obsReference(): ObservableReference<CT, PT?> {
    return ObservableReference()
}

class ExampleNode(foo: String) : ASTNode() {
    var p1 by obsProperty(foo)
    val r2 : ExampleNode by obsReference()
}

fun main(args: Array<String>) {
    val n = ExampleNode("a")
    n.p1 = "b"
    n.p1 = "c"
    n.p1 = "d"
    println(n.p1)
}