package com.strumenta.kolasu.lionweb

/**
 * Identify if a Node is a partition or not. This is based on the type of the Node.
 * Nodes of Partition types should be always and exclusively used as partitions and never be placed within
 * partitions.
 * Conversely nodes of non-Partition types can only used within partitions and never be partitions themselves.
 */
val KNode.isPartition
    get() = this::class.annotations.any { it is LionWebPartition }
