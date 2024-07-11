package com.strumenta.kolasu.lionwebclient

import java.io.File

object PerformanceLogger {
    var file = File("performance-log.csv")

    private fun ensureFileIsReady() {
        if (!file.exists()) {
            file.appendText("event,time\n")
        }
    }

    fun log(description: String, baseTime: Long? = null) : Long {
        ensureFileIsReady()
        val t = System.currentTimeMillis()
        if (baseTime == null) {
            file.appendText("\"${description}\",${t},\n")
        } else {
            file.appendText("\"${description}\",${t},${t-baseTime}\n")
        }
        return t
    }

    fun log(description: String, operation: () -> Unit) {
        val t0 = log("$description - start")
        try {
            operation()
        } finally {
            log("$description - end", t0)
        }
    }
}