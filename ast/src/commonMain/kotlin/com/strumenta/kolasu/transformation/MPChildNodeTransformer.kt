package com.strumenta.kolasu.transformation

val b = 2


 /**
 * Information on how to retrieve a child node.
 *
 * The setter could be null, if the property is not mutable. In that case the value
 * must necessarily be passed when constructing the parent.
 */
 data class MPChildNodeTransformer<Source, Target, Child>(
    val name: String,
    val get: (Source) -> Any?,
    val setter: ((Target, Child?) -> Unit)?,
 ) {
//    fun set(
//        node: Target,
//        child: Child?,
//    ) {
//        if (setter == null) {
//            throw IllegalStateException("Unable to set value $name in  $node")
//        }
//        try {
//            setter!!(node, child)
//        } catch (e: Exception) {
//            throw Exception("$name could not set child $child of $node using $setter", e)
//        }
//    }
}
