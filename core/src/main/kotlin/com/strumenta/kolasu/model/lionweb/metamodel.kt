package com.strumenta.kolasu.model.lionweb

import com.strumenta.kolasu.model.ASTNode
import org.lionweb.lioncore.java.metamodel.Metamodel
import java.lang.IllegalArgumentException
import java.lang.IllegalStateException
import kotlin.reflect.KClass
import kotlin.reflect.full.staticProperties
import kotlin.reflect.jvm.jvmName

internal fun requireMetamodelFor(kClass: KClass<out Any>): ReflectionBasedMetamodel {
    return metamodelFor(kClass) ?: throw IllegalArgumentException("No metamodel found for $kClass")
}

internal fun metamodelFor(kClass: KClass<out Any>): ReflectionBasedMetamodel? {
    val metamodelInstance: ReflectionBasedMetamodel? = if (kClass.jvmName.contains("$")) {
        val outerClass = kClass.java.declaringClass.kotlin
        val metamodelKClass = outerClass.nestedClasses.find { it.simpleName == "Metamodel" }
        if (metamodelKClass == null) {
            return metamodelFor(outerClass as KClass<out ASTNode>)
        }
        ReflectionBasedMetamodel.INSTANCES[metamodelKClass]
            ?: metamodelKClass?.objectInstance as? ReflectionBasedMetamodel
    } else {
        val metamodelQName = kClass.qualifiedName!!.removeSuffix(".${kClass.simpleName}") + ".Metamodel"
        val classLoader = kClass.java.classLoader ?: throw IllegalStateException("No class loader for ${kClass.java}")
        val metamodelKClass = try {
            classLoader.loadClass(metamodelQName).kotlin
        } catch (e: ClassNotFoundException) {
            throw RuntimeException(
                "Unable to find the metamodel for Kotlin class $kClass. " +
                    "We looked for Java class $metamodelQName",
                e
            )
        }
        if (metamodelKClass == null) {
            throw IllegalStateException("Metamodel class not found")
        }
        val metamodelInstanceRaw = metamodelKClass.staticProperties.find { it.name == "INSTANCE" }?.let { instance ->
            val instanceRaw = instance.get()
            if (instanceRaw !is Metamodel) {
                throw IllegalStateException(
                    "value of INSTANCE field for $metamodelKClass is not a " +
                        "Metamodel but it is $instanceRaw"
                )
            }
            instanceRaw as ReflectionBasedMetamodel
        } ?: metamodelKClass.java.fields.find { it.name == "INSTANCE" }?.get(null)
            ?: ReflectionBasedMetamodel.INSTANCES[metamodelKClass] ?: throw java.lang.RuntimeException(
            "Unable to get metamodel instance for $metamodelKClass"
        )
        if (metamodelInstanceRaw !is ReflectionBasedMetamodel) {
            throw IllegalStateException(
                "Object instance for $metamodelKClass is not a ReflectionBasedMetamodel" +
                    " but it is $metamodelInstanceRaw"
            )
        }
        metamodelInstanceRaw
    }
    return metamodelInstance
}
