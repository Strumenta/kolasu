package com.strumenta.kolasu.antlr.mapping

import com.strumenta.kolasu.ast.NodeLike
import com.strumenta.kolasu.model.PossiblyNamed
import com.strumenta.kolasu.model.ReferenceByName
import com.strumenta.kolasu.model.children
import com.strumenta.kolasu.transformation.ASTTransformer
import com.strumenta.kolasu.transformation.preferredConstructor
import org.antlr.v4.runtime.ParserRuleContext
import org.antlr.v4.runtime.RuleContext
import org.antlr.v4.runtime.Token
import org.antlr.v4.runtime.tree.TerminalNode
import kotlin.reflect.KCallable
import kotlin.reflect.KType
import kotlin.reflect.full.createType
import kotlin.reflect.full.memberFunctions
import kotlin.reflect.full.memberProperties

object TrivialFactoryOfParseTreeToASTNodeTransformer {
    fun convertString(
        text: String,
        astTransformer: ASTTransformer,
        expectedType: KType,
    ): Any? {
        return when (expectedType.classifier) {
            ReferenceByName::class -> {
                ReferenceByName<PossiblyNamed>(name = text)
            }

            String::class -> {
                text
            }

            Int::class -> {
                text.toInt()
            }

            else -> {
                TODO()
            }
        }
    }

    fun convert(
        value: Any?,
        astTransformer: ASTTransformer,
        expectedType: KType,
    ): Any? {
        when (value) {
            is Token -> {
                return convertString(value.text, astTransformer, expectedType)
            }

            is List<*> -> {
                return value.map { convert(it, astTransformer, expectedType.arguments[0].type!!) }
            }

            is ParserRuleContext -> {
                return when (expectedType) {
                    String::class.createType(), String::class.createType(nullable = true) -> {
                        value.text
                    }

                    else -> {
                        astTransformer.transform(value)
                    }
                }
            }

            null -> {
                return null
            }

            is TerminalNode -> {
                return convertString(value.text, astTransformer, expectedType)
            }

            else -> TODO("value $value (${value.javaClass})")
        }
    }

    inline fun <S : RuleContext, reified T : NodeLike> trivialTransformer(
        vararg nameConversions: Pair<String, String>,
    ): (
        S,
        ASTTransformer,
    ) -> T? =
        { parseTreeNode, astTransformer ->
            val constructor = T::class.preferredConstructor()
            val args: Array<Any?> =
                constructor
                    .parameters
                    .map {
                        val parameterName = it.name
                        val searchedName = nameConversions.find { it.second == parameterName }?.first ?: parameterName
                        val parseTreeMember =
                            parseTreeNode
                                .javaClass
                                .kotlin
                                .memberProperties
                                .find { it.name == searchedName }
                        if (parseTreeMember == null) {
                            val method =
                                parseTreeNode.javaClass.kotlin.memberFunctions.find {
                                    it.name == searchedName && it.parameters.size == 1
                                }
                            if (method == null) {
                                TODO(
                                    "Unable to convert $parameterName (looking for $searchedName in " +
                                        "${parseTreeNode.javaClass})",
                                )
                            } else {
                                val value = method.call(parseTreeNode)
                                convert(value, astTransformer, it.type)
                            }
                        } else {
                            val value = parseTreeMember.get(parseTreeNode)
                            convert(value, astTransformer, it.type)
                        }
                    }.toTypedArray()
            try {
                val instance = constructor.call(*args)
                instance.children.forEach { it.parent = instance }
                instance
            } catch (e: java.lang.IllegalArgumentException) {
                throw java.lang.RuntimeException(
                    "Failure while invoking constructor $constructor with args: " +
                        args.joinToString(",") { "$it (${it?.javaClass})" },
                    e,
                )
            }
        }
}

inline fun <reified S : RuleContext, reified T : NodeLike> ASTTransformer.registerTrivialPTtoASTConversion(
    vararg nameConversions: Pair<String, String>,
) {
    this.registerNodeTransformer(
        S::class,
        TrivialFactoryOfParseTreeToASTNodeTransformer.trivialTransformer<S, T>(*nameConversions),
    )
}

inline fun <reified S : RuleContext, reified T : NodeLike> ParseTreeToASTTransformer.registerTrivialPTtoASTConversion(
    vararg nameConversions: Pair<KCallable<*>, KCallable<*>>,
) = this.registerTrivialPTtoASTConversion<S, T>(
    *nameConversions
        .map { it.first.name to it.second.name }
        .toTypedArray(),
)

inline fun <reified S : RuleContext, reified T : NodeLike> ParseTreeToASTTransformer.unwrap(
    wrappingMember: KCallable<*>,
) {
    this.registerNodeTransformer(S::class) { parseTreeNode, astTransformer ->
        val wrapped = wrappingMember.call(parseTreeNode)
        astTransformer.transform(wrapped) as T?
    }
}
