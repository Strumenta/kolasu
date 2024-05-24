package com.strumenta.kolasu.language

import com.strumenta.kolasu.model.NodeLike
import com.strumenta.kolasu.model.asAttribute
import com.strumenta.kolasu.model.asContainment
import com.strumenta.kolasu.model.asReference
import com.strumenta.kolasu.model.isAttribute
import com.strumenta.kolasu.model.isConcept
import com.strumenta.kolasu.model.isConceptInterface
import com.strumenta.kolasu.model.isContainment
import com.strumenta.kolasu.model.isReference
import com.strumenta.kolasu.model.nodeProperties
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.full.allSupertypes

fun StarLasuLanguage.explore(vararg classes: Class<*>) {
    explore(*classes.map { it.kotlin }.toTypedArray())
}

private fun collectFromType(
    kType: KType,
    classesCollected: MutableList<KClass<*>>,
) {
    if ((
            kType
                .classifier as? KClass<*>
        )?.allSupertypes?.any { it.classifier == List::class } == true
    ) {
        collect(kType.arguments[0].type!!.classifier as KClass<*>, classesCollected)
    } else {
        collect(kType.classifier as KClass<*>, classesCollected)
    }
}

private fun collect(
    kClass: KClass<*>,
    classesCollected: MutableList<KClass<*>>,
) {
    if (kClass == NodeLike::class) {
        return
    }
    if (classesCollected.contains(kClass)) {
        return
    } else {
        classesCollected.add(kClass)
    }

    if (kClass.isSealed) {
        kClass.sealedSubclasses.forEach {
            collect(it, classesCollected)
        }
    }

    kClass.nodeProperties.forEach { prop ->
        if (prop.isContainment()) {
            if (prop.returnType.classifier == List::class) {
                collect(
                    prop
                        .returnType
                        .arguments[0]
                        .type!!
                        .classifier as KClass<*>,
                    classesCollected,
                )
            } else {
                collectFromType(prop.returnType, classesCollected)
            }
        } else if (prop.isReference()) {
            collect(
                prop
                    .returnType
                    .arguments[0]
                    .type!!
                    .classifier as KClass<*>,
                classesCollected,
            )
        }
    }
}

fun StarLasuLanguage.explore(vararg kClasses: KClass<*>) {
    val classesCollected = mutableListOf<KClass<*>>()
    kClasses.forEach { kClass ->
        if (this.types.none { it.name == kClass.simpleName }) {
            collect(kClass, classesCollected)

            // create
            classesCollected.forEach { kClass ->
                if (kClass.isConcept) {
                    val concept = Concept(this, kClass.simpleName!!)
                    concept.explicitlySetKotlinClass = kClass
                    this.types.add(concept)
                } else if (kClass.isConceptInterface) {
                    val conceptInterface = ConceptInterface(this, kClass.simpleName!!)
                    this.types.add(conceptInterface)
                } else if (kClass.allSupertypes.any { it.classifier == List::class }) {
                    throw IllegalStateException("This should not happen")
                } else {
                    TODO("Neither a Concept or a ConceptInterface: $kClass")
                }
            }
        }
    }

    // populate
    classesCollected.forEach { kClass ->
        val classifier: Classifier? =
            if (kClass.isConcept) {
                getConcept(kClass.simpleName!!)
            } else if (kClass.isConceptInterface) {
                getConceptInterface(kClass.simpleName!!)
            } else {
                null
            }
        if (classifier != null) {
            kClass.nodeProperties.forEach { prop ->
                if (prop.isContainment()) {
                    val containment = prop.asContainment(this)
                    classifier.declaredFeatures.add(containment)
                }
                if (prop.isReference()) {
                    classifier.declaredFeatures.add(prop.asReference(this))
                }
                if (prop.isAttribute()) {
                    classifier.declaredFeatures.add(prop.asAttribute())
                }
            }
        }
    }
}
