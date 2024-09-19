package com.strumenta.kolasu.transformation

import com.strumenta.kolasu.model.Node
import com.strumenta.kolasu.model.NodeLike
import com.strumenta.kolasu.model.NodeOrigin
import com.strumenta.kolasu.model.Origin
import com.strumenta.kolasu.model.Range
import com.strumenta.kolasu.traversing.walkAncestors
import kotlin.reflect.KClass

/**
 * This indicates if the Node itself is marked as a placeholder. Note that the Node could not be directly marked
 * as such and still be the descendants of such type of Node. In other words it could be in a placeholder tree.
 * This operation is not expensive to perform.
 */
val NodeLike.isDirectlyPlaceholderASTTransformation: Boolean
    get() = this.origin is PlaceholderASTTransformation

/**
 * This indicates if the Node itself is marked as a placeholder or if any of its ancestors are. Note that the Node
 * could not be directly marked as such and still be the descendants of such type of Node. In other words it could be
 * in a placeholder tree.
 * This operation is expensive to perform.
 */
val NodeLike.isDirectlyOrIndirectlyAPlaceholderASTTransformation: Boolean
    get() =
        this.isDirectlyPlaceholderASTTransformation ||
            this.walkAncestors().any {
                it.isDirectlyPlaceholderASTTransformation
            }

/**
 * This is used to indicate that a Node represents some form of placeholders to be used in transformation.
 */
sealed class PlaceholderASTTransformation(
    val origin: Origin?,
    val message: String,
) : Origin {
    private var overriddenRange: Range? = null
    override var range: Range?
        get() = origin?.range ?: overriddenRange
        set(value) {
            if (origin == null) {
                overriddenRange = value
            } else {
                origin.range = value
            }
        }
    override val sourceText: String?
        get() = origin?.sourceText
}

/**
 * This is used to indicate that we do not know how to transform a certain node.
 */
class MissingASTTransformation(
    origin: Origin?,
    val transformationSource: Any?,
    val expectedType: KClass<out NodeLike>? = null,
    message: String =
        "Translation of a node is not yet implemented: " +
            "${if (transformationSource is Node) transformationSource.concept.name else transformationSource}" +
            if (expectedType != null) " into $expectedType" else "",
) : PlaceholderASTTransformation(origin, message) {
    constructor(transformationSource: NodeLike, expectedType: KClass<out NodeLike>? = null) : this(
        NodeOrigin(transformationSource),
        transformationSource,
        expectedType,
    )
}

/**
 * This is used to indicate that, while we had a transformation for a given node, that failed.
 * This is typically the case because the transformation covers only certain case and we encountered
 * one that was not covered.
 */
class FailingASTTransformation(
    origin: Origin?,
    message: String,
) : PlaceholderASTTransformation(origin, message)
