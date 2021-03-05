package com.strumenta.kolasu.emf

import com.strumenta.kolasu.model.processProperties
import org.eclipse.emf.ecore.EClass
import org.eclipse.emf.ecore.EPackage
import org.eclipse.emf.ecore.EcoreFactory
import java.util.*
import kotlin.reflect.KClass
import kotlin.reflect.jvm.jvmErasure

class MetamodelBuilder(packageName: String) {

    private val ePackage : EPackage
    private val considered = LinkedList<KClass<*>>()

    init {
        ePackage = EcoreFactory.eINSTANCE.createEPackage()
        ePackage.name = packageName
    }

    private fun toEClass(kClass: KClass<*>) : EClass {
        val eClass = EcoreFactory.eINSTANCE.createEClass()
        eClass.name = kClass.simpleName
        eClass.isAbstract = kClass.isAbstract
        return eClass
    }

    fun addClass(kClass: KClass<*>) {
        if (considered.contains(kClass)) {
            return
        }
        kClass.java.processProperties {
            if (it.provideNodes) {
                println(it.valueType.jvmErasure)
                addClass(it.valueType.jvmErasure)
            }
        }
        if (kClass.isSealed) {
            kClass.sealedSubclasses.forEach { addClass(it) }
        }
        ePackage.eClassifiers.add(toEClass(kClass))
        considered.add(kClass)
    }

    fun generate() : EPackage {
        return ePackage
    }
}