package com.strumenta.kolasu.transformation

import com.strumenta.kolasu.model.Node
import com.strumenta.kolasu.model.Origin
import com.strumenta.kolasu.model.Position
import kotlin.reflect.KClass

/**
 * This is used to indicate that a Node represents some form of placeholders to be used in transformation.
 */
sealed class PlaceholderASTTransformation(val origin: Origin?, val message: String) : Origin {
    override val position: Position?
        get() = origin?.position
    override val sourceText: String?
        get() = origin?.sourceText
}

/**
 * This is used to indicate that we do not know how to transform a certain node.
 */
class MissingASTTransformation(
    origin: Origin?,
    val transformationSource: Any?,
    val expectedType: KClass<out Node>? = null,
    message: String = "Translation of a node is not yet implemented: " +
        "${if (transformationSource is Node) transformationSource.simpleNodeType else transformationSource}" +
        if (expectedType != null) " into $expectedType" else ""
) :
    PlaceholderASTTransformation(origin, message) {
    constructor(transformationSource: Node, expectedType: KClass<out Node>? = null) : this(
        transformationSource,
        transformationSource,
        expectedType
    )
}

/**
 * This is used to indicate that, while we had a transformation for a given node, that failed.
 * This is typically the case because the transformation covers only certain case and we encountered
 * one that was not covered.
 */
class FailingASTTransformation(origin: Origin?, message: String) : PlaceholderASTTransformation(origin, message)
