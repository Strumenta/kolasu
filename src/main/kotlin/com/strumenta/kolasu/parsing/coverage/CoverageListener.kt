package com.strumenta.kolasu.parsing.coverage

import org.antlr.v4.runtime.Parser
import org.antlr.v4.runtime.ParserRuleContext
import org.antlr.v4.runtime.atn.AtomTransition
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

data class Path(val elements: List<PathElement> = listOf(), val states: MutableSet<Int> = mutableSetOf()) {
    fun followWith(el: PathElement): Path {
        return Path(elements + el, states = this.states.toMutableSet())
    }

    fun parent(): Path {
        return Path(elements.subList(0, elements.size - 1))
    }

    fun toString(parser: Parser): String {
        return elements.joinToString(" > ") { it.toString(parser) }
    }

    override fun equals(other: Any?): Boolean {
        return other is Path && elements == other.elements
    }

    override fun hashCode(): Int {
        return elements.hashCode()
    }
}

open class CoverageListener(var parser: Parser? = null) : ParseTreeListener {

    val paths = mutableMapOf<Path, Boolean>()
    var path: Path = Path()

    override fun visitTerminal(node: TerminalNode) {
        path = path.followWith(PathElement(node.symbol.type, false))
        paths[path] = true
    }

    override fun visitErrorNode(node: ErrorNode?) {}

    override fun enterEveryRule(ctx: ParserRuleContext) {
        addUncoveredPaths(ctx.invokingState)
        if (!isLeftRecursive(ctx.ruleIndex)) {
            val el = PathElement(ctx.ruleIndex, true)
            path = path.followWith(el)
            paths[path] = true
        }
        addUncoveredPaths()
    }

    private fun isLeftRecursive(ruleIndex: Int): Boolean {
        val leftRec = if (path.elements.isNotEmpty()) {
            val last = path.elements.last()
            last.rule && last.symbol == ruleIndex
        } else false
        return leftRec
    }

    protected fun addUncoveredPaths(state: Int = parser!!.state) {
        if (path.states.contains(state) || state < 0) {
            return
        }
        path.states.add(state)
        parser!!.atn.states[state].transitions.forEach {
            when (it) {
                is RuleTransition -> {
                    if (!isLeftRecursive(it.ruleIndex)) {
                        addUncoveredPath(path.followWith(PathElement(it.ruleIndex, true)))
                    }
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
                else -> {
                    addUncoveredPaths(it.target.stateNumber)
                }
            }
        }
    }

    private fun addUncoveredPath(path: Path) {
        if (!paths.containsKey(path)) {
            paths[path] = false
        }
    }

    override fun exitEveryRule(ctx: ParserRuleContext) {
        while (path.elements.isNotEmpty() && (!path.elements.last().rule || path.elements.last().symbol != ctx.ruleIndex)) {
            path = Path(path.elements.subList(0, path.elements.size - 1), path.states)
        }
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
