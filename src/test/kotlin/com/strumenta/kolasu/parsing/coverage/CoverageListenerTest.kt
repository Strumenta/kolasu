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
                    "compilationUnit > statement > DISPLAY",
                    "compilationUnit > statement > INPUT",
                    "compilationUnit > statement > SET > ID > EQUAL > expression > DEC_LIT",
                    "compilationUnit > statement > SET > ID > EQUAL > expression > STRING_LIT",
                    "compilationUnit > statement > SET > ID > EQUAL > expression > BOOLEAN_LIT"
                )
            )
        }
        assertCoverage(coverage, 5, 12)
    }

    @Test
    fun coverageOfPlusBlock2() {
        val code = "set foo = 123 set bar = 1.23 display foo"
        val lexer = SimpleLangLexer(CharStreams.fromString(code))
        val parser = SimpleLangParser(CommonTokenStream(lexer))
        val coverage = CoverageListener()
        coverage.listenTo(parser)
        parser.compilationUnit()
        assertTrue {
            coverage.uncoveredPathStrings().containsAll(
                listOf(
                    "compilationUnit > statement > INPUT",
                    "compilationUnit > statement > SET > ID > EQUAL > expression > STRING_LIT",
                    "compilationUnit > statement > SET > ID > EQUAL > expression > BOOLEAN_LIT"
                )
            )
        }
        assertCoverage(coverage, 5, 12)
    }

    @Test
    fun coverageOfLeftRecursion() {
        val code = "set foo = 123 + 124 + 125"
        val lexer = SimpleLangLexer(CharStreams.fromString(code))
        val parser = SimpleLangParser(CommonTokenStream(lexer))
        val coverage = CoverageListener()
        coverage.listenTo(parser)
        parser.compilationUnit()
        assertTrue {
            coverage.uncoveredPathStrings().containsAll(
                listOf(
                    "compilationUnit > statement > DISPLAY",
                    "compilationUnit > statement > INPUT",
                    "compilationUnit > statement > SET > ID > EQUAL > expression > DEC_LIT",
                    "compilationUnit > statement > SET > ID > EQUAL > expression > STRING_LIT",
                    "compilationUnit > statement > SET > ID > EQUAL > expression > BOOLEAN_LIT",
                    "compilationUnit > statement > SET > ID > EQUAL > expression > MINUS",
                    "compilationUnit > statement > SET > ID > EQUAL > expression > MULT",
                    "compilationUnit > statement > SET > ID > EQUAL > expression > DIV"
                )
            )
        }
        assertCoverage(coverage, 8, 21)
    }
}
