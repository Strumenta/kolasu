package com.strumenta.kolasu.transformation

import com.strumenta.kolasu.model.NodeLike

/**
 * Transformer that, given a tree node, will instantiate the corresponding transformed node.
 */
class MPNodeTransformer<Source, Output : NodeLike>(
    val constructorToUse: (Source, MPASTTransformer, MPNodeTransformer<Source, Output>) -> List<Output>,
) {
    companion object {
        fun <Source : Any, Output : NodeLike> single(
            singleConstructor: (Source, MPASTTransformer, MPNodeTransformer<Source, Output>) -> Output?,
        ): MPNodeTransformer<Source, Output> =
            MPNodeTransformer { source, at, nf ->
                val result = singleConstructor(source, at, nf)
                if (result == null) emptyList() else listOf(result)
            }
    }
}
