package com.strumenta.kolasu.mapping

import org.antlr.v4.runtime.ParserRuleContext

/**
 * Translate the given node and ensure a certain type will be obtained.
 *
 * Example:
 * ```
 * JPostIncrementExpr(translateCasted<JExpression>(expression().first()))
 * ```
 */
fun <T> ParseTreeToASTTransformer.translateCasted(original: ParserRuleContext): T {
    val result = transformToNode(original)
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
fun <T> ParseTreeToASTTransformer.translateList(original: Collection<out ParserRuleContext>?): MutableList<T> {
    return original?.map { transform(it) as List<T> }?.flatten()?.toMutableList() ?: mutableListOf()
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
fun <T> ParseTreeToASTTransformer.translateOptional(original: ParserRuleContext?): T? {
    return original?.let { transformToNode(it) as T }
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
        require(nodeChildren.size == 1)
        require(nodeChildren[0] is ParserRuleContext)
        return nodeChildren[0]
    }
