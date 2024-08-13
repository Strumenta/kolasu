package com.strumenta.kolasu.lionweb

import java.io.File

object PerformanceLogger {
    var file = File("performance-log.csv")
    val sumByStep = mutableMapOf<String, Long>()

    fun sumByStep(description: String): Long {
        return sumByStep.getOrDefault(description, 0)
    }

    fun printAllSums() {
        println("== Performance logger sums ===")
        sumByStep.forEach {
            println("${it.key}: ${it.value}")
        }
    }

    private fun ensureFileIsReady() {
        if (!file.exists()) {
            file.appendText("event,time\n")
        }
    }

<<<<<<<< HEAD:lionwebrepo-client/src/main/kotlin/com/strumenta/kolasu/lionwebclient/PerformanceLogger.kt
    fun log(
        description: String,
        baseTime: Long? = null,
    ): Long {
========
    fun log(description: String, baseTime: Long? = null): Long {
>>>>>>>> maintenance/kolasu15:lionweb/src/main/kotlin/com/strumenta/kolasu/lionweb/PerformanceLogger.kt
        ensureFileIsReady()
        val t = System.currentTimeMillis()
        if (baseTime == null) {
            file.appendText("\"${description}\",$t,\n")
        } else {
            val delta = t - baseTime
            file.appendText("\"${description}\",$t,${delta}\n")
            val baseDescription = description.removeSuffix(" - end")
            sumByStep[baseDescription] = sumByStep.getOrDefault(baseDescription, 0) + delta
        }
        return t
    }

    fun log(
        description: String,
        operation: () -> Unit,
    ) {
        val t0 = log("$description - start")
        try {
            operation()
        } finally {
            log("$description - end", t0)
        }
    }
}
