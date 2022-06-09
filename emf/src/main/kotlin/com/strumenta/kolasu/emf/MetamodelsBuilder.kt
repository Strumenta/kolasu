package com.strumenta.kolasu.emf

import org.eclipse.emf.ecore.EClass
import org.eclipse.emf.ecore.EPackage
import org.eclipse.emf.ecore.resource.Resource
import java.util.*
import kotlin.reflect.KClass

/**
 * This class is a composite around MetamodelBuilder which permits to build EPackages from Kotlin packages of classes
 * which has relations among them.
 */
class MetamodelsBuilder(val resource: Resource? = null) {
    internal val singleMetamodelsBuilders = LinkedList<MetamodelBuilder>()

    fun addMetamodel(packageName: String, nsURI: String, nsPrefix: String) {
        val smb = MetamodelBuilder(packageName, nsURI, nsPrefix, resource)
        smb.container = this
        singleMetamodelsBuilders.add(smb)
    }

    fun provideClass(kClass: KClass<*>): EClass {
        for (smb in singleMetamodelsBuilders) {
            if (smb.canProvideClass(kClass)) {
                return smb.provideClass(kClass)
            }
        }
        throw IllegalArgumentException("Unable to provide EClass for ${kClass.qualifiedName}")
    }

    fun generate(): List<EPackage> {
        return singleMetamodelsBuilders.map { it.generate() }
    }
}
