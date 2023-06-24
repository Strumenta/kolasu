import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.asTypeName
import com.strumenta.kolasu.lionweb.StarLasuLWLanguage
import com.strumenta.kolasu.model.Node
import io.lionweb.lioncore.java.language.Concept
import io.lionweb.lioncore.java.language.Containment
import io.lionweb.lioncore.java.language.DataType
import io.lionweb.lioncore.java.language.FeaturesContainer
import io.lionweb.lioncore.java.language.Language
import io.lionweb.lioncore.java.language.LionCoreBuiltins
import io.lionweb.lioncore.java.language.Property
import io.lionweb.lioncore.java.language.Reference
import java.lang.IllegalStateException

data class KotlinFile(val path: String , val code: String)

class ASTClassesGenerator(val packageName: String, val language: Language) {

    private fun typeName(featuresContainer: FeaturesContainer<*>) : TypeName {
        return when {
            featuresContainer.id == StarLasuLWLanguage.ASTNode.id -> {
                Node::class.java.asTypeName()
            }
            featuresContainer.language == this.language -> {
                ClassName.bestGuess("${packageName}.${featuresContainer.name}")
            }
            else -> {
                TODO()
            }
        }
    }

    private fun typeName(dataType: DataType<*>) : TypeName {
        if (dataType == LionCoreBuiltins.getString()) {
            return String::class.java.asTypeName()
        } else if (dataType == LionCoreBuiltins.getBoolean()) {
            return Boolean::class.java.asTypeName()
        } else {
            TODO("DataType: $dataType")
        }
    }

    fun generateClasses() : Set<KotlinFile> {
        val fileSpecBuilder = FileSpec.builder(packageName, "${language.name}AST.kt")
        language.elements.forEach { element ->
            when (element) {
                is Language -> Unit
                is Concept -> {
                    val typeSpec = TypeSpec.classBuilder(element.name!!)
                    if (element.isAbstract) {
                        typeSpec.modifiers.add(KModifier.SEALED)
                    }
                    if (element.features.isNotEmpty()) {
                        typeSpec.modifiers.add(KModifier.DATA)
                    }
                    if (element.extendedConcept == null) {
                        throw IllegalStateException()
                    } else {
                        typeSpec.superclass(typeName(element.extendedConcept!!))
                    }
                    element.implemented.forEach {
                        TODO()
                    }
                    val constructor = FunSpec.constructorBuilder()
                    element.features.forEach { feature ->
                        when (feature) {
                            is Property -> {
                                val type = typeName(feature.type!!)
                                constructor.addParameter(feature.name!!, type)
                                typeSpec.addProperty(PropertySpec.builder(feature.name!!, type)
                                    .mutable(true).initializer(feature.name!!).build())
                            }
                            is Containment -> {
                                var type = typeName(feature.type!!)
                                if (feature.isMultiple) {
                                   type = MutableList::class.java.asClassName().parameterizedBy(type)
                                }
                                constructor.addParameter(feature.name!!, type)
                                typeSpec.addProperty(PropertySpec.builder(feature.name!!, type)
                                    .mutable(true).initializer(feature.name!!).build())
                            }
                            is Reference -> TODO()
                        }
                    }
                    if (constructor.parameters.isNotEmpty()) {
                        typeSpec.primaryConstructor(constructor.build())
                    }
                    fileSpecBuilder.addType(typeSpec.build())
                }
                else -> TODO()
            }
        }
        val file = KotlinFile(path = "ast.kt", fileSpecBuilder.build().toString())
        return setOf(file)
    }
}