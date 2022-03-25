package com.strumenta.kolasu.model

import kotlin.test.assertEquals
import org.junit.Test as test

abstract class Expr : Node()
class Number(val value: Int) : Expr()
class Add(val left: Expr, val right: Expr) : Expr()
class Sub(private val left: Expr, private val right: Expr) : Expr()
class Empty(val empty: List<Expr>? = emptyList()) : Expr()

open class Expressions(
    val public: List<Expr> = emptyList(),
    private val private: List<Expr> = emptyList(),
    protected val protected: List<Expr> = emptyList(),
    internal val internal: List<Expr> = emptyList()
) : Expr()

class PrintingTest {
    @test
    fun basicTest() {
        val ast = Add(Add(Number(3), Number(9)), Number(1))
        assertEquals(
            """
                Add {
                  left = [
                    Add {
                      left = [
                        Number {
                          value = 3
                        } // Number
                      ]
                      right = [
                        Number {
                          value = 9
                        } // Number
                      ]
                    } // Add
                  ]
                  right = [
                    Number {
                      value = 1
                    } // Number
                  ]
                } // Add
                
            """.trimIndent(),
            ast.debugPrint()
        )
    }

    @test
    fun privateFieldsGetPrintedToo() {
        val ast = Add(Sub(Number(3), Number(9)), Number(1))
        assertEquals(
            """
                Add {
                  left = [
                    Sub {
                    } // Sub
                  ]
                  right = [
                    Number {
                      value = 1
                    } // Number
                  ]
                } // Add
            
            """.trimIndent(),
            ast.debugPrint()
        )
    }

    @test
    fun testDebugPrintSkippingAllProperties() {
        val ast = Expressions(
            internal = listOf(Number(1), Number(2)),
            private = listOf(Number(3), Number(4)),
            protected = listOf(Number(5), Number(6)),
            public = listOf(Number(7), Number(8))
        )
        assertEquals(
            """
                Expressions {
                } // Expressions
            
            """.trimIndent(),
            ast.debugPrint(
                configuration = DebugPrintConfiguration(
                    skipPublicProperties = true,
                    skipPrivateProperties = true,
                    skipProtectedProperties = true,
                    skipInternalProperties = true
                )
            )
        )
    }

    @test
    fun testDebugPrintSkippingPublicProperties() {
        val ast = Expressions(
            internal = listOf(Number(1), Number(2)),
            private = listOf(Number(3), Number(4)),
            protected = listOf(Number(5), Number(6)),
            public = listOf(Number(7), Number(8))
        )
        assertEquals(
            """
                Expressions {
                  internal = [
                    Number {
                    } // Number
                    Number {
                    } // Number
                  ]
                  private = [
                    Number {
                    } // Number
                    Number {
                    } // Number
                  ]
                  protected = [
                    Number {
                    } // Number
                    Number {
                    } // Number
                  ]
                } // Expressions
            
            """.trimIndent(),
            ast.debugPrint(
                configuration = DebugPrintConfiguration(
                    skipPublicProperties = true,
                    skipPrivateProperties = false,
                    skipProtectedProperties = false,
                    skipInternalProperties = false
                )
            )
        )
    }

    @test
    fun testDebugPrintSkippingPrivateProperties() {
        val ast = Expressions(
            internal = listOf(Number(1), Number(2)),
            private = listOf(Number(3), Number(4)),
            protected = listOf(Number(5), Number(6)),
            public = listOf(Number(7), Number(8))
        )
        assertEquals(
            """
                Expressions {
                  internal = [
                    Number {
                      value = 1
                    } // Number
                    Number {
                      value = 2
                    } // Number
                  ]
                  protected = [
                    Number {
                      value = 5
                    } // Number
                    Number {
                      value = 6
                    } // Number
                  ]
                  public = [
                    Number {
                      value = 7
                    } // Number
                    Number {
                      value = 8
                    } // Number
                  ]
                } // Expressions
            
            """.trimIndent(),
            ast.debugPrint(
                configuration = DebugPrintConfiguration(
                    skipPublicProperties = false,
                    skipPrivateProperties = true,
                    skipProtectedProperties = false,
                    skipInternalProperties = false
                )
            )
        )
    }

    @test
    fun testDebugPrintSkippingProtectedProperties() {
        val ast = Expressions(
            internal = listOf(Number(1), Number(2)),
            private = listOf(Number(3), Number(4)),
            protected = listOf(Number(5), Number(6)),
            public = listOf(Number(7), Number(8))
        )
        assertEquals(
            """
                Expressions {
                  internal = [
                    Number {
                      value = 1
                    } // Number
                    Number {
                      value = 2
                    } // Number
                  ]
                  private = [
                    Number {
                      value = 3
                    } // Number
                    Number {
                      value = 4
                    } // Number
                  ]
                  public = [
                    Number {
                      value = 7
                    } // Number
                    Number {
                      value = 8
                    } // Number
                  ]
                } // Expressions
            
            """.trimIndent(),
            ast.debugPrint(
                configuration = DebugPrintConfiguration(
                    skipPublicProperties = false,
                    skipPrivateProperties = false,
                    skipProtectedProperties = true,
                    skipInternalProperties = false
                )
            )
        )
    }

    @test
    fun testDebugPrintSkippingInternalProperties() {
        val ast = Expressions(
            internal = listOf(Number(1), Number(2)),
            private = listOf(Number(3), Number(4)),
            protected = listOf(Number(5), Number(6)),
            public = listOf(Number(7), Number(8))
        )
        assertEquals(
            """
                Expressions {
                  private = [
                    Number {
                      value = 3
                    } // Number
                    Number {
                      value = 4
                    } // Number
                  ]
                  protected = [
                    Number {
                      value = 5
                    } // Number
                    Number {
                      value = 6
                    } // Number
                  ]
                  public = [
                    Number {
                      value = 7
                    } // Number
                    Number {
                      value = 8
                    } // Number
                  ]
                } // Expressions
            
            """.trimIndent(),
            ast.debugPrint(
                configuration = DebugPrintConfiguration(
                    skipPublicProperties = false,
                    skipPrivateProperties = false,
                    skipProtectedProperties = false,
                    skipInternalProperties = true
                )
            )
        )
    }

    @test
    fun testDebugPrintSkippingNoProperties() {
        val ast = Expressions(
            internal = listOf(Number(1), Number(2)),
            private = listOf(Number(3), Number(4)),
            protected = listOf(Number(5), Number(6)),
            public = listOf(Number(7), Number(8))
        )
        assertEquals(
            """
                Expressions {
                  internal = [
                    Number {
                      value = 1
                    } // Number
                    Number {
                      value = 2
                    } // Number
                  ]
                  private = [
                    Number {
                      value = 3
                    } // Number
                    Number {
                      value = 4
                    } // Number
                  ]
                  protected = [
                    Number {
                      value = 5
                    } // Number
                    Number {
                      value = 6
                    } // Number
                  ]
                  public = [
                    Number {
                      value = 7
                    } // Number
                    Number {
                      value = 8
                    } // Number
                  ]
                } // Expressions
            
            """.trimIndent(),
            ast.debugPrint(
                configuration = DebugPrintConfiguration(
                    skipPublicProperties = false,
                    skipPrivateProperties = false,
                    skipProtectedProperties = false,
                    skipInternalProperties = false
                )
            )
        )
    }

    @test
    fun testDebugPrintSkippingAllExceptPublicPropertiesByDefault() {
        val ast = Expressions(
            internal = listOf(Number(1), Number(2)),
            private = listOf(Number(3), Number(4)),
            protected = listOf(Number(5), Number(6)),
            public = listOf(Number(7), Number(8))
        )
        assertEquals(
            """
                Expressions {
                  public = [
                    Number {
                      value = 7
                    } // Number
                    Number {
                      value = 8
                    } // Number
                  ]
                } // Expressions
            
            """.trimIndent(),
            ast.debugPrint()
        )
    }

    @test
    fun shortFormatForEmptyArrays() {
        val ast = Empty()
        assertEquals(
            """
              Empty {
                empty = []
              } // Empty
              
            """.trimIndent(),
            ast.debugPrint()
        )
    }
}
