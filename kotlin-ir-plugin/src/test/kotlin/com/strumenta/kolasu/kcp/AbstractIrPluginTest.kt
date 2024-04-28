package com.strumenta.kolasu.kcp

abstract class AbstractIrPluginTest {
    protected fun CompilationResultWithClassLoader.invokeMainMethod(className: String) {
        val mainKt = this.classLoader.loadClass(className)
        val mainMethod =
            mainKt.methods.find { it.name == "main" }
                ?: throw IllegalArgumentException("Main method not found in compiled code")
        when (mainMethod.parameterCount) {
            0 -> {
                mainMethod.invoke(null)
            }

            1 -> {
                mainMethod.invoke(null, arrayOf<String>())
            }

            else -> {
                throw IllegalStateException(
                    "The main method found expect these parameters: ${mainMethod.parameters}. " +
                        "Main method: $mainMethod",
                )
            }
        }
    }
}
