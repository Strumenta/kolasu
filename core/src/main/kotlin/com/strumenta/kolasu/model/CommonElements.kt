package com.strumenta.kolasu.model

@NodeType
interface Statement

@NodeType
interface Expression

/**
 * This should be used for definitions of classes, interfaces, structures
 */
@NodeType
interface EntityDeclaration

@NodeType
interface QuotedElement {
    var placeholderName: String?
    @property:Internal
    val multipleQuotedElement: Boolean

    /**
     * Return true if the node can be represented by this QuotedElement.
     * For example, a certain QuotedElement could be used only to match expressions or statements.
     * In case this is a multiple quoted element, and it is used to match many elements, each element
     * should be passed to this method. For example, if a certain Quoted Element is used to match a sequence of three
     * statements S1, S2, and S3, all of them should be passed, one by one, as parameters of this method.
     */
    fun applicableTo(node: Node) : Boolean = true
}


