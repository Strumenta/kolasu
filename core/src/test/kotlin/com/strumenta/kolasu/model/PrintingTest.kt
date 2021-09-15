package com.strumenta.kolasu.model

import kotlin.test.assertEquals
import org.junit.Test as test

abstract class Expr : Node()
class Number(val value: Int) : Expr()
class Add(val left: Expr, val right: Expr) : Expr()
class Sub(private val left: Expr, private val right: Expr) : Expr()

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
          nodeType = com.strumenta.kolasu.model.Number
          parseTreeNode = null
          specifiedPosition = null
        } // Number
      ]
      right = [
        Number {
          value = 9
          nodeType = com.strumenta.kolasu.model.Number
          parseTreeNode = null
          specifiedPosition = null
        } // Number
      ]
      nodeType = com.strumenta.kolasu.model.Add
      parseTreeNode = null
      specifiedPosition = null
    } // Add
  ]
  right = [
    Number {
      value = 1
      nodeType = com.strumenta.kolasu.model.Number
      parseTreeNode = null
      specifiedPosition = null
    } // Number
  ]
  nodeType = com.strumenta.kolasu.model.Add
  parseTreeNode = null
  specifiedPosition = null
} // Add
""",
            ast.debugPrint()
        )
    }

    @test
    fun privateFieldsGetPrintedToo() {
        val ast = Add(Sub(Number(3), Number(9)), Number(1))
        assertEquals(
            """Add {
  left = [
    Sub {
      nodeType = com.strumenta.kolasu.model.Sub
      parseTreeNode = null
      specifiedPosition = null
    } // Sub
  ]
  right = [
    Number {
      value = 1
      nodeType = com.strumenta.kolasu.model.Number
      parseTreeNode = null
      specifiedPosition = null
    } // Number
  ]
  nodeType = com.strumenta.kolasu.model.Add
  parseTreeNode = null
  specifiedPosition = null
} // Add
""",
            ast.debugPrint()
        )
    }
}
