package com.strumenta.kolasu.antlr4j.mapping

import com.strumenta.kolasu.antlr4j.parsing.getOriginalText
import com.strumenta.kolasu.model.observable.ObservableList
import com.strumenta.kolasu.transformation.ASTTransformer
import com.strumenta.kolasu.transformation.ParameterConverter
import org.antlr.v4.runtime.ParserRuleContext
import org.antlr.v4.runtime.tree.ParseTree
import kotlin.reflect.KParameter
import kotlin.reflect.full.createType

/**
 * Translate the given node and ensure a certain type will be obtained.
 *
 * Example:
 * ```
 * JPostIncrementExpr(translateCasted<JExpression>(expression().first()))
 * ```
 */
fun <T> ASTTransformer.translateCasted(original: Any): T {
    val result = transform(original)
    if (result is Nothing) {
        throw IllegalStateException("Transformation produced Nothing")
    }
    return result as T
}

/**
 * Translate a whole collection into a mutable list, translating each element and ensuring
 * the list has the expected type.
 *
 * Example:
 * ```
 * JExtendsType(translateCasted(pt.typeType()), translateList(pt.annotation()))
 * ```
 */
fun <T> ASTTransformer.translateList(original: Collection<out Any>?): MutableList<T> {
    return original?.map { transformIntoNodes(it) as List<T> }?.flatten()?.toMutableList() ?: mutableListOf()
}

fun <E> List<E>.toObservableList(): ObservableList<E> {
    val ol = ObservableList<E>()
    ol.addAll(this)
    return ol
}

/**
 * Translate the given node and ensure a certain type will be obtained, if the value is not null.
 * If the value is null, null is returned.
 *
 * Example:
 * ```
 *  JVariableDeclarator(
 *      name = pt.variableDeclaratorId().text,
 *      arrayDimensions = mutableListOf(),
 *      initializer = translateOptional(pt.variableInitializer())
 *  )
 *  ```
 */
fun <T> ASTTransformer.translateOptional(original: Any?): T? {
    return original?.let { transform(it) as T }
}

/**
 * Translate the only child (of type ParseRuleContext) and ensure the resulting value
 * as the expected type.
 *
 * Example:
 * ```
 * registerNodeFactory<MemberDeclarationContext, JEntityMember> {
 *     translateOnlyChild<JEntityMember>(this)
 * }
 * ```
 */
fun <T> ParseTreeToASTTransformer.translateOnlyChild(parent: ParserRuleContext): T {
    return translateCasted(parent.onlyChild)
}

/**
 * It returns the only child (of type ParseRuleContext). If there is no children or more than
 * one child, an exception is thrown.
 */
val ParserRuleContext.onlyChild: ParserRuleContext
    get() {
        val nodeChildren = children.filterIsInstance<ParserRuleContext>()
        require(nodeChildren.size == 1) {
            "ParserRuleContext was expected to have exactly one child, " +
                "while it has ${nodeChildren.size}. ParserRuleContext: ${this.getOriginalText()} " +
                "(${this.javaClass.canonicalName})"
        }
        return nodeChildren[0]
    }

object ParseTreeToStringParameterConverter : ParameterConverter {
    override fun isApplicable(
        kParameter: KParameter,
        value: Any?,
    ): Boolean = kParameter.type == String::class.createType() && value is ParseTree

    override fun convert(
        kParameter: KParameter,
        value: Any?,
    ): Any? = (value as ParseTree).text
}
