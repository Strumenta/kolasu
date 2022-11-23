package com.strumenta.kolasu.transformation

import com.strumenta.kolasu.model.Node
import org.antlr.v4.runtime.RuleContext
import kotlin.reflect.full.memberProperties

object TrivialFactoryOfParseTreeToASTNodeFactory {

    private fun convert(value: Any, astTransformer: ASTTransformer) : Any? {
        TODO()
    }

    inline fun <S : RuleContext, reified T : Node> trivialFactory(): (S, ASTTransformer) -> T? {
        return { parseTreeNode, astTransformer ->
            val constructors = T::class.constructors
            println("class ${T::class}")
            println("constructors ${constructors}")
            if (constructors.size != 1) {
                throw java.lang.RuntimeException(
                    "Trivial Factory supports only classes with exactly one constructor. " +
                        "Class ${T::class.qualifiedName} has ${constructors.size}"
                )
            }
            val constructor = constructors.first()
            val args: Array<Any?> = constructor.parameters.map {
                println("looking for value for constructor parameter ${it} ${it.name}")
                val parameterName = it.name
                println("parseTreeClass ${parseTreeNode.javaClass.kotlin}")
                val parseTreeMember = parseTreeNode.javaClass.kotlin.memberProperties.find { it.name == parameterName }
                if (parseTreeMember == null) {
                    TODO()
                } else {
                    val value = parseTreeMember.get(parseTreeNode)
                    println("producing value $value (${value?.javaClass}) for $parameterName")
                    convert(value, astTransformer)
                }

            }.toTypedArray()
            constructor.call(*args)
        }
    }
}
