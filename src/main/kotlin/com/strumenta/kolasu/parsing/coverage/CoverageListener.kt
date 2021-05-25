package com.strumenta.kolasu.parsing.coverage

import com.strumenta.kolasu.model.mutableStackOf
import org.antlr.v4.runtime.Parser
import org.antlr.v4.runtime.ParserRuleContext
import org.antlr.v4.runtime.atn.AtomTransition
import org.antlr.v4.runtime.atn.EpsilonTransition
import org.antlr.v4.runtime.atn.RuleTransition
import org.antlr.v4.runtime.atn.SetTransition
import org.antlr.v4.runtime.tree.ErrorNode
import org.antlr.v4.runtime.tree.ParseTreeListener
import org.antlr.v4.runtime.tree.TerminalNode

data class PathElement(val symbol: Int, val rule: Boolean) {
    fun toString(parser: Parser): String {
        return if (rule) {
            parser.ruleNames[symbol]
        } else {
            parser.vocabulary.getSymbolicName(symbol)
        }
    }
}

data class Path(val elements: List<PathElement> = mutableListOf()) {
    fun followWith(el: PathElement): Path {
        return Path(elements + el)
    }

    fun parent(): Path {
        return Path(elements.subList(0, elements.size - 1))
    }

    fun toString(parser: Parser): String {
        return elements.map { it.toString(parser) }.joinToString(" > ")
    }
}

open class CoverageListener(var parser: Parser? = null) : ParseTreeListener {

    val paths = mutableMapOf<Path, Boolean>()
    protected var path: Path = Path()
    protected val states = mutableStackOf<Int>()

    override fun visitTerminal(node: TerminalNode) {
        addUncoveredPaths()
        path = path.followWith(PathElement(node.symbol.type, false))
        paths[path] = true
        var remainder = Path()
        for(i in (0 until path.elements.size).reversed()) {
            remainder = Path(listOf(path.elements[i]) + remainder.elements)
            if(path.elements[i].rule) {
                paths[remainder] = true
            }
        }
        while(remainder.elements.size > 1) {
            remainder = Path(remainder.elements.subList(0, remainder.elements.size - 1))
            paths[remainder] = true
        }
    }

    override fun visitErrorNode(node: ErrorNode?) {}

    override fun enterEveryRule(ctx: ParserRuleContext) {
        val el = PathElement(ctx.ruleIndex, true)
        val newPath = path.followWith(el)
        path = Path(mutableListOf(el))
        addUncoveredPaths()
        paths[path] = true
        path = newPath
        paths[path] = true
    }

    protected fun addUncoveredPaths(state: Int = parser!!.state) {
        if (!states.contains(state)) {
            states.push(state)
            parser!!.atn.states[state].transitions.forEach {
                when (it) {
                    is RuleTransition -> {
                        addUncoveredPath(path.followWith(PathElement(it.ruleIndex, true)))
                    }
                    is AtomTransition -> {
                        addUncoveredPath(path.followWith(PathElement(it.label, false)))
                    }
                    is SetTransition -> {
                        it.set.intervals.forEach { interval ->
                            for (i in interval.a..interval.b) {
                                addUncoveredPath(path.followWith(PathElement(i, false)))
                            }
                        }
                    }
                    is EpsilonTransition -> {
                        addUncoveredPaths(it.target.stateNumber)
                    }
                }
            }
            states.pop()
        }
    }

    private fun addUncoveredPath(path: Path) {
        if (!paths.containsKey(path)) {
            paths[path] = false
        }
    }

    override fun exitEveryRule(ctx: ParserRuleContext) {
        var last = path.elements.last()
        var remainder = Path(mutableListOf(last))
        if(last.rule) {
            paths[remainder] = true
        }
        while (!last.rule || last.symbol != ctx.ruleIndex) {
            path = path.parent()
            paths[path] = true
            last = path.elements.last()

            remainder = Path(listOf(last) + remainder.elements)
            if(last.rule) {
                paths[remainder] = true
            }
        }
        while(remainder.elements.size > 1) {
            remainder = Path(remainder.elements.subList(0, remainder.elements.size - 1))
            paths[remainder] = true
        }
        addUncoveredPaths()
        path = path.parent()
    }

    fun pathStrings(): List<String> {
        return paths.keys.map { it.toString(parser!!) }
    }

    fun uncoveredPaths(maxLength: Int = Int.MAX_VALUE): Collection<Path> {
        return paths.filterValues { !it }.keys.filter { it.elements.size <= maxLength }
    }

    fun uncoveredPathStrings(maxLength: Int = Int.MAX_VALUE): Collection<String> {
        return uncoveredPaths(maxLength).map { it.toString(parser!!) }
    }

    fun listenTo(parser: Parser) {
        this.parser?.removeParseListener(this)
        this.parser = parser
        parser.addParseListener(this)
    }

    fun percentage() = (1.0 - (this.uncoveredPaths().size.toDouble() / this.paths.size.toDouble())) * 100.0
}
