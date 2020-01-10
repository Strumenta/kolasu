package com.strumenta.kolasu.model

import kotlin.test.assertEquals
import org.junit.Test as test

abstract class Expr : Node()
class Number(val value: Int) : Expr()
class Add(val left: Expr, val right: Expr) : Expr()

class PrintingTest {
    @test
    fun basicTest() {
        val ast = Add(Add(Number(3), Number(9)), Number(1))
        assertEquals(
"""Add {
  left = [
    Add {
      left = [
        Number {
          value = 3
          parseTreeNode = null
          specifiedPosition = null
        } // Number
      ]
      right = [
        Number {
          value = 9
          parseTreeNode = null
          specifiedPosition = null
        } // Number
      ]
      parseTreeNode = null
      specifiedPosition = null
    } // Add
  ]
  right = [
    Number {
      value = 1
      parseTreeNode = null
      specifiedPosition = null
    } // Number
  ]
  parseTreeNode = null
  specifiedPosition = null
} // Add
""",
            ast.multilineString()
        )
    }
}
