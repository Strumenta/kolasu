package com.strumenta.kolasu.transformation

import com.strumenta.kolasu.model.Node
import com.strumenta.kolasu.model.PossiblyNamed
import com.strumenta.kolasu.model.ReferenceByName
import org.antlr.v4.runtime.ParserRuleContext
import org.antlr.v4.runtime.RuleContext
import org.antlr.v4.runtime.Token
import kotlin.reflect.KClass
import kotlin.reflect.KClassifier
import kotlin.reflect.KType
import kotlin.reflect.full.memberProperties

object TrivialFactoryOfParseTreeToASTNodeFactory {

    public fun convert(value: Any?, astTransformer: ASTTransformer, expectedType: KType) : Any? {
        if (value is Token) {
            println(expectedType)
            if (expectedType.classifier == ReferenceByName::class) {
                return ReferenceByName<PossiblyNamed>(name = value.text)
            }
            return value.text
        } else if (value is List<*>) {
            return value.map { convert(it, astTransformer, expectedType.arguments[0].type!!) }
            //TODO("type ${expectedType} ${expectedType.javaClass}")
        } else if (value is ParserRuleContext) {
            return astTransformer.transform(value)
        }
        TODO("value ${value} (${value?.javaClass})")
    }

    inline fun <S : RuleContext, reified T : Node> trivialFactory(vararg nameConversions: Pair<String, String>): (S, ASTTransformer) -> T? {
        return { parseTreeNode, astTransformer ->
            val constructors = T::class.constructors
            //println("class ${T::class}")
            //println("constructors ${constructors}")
            if (constructors.size != 1) {
                throw java.lang.RuntimeException(
                    "Trivial Factory supports only classes with exactly one constructor. " +
                        "Class ${T::class.qualifiedName} has ${constructors.size}"
                )
            }
            val constructor = constructors.first()
            val args: Array<Any?> = constructor.parameters.map {
                //println("looking for value for constructor parameter ${it} ${it.name}")
                val parameterName = it.name
                val searchedName = nameConversions.find { it.second == parameterName }?.let { it.first } ?: parameterName
                //println("parseTreeClass ${parseTreeNode.javaClass.kotlin}")
                val parseTreeMember = parseTreeNode.javaClass.kotlin.memberProperties.find { it.name == searchedName }
                if (parseTreeMember == null) {
                    TODO("Unable to convert ${parameterName} (looking for ${searchedName})")
                } else {
                    val value = parseTreeMember.get(parseTreeNode)
                    //println("producing value $value (${value?.javaClass}) for $parameterName")
                    convert(value, astTransformer, it.type)
                }

            }.toTypedArray()
            constructor.call(*args)
        }
    }
}

inline fun <reified S : RuleContext, reified T : Node> ASTTransformer.registerTrivialPTtoASTConversion(vararg nameConversions: Pair<String, String>) {
    this.registerNodeFactory(
        S::class,
        TrivialFactoryOfParseTreeToASTNodeFactory.trivialFactory<S, T>(*nameConversions)
    )
}