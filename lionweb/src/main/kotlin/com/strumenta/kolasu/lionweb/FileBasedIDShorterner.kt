package com.strumenta.kolasu.lionweb

import com.strumenta.kolasu.ids.NodeIdProvider
import com.strumenta.kolasu.model.Node
import java.io.BufferedWriter
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStreamWriter
import java.util.IdentityHashMap

class FileBasedIDShortener(val file: File, val baseNodeIdProvider: NodeIdProvider = StructuralLionWebNodeIdProvider()) : NodeIdProvider {

    private var next = 1L
    private val map = BiMap<String, String>()
    private val fos = FileOutputStream(file, true)
    private val writer = OutputStreamWriter(fos, Charsets.UTF_8)
    private val bWriter = BufferedWriter(writer)

    init {
        if (file.exists()) {
            file.readLines().filter { it.isNotBlank() }.forEach { line ->
                val parts = line.split(",")
                require(parts.size == 2)
                val originalNodeID = parts[0]
                val shorterNodeID = parts[1]
                map.associate(originalNodeID, shorterNodeID)
                val n = shorterNodeID.toLong()
                next = next.coerceAtLeast(n + 1)
            }
        }
    }

    override fun id(kNode: Node): String {
        val baseID = baseNodeIdProvider.id(kNode)
        return shorterID(baseID)
    }

    override var parentProvider: NodeIdProvider?
        get() = null
        set(value) {
            throw UnsupportedOperationException()
        }

    fun flush() {
        bWriter.flush()
        bWriter.close()
        writer.close()
        fos.close()
    }

    private fun shorterID(originalNodeID: String) : String {
        if (!map.containsA(originalNodeID)) {
            return assignShorterID(originalNodeID)
        }
        return map.byA(originalNodeID)!!
    }

    private fun originalNodeID(shorterID: String) : String {
        if (!map.containsB(shorterID)) {
            throw IllegalStateException()
        }
        return map.byB(shorterID)!!
    }

    private fun assignShorterID(originalNodeID: String) : String {
        val shorterID = next.toString()
        map.associate(originalNodeID, shorterID)
        next++
        bWriter.write("${originalNodeID},${shorterID}\n")
        return shorterID
    }
}


class SequenceIDAssigner(val file: File) : NodeIdProvider {

    private var next = 1L
    private val cache = IdentityHashMap<KNode, String>()
    private val fos = FileOutputStream(file, true)
    private val writer = OutputStreamWriter(fos, Charsets.UTF_8)
    private val bWriter = BufferedWriter(writer)

    init {
        if (file.exists()) {
            file.readLines().filter { it.isNotBlank() }.forEach { line ->
                val n = line.toLong()
                next = next.coerceAtLeast(n + 1)
            }
        }
    }

    override fun id(kNode: Node): String {
        return cache.getOrPut(kNode) {
            assignNextID()
        }
    }

    override var parentProvider: NodeIdProvider?
        get() = null
        set(value) {
            throw UnsupportedOperationException()
        }

    fun flush() {
        bWriter.flush()
        bWriter.close()
        writer.close()
        fos.close()
    }

    private fun assignNextID() : String {
        val id = next.toString()
        next++
        bWriter.write("${id}\n")
        return id
    }
}