package com.strumenta.kolasu.transformation

import com.strumenta.kolasu.model.NodeLike

val c = 3

//
// import com.strumenta.kolasu.model.NodeLike
// import kotlin.reflect.KClass
// import kotlin.reflect.KMutableProperty1
// import kotlin.reflect.KProperty1
// //import kotlin.reflect.full.memberFunctions
// //import kotlin.reflect.full.memberProperties
//

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
