package com.strumenta.kolasu.lionweb

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.asTypeName
import com.strumenta.kolasu.model.Named
import com.strumenta.kolasu.model.Node
import com.strumenta.kolasu.model.ReferenceByName
import io.lionweb.lioncore.java.language.Concept
import io.lionweb.lioncore.java.language.Containment
import io.lionweb.lioncore.java.language.DataType
import io.lionweb.lioncore.java.language.Classifier
import io.lionweb.lioncore.java.language.Enumeration
import io.lionweb.lioncore.java.language.Feature
import io.lionweb.lioncore.java.language.Language
import io.lionweb.lioncore.java.language.LionCoreBuiltins
import io.lionweb.lioncore.java.language.Property
import io.lionweb.lioncore.java.language.Reference
import io.lionweb.lioncore.java.self.LionCore
import org.jetbrains.kotlin.konan.file.File

data class KotlinFile(val path: String, val code: String)

/**
 * This class generates Kotlin code for a given LIonWeb Language.
 */
class ASTGenerator(val packageName: String, val language: Language) {

    private fun processFeature(feature: Feature<*>, constructor: FunSpec.Builder,
                               typeSpec: TypeSpec.Builder, inherited: Boolean, sealed: Boolean) {
        val modifiers = mutableListOf<KModifier>()
        if (inherited) {
            modifiers.add(KModifier.OVERRIDE)
        }
        if (sealed) {
            modifiers.add(KModifier.OPEN)
        }
        when (feature) {
            is Property -> {
                val type = typeName(feature.type!!)
                constructor.addParameter(feature.name!!, type)
                typeSpec.addProperty(
                    PropertySpec.builder(feature.name!!, type)
                        .addModifiers(modifiers)
                        .mutable(true).initializer(feature.name!!).build()
                )
            }

            is Containment -> {
                var type = typeName(feature.type!!)
                if (feature.isMultiple) {
                    type =
                        ClassName.bestGuess("kotlin.collections.MutableList").parameterizedBy(type)
                }
                constructor.addParameter(feature.name!!, type)
                typeSpec.addProperty(
                    PropertySpec.builder(feature.name!!, type)
                        .addModifiers(modifiers)
                        .mutable(true).initializer(feature.name!!).build()
                )
            }

            is Reference -> {
                var type = typeName(feature.type!!)
                type =
                    ClassName.bestGuess(ReferenceByName::class.qualifiedName!!).parameterizedBy(type)
                constructor.addParameter(feature.name!!, type)
                typeSpec.addProperty(
                    PropertySpec.builder(feature.name!!, type)
                        .addModifiers(modifiers)
                        .mutable(true).initializer(feature.name!!).build()
                )
            }
        }
    }

    fun generateClasses(existingKotlinClasses: Set<String> = emptySet()): Set<KotlinFile> {
        val fileSpecBuilder = FileSpec.builder(packageName, "${language.name}AST.kt")
        language.elements.forEach { element ->
            when (element) {
                is Concept -> {
                    val typeSpec = TypeSpec.classBuilder(element.name!!)
                    val fqName = "$packageName.${element.name!!}"
                    if (fqName in existingKotlinClasses) {
                        println("    Skipping ${element.name} as a Kotlin class with that name already exist")
                        fileSpecBuilder.addFileComment(
                            "Skipping ${element.name} as a Kotlin class with that name already exist"
                        )
                    } else {
                        if (element.isAbstract) {
                            typeSpec.modifiers.add(KModifier.SEALED)
                        }
                        if (element.allFeatures().isNotEmpty() && !element.isAbstract) {
                            typeSpec.modifiers.add(KModifier.DATA)
                        }
                        if (element.extendedConcept == null) {
                            throw IllegalStateException()
                        } else {
                            typeSpec.superclass(typeName(element.extendedConcept!!))
                            element.extendedConcept.allFeatures().forEach {
                                typeSpec.addSuperclassConstructorParameter(it.name)
                            }
                        }
                        element.implemented.forEach {
                            typeSpec.addSuperinterface(typeName(it))
                        }
                        val constructor = FunSpec.constructorBuilder()
                        element.inheritedFeatures().forEach { feature ->
                            processFeature(feature, constructor, typeSpec, true, element.isAbstract)
                        }
                        element.features.forEach { feature ->
                            processFeature(feature, constructor, typeSpec, false, element.isAbstract)
                        }
                        if (constructor.parameters.isNotEmpty()) {
                            typeSpec.primaryConstructor(constructor.build())
                        }
                        fileSpecBuilder.addType(typeSpec.build())
                    }
                }
                is Enumeration -> {
                    val typeSpec = TypeSpec.enumBuilder(element.name!!)
                    element.literals.forEach {
                        typeSpec.addEnumConstant(it.name)
                    }
                    fileSpecBuilder.addType(typeSpec.build())
                }
                else -> TODO("element is $element")
            }
        }
        val path = if (packageName.isNullOrEmpty()) "AST.kt" else packageName.split(".").joinToString(File.separator) + File.separator + "AST.kt"
        val file = KotlinFile(path = path, fileSpecBuilder.build().toString())
        return setOf(file)
    }

    private fun typeName(classifier: Classifier<*>): TypeName {
        return when {
            classifier.id == StarLasuLWLanguage.ASTNode.id -> {
                Node::class.java.asTypeName()
            }
            classifier.id == LionCoreBuiltins.getNode().id -> {
                Node::class.java.asTypeName()
            }
            classifier.id == LionCoreBuiltins.getINamed().id -> {
                Named::class.java.asTypeName()
            }
            classifier.language == this.language -> {
                ClassName.bestGuess("$packageName.${classifier.name}")
            }
            else -> {
                TODO("Classifier $classifier. ID ${classifier.id}, NODE id: ${LionCoreBuiltins.getNode().id}")
            }
        }
    }

    private fun typeName(dataType: DataType<*>): TypeName {
        return when {
            dataType == LionCoreBuiltins.getString() -> {
                ClassName.bestGuess("kotlin.String")
            }
            dataType == LionCoreBuiltins.getBoolean() -> {
                Boolean::class.java.asTypeName()
            }
            dataType == LionCoreBuiltins.getInteger() -> {
                ClassName.bestGuess("kotlin.Int")
            }
            dataType.language == this.language -> {
                ClassName.bestGuess("$packageName.${dataType.name}")
            }
            else -> {
                TODO("DataType: $dataType")
            }
        }
    }
}
