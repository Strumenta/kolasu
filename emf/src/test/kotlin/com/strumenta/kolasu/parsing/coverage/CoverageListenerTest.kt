package com.strumenta.kolasu.parsing.coverage

import com.strumenta.simplelang.SimpleLangLexer
import com.strumenta.simplelang.SimpleLangParser
import kotlin.math.abs
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CommonTokenStream
import org.junit.Test

class CoverageListenerTest {

    @Test
    fun initiallyEmpty() {
        val coverage = CoverageListener()
        assertTrue { coverage.uncoveredPaths().isEmpty() }
        assertTrue { coverage.paths.isEmpty() }
    }

    @Test
    fun coverageOfSimpleRule() {
        val code = "int"
        val lexer = SimpleLangLexer(CharStreams.fromString(code))
        val parser = SimpleLangParser(CommonTokenStream(lexer))
        val coverage = CoverageListener()
        coverage.listenTo(parser)
        parser.type()
        assertTrue {
            coverage.uncoveredPathStrings().containsAll(listOf("type > DEC", "type > STRING", "type > BOOLEAN"))
        }
        assertCoverage(coverage, 3, 5)
    }

    private fun assertCoverage(coverage: CoverageListener, expectedUncovered: Int, expectedPaths: Int) {
        assertEquals(expectedUncovered, coverage.uncoveredPaths().size)
        assertEquals(expectedPaths, coverage.paths.size)
        assertTrue { abs((expectedPaths.toDouble() - expectedUncovered.toDouble()) / expectedPaths.toDouble() - coverage.percentage() / 100.0) < .00001 }
    }

    @Test
    fun coverageOfPlusBlock1() {
        val code = "set foo = 123"
        val lexer = SimpleLangLexer(CharStreams.fromString(code))
        val parser = SimpleLangParser(CommonTokenStream(lexer))
        val coverage = CoverageListener()
        coverage.listenTo(parser)
        parser.compilationUnit()
        assertTrue {
            coverage.uncoveredPathStrings().containsAll(
                listOf(
                    "statement > DISPLAY",
                    "statement > INPUT",
                    "expression > DEC_LIT",
                    "expression > STRING_LIT",
                    "expression > BOOLEAN_LIT"
                )
            )
        }
        assertCoverage(coverage, 5, 14)
    }

    @Test
    fun coverageOfPlusBlock2() {
        val code = "set foo = 123 set bar = 1.23 display 12.3"
        val lexer = SimpleLangLexer(CharStreams.fromString(code))
        val parser = SimpleLangParser(CommonTokenStream(lexer))
        val coverage = CoverageListener()
        coverage.listenTo(parser)
        parser.compilationUnit()
        assertTrue {
            coverage.uncoveredPathStrings().containsAll(
                listOf(
                    "statement > INPUT",
                    "expression > STRING_LIT",
                    "expression > BOOLEAN_LIT"
                )
            )
        }
        assertCoverage(coverage, 3, 15)
    }

    @Test
    fun coverageOfLeftRecursion1() {
        val code = "set foo = 123 + 124 + 125"
        val lexer = SimpleLangLexer(CharStreams.fromString(code))
        val parser = SimpleLangParser(CommonTokenStream(lexer))
        val coverage = CoverageListener()
        coverage.listenTo(parser)
        parser.compilationUnit()
        assertTrue {
            coverage.uncoveredPathStrings().containsAll(
                listOf(
                    "statement > DISPLAY",
                    "statement > INPUT",
                    "expression > DEC_LIT",
                    "expression > STRING_LIT",
                    "expression > BOOLEAN_LIT",
                    "expression > MINUS",
                    "expression > MULT",
                    "expression > DIV"
                )
            )
        }
        assertCoverage(coverage, 8, 19)
    }

    @Test
    fun coverageOfLeftRecursion2() {
        val code = "set foo = 123 + 124 + 125 set bar = 123 + 12.4 - 1.25"
        val lexer = SimpleLangLexer(CharStreams.fromString(code))
        val parser = SimpleLangParser(CommonTokenStream(lexer))
        val coverage = CoverageListener()
        coverage.listenTo(parser)
        parser.compilationUnit()
        assertTrue {
            coverage.uncoveredPathStrings().containsAll(
                listOf(
                    "statement > DISPLAY",
                    "statement > INPUT",
                    "expression > STRING_LIT",
                    "expression > BOOLEAN_LIT",
                    "expression > MULT",
                    "expression > DIV"
                )
            )
        }
        assertCoverage(coverage, 6, 20)
    }
}
