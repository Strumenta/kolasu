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
        return if(rule) {
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
    }

    override fun visitErrorNode(node: ErrorNode?) {}

    override fun enterEveryRule(ctx: ParserRuleContext) {
        path = path.followWith(PathElement(ctx.ruleIndex, true))
        addUncoveredPaths()
        if(paths.containsKey(path)) {
            paths[path] = true
        }
    }

    protected fun addUncoveredPaths(state: Int = parser!!.state) {
        if(!states.contains(state)) {
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
                            for(i in interval.a .. interval.b) {
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

    private fun addUncoveredPath(transPath: Path) {
        if (!paths.containsKey(transPath)) {
            paths[transPath] = false
        }
    }

    override fun exitEveryRule(ctx: ParserRuleContext) {
        var last = path.elements.last()
        while (!last.rule || last.symbol != ctx.ruleIndex) {
            path = path.parent()
            last = path.elements.last()
        }
        addUncoveredPaths()
    }

    fun pathStrings(): List<String> {
        return paths.keys.map { it.toString(parser!!) }
    }

    fun uncoveredPaths(): Set<Path> {
        return paths.filterValues { !it }.keys
    }

    fun uncoveredPathStrings(): List<String> {
        return uncoveredPaths().map { it.toString(parser!!) }
    }

    fun listenTo(parser: Parser) {
        this.parser?.removeParseListener(this)
        this.parser = parser
        parser.addParseListener(this)
    }

    fun percentage(): Double {
        return 1.0 - (this.uncoveredPaths().size.toDouble() / this.paths.size.toDouble())
    }
}