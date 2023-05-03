package com.smeup.rpgparser.parsing.ast

import com.strumenta.kolasu.emf.rpgast.*
import com.strumenta.kolasu.model.*
import com.strumenta.kolasu.traversing.findAncestorOfType

class DBFile
class Record
class Result

interface StatementThatCanDefineData {
    fun dataDefinition(): List<InStatementDataDefinition>
}

enum class AssignmentOperator(val text: String) {
    NORMAL_ASSIGNMENT("="),
    PLUS_ASSIGNMENT("+="),
    MINUS_ASSIGNMENT("-="),
    MULT_ASSIGNMENT("*="),
    DIVIDE_ASSIGNMENT("/="),
    EXP_ASSIGNMENT("**=");
}

abstract class Statement(specifiedRange: Range? = null) : ASTNode(specifiedRange) {
    open fun simpleDescription() = "Issue executing ${javaClass.simpleName} at line ${startLine()}."
}

interface CompositeStatement {
    val body: List<Statement>
}

fun List<Statement>.explode(): List<Statement> {
    val result = mutableListOf<Statement>()
    forEach {
        if (it is CompositeStatement) {
            result.addAll(it.body.explode())
        } else {
            result.add(it)
        }
    }
    return result
}

data class ExecuteSubroutine(
    var subroutine: ReferenceByName<Subroutine>,
    val specifiedRange: Range? = null
) : Statement(specifiedRange)

data class SelectStmt(
    var cases: List<SelectCase>,
    var other: SelectOtherClause? = null,
    val specifiedRange: Range? = null
) : Statement(specifiedRange), CompositeStatement {

    override val body: List<Statement>
        get() {
            val result = mutableListOf<Statement>()
            cases.forEach { case ->
                result.addAll(case.body.explode())
            }
            if (other?.body != null) result.addAll(other!!.body.explode())
            return result
        }
}

data class SelectOtherClause(override val body: List<Statement>, val specifiedRange: Range? = null) :
    ASTNode(
        specifiedRange
    ),
    CompositeStatement

data class SelectCase(
    val condition: Expression,
    override val body: List<Statement>,
    val specifiedRange: Range? = null
) : ASTNode(specifiedRange), CompositeStatement

data class EvalFlags(
    val halfAdjust: Boolean = false,
    val maximumNumberOfDigitsRule: Boolean = false,
    val resultDecimalPositionRule: Boolean = false
)

data class EvalStmt(
    val target: AssignableExpression,
    var expression: Expression,
    val operator: AssignmentOperator = AssignmentOperator.NORMAL_ASSIGNMENT,
    val flags: EvalFlags = EvalFlags(),
    val specifiedRange: Range? = null
) :
    Statement(specifiedRange)

data class SubDurStmt(
    val factor1: Expression?,
    val target: AssignableExpression,
    val factor2: Expression,
    val durationCode: DurationCode,
    val specifiedRange: Range? = null
) :
    Statement(specifiedRange)

data class MoveStmt(
    val target: AssignableExpression,
    var expression: Expression,
    val specifiedRange: Range? = null
) :
    Statement(specifiedRange)

data class MoveAStmt(
    val operationExtender: String?,
    val target: AssignableExpression,
    var expression: Expression,
    val specifiedRange: Range? = null
) :
    Statement(specifiedRange)

data class MoveLStmt(
    val operationExtender: String?,
    val target: AssignableExpression,
    @Derived val dataDefinition: InStatementDataDefinition? = null,
    var expression: Expression,
    val specifiedRange: Range? = null
) : Statement(specifiedRange), StatementThatCanDefineData {
    override fun dataDefinition(): List<InStatementDataDefinition> {
        if (dataDefinition != null) {
            return listOf(dataDefinition)
        }
        return emptyList()
    }
}

abstract class AbstractReadEqualStmt(
    @Transient open val searchArg: Expression? = null, // Factor1
    @Transient open val name: String = "", // Factor 2
    specifiedRange: Range? = null,
    private val logPref: String

) : Statement(specifiedRange) {

    abstract fun read(dbFile: DBFile, kList: List<String>? = null): Result
}

abstract class AbstractReadStmt(
    @Transient open val name: String = "", // Factor 2
    specifiedRange: Range? = null,
    private val logPref: String
) : Statement(specifiedRange) {

    abstract fun readOp(dbFile: DBFile): Result
}

abstract class AbstractStoreStmt(
    @Transient open val name: String = "", // Factor 2
    specifiedRange: Range? = null,
    private val logPref: String
) : Statement(specifiedRange) {

    abstract fun store(dbFile: DBFile, record: Record): Result
}

abstract class AbstractSetStmt(
    // this one is a dummy expression needed to initialize because of "transient" annotation
    @Transient open val searchArg: Expression = StringLiteral(""), // Factor1
    @Transient open val name: String = "", // Factor 2
    specifiedRange: Range? = null,
    private val logPref: String = ""
) : Statement(specifiedRange) {

    abstract fun set(dbFile: DBFile, kList: List<String>): Boolean
}

// TODO add other parameters

data class ChainStmt(
    override val searchArg: Expression, // Factor1
    override val name: String, // Factor 2
    val specifiedRange: Range? = null
) : AbstractReadEqualStmt(searchArg, name, specifiedRange, "CHAIN") {
    override fun read(dbFile: DBFile, kList: List<String>?): Result = Result()
}

data class ReadEqualStmt(
    override val searchArg: Expression?,
    override val name: String,
    val specifiedRange: Range? = null
) : AbstractReadEqualStmt(
    searchArg = searchArg, name = name, specifiedRange = specifiedRange,
    logPref = "READE"
) {

    override fun read(dbFile: DBFile, kList: List<String>?): Result = Result()
}

data class ReadPreviousEqualStmt(
    override val searchArg: Expression?,
    override val name: String,
    val specifiedRange: Range? = null
) : AbstractReadEqualStmt(
    searchArg = searchArg, name = name, specifiedRange = specifiedRange,
    logPref = "READPE"
) {

    override fun read(dbFile: DBFile, kList: List<String>?): Result = Result()
}

data class ReadStmt(override val name: String, val specifiedRange: Range?) : AbstractReadStmt(
    name,
    specifiedRange,
    "READ"
) {
    override fun readOp(dbFile: DBFile) = Result()
}

data class ReadPreviousStmt(override val name: String, val specifiedRange: Range?) : AbstractReadStmt(
    name,
    specifiedRange,
    "READP"
) {
    override fun readOp(dbFile: DBFile) = Result()
}

data class WriteStmt(override val name: String, val specifiedRange: Range?) : AbstractStoreStmt(
    name = name,
    specifiedRange = specifiedRange,
    logPref = "WRITE"
) {
    override fun store(dbFile: DBFile, record: Record) = Result()
}

data class UpdateStmt(override val name: String, val specifiedRange: Range?) : AbstractStoreStmt(
    name = name,
    specifiedRange = specifiedRange,
    logPref = "UPDATE"
) {
    override fun store(dbFile: DBFile, record: Record) = Result()
}

data class DeleteStmt(override val name: String, val specifiedRange: Range?) : AbstractStoreStmt(
    name = name,
    specifiedRange = specifiedRange,
    logPref = "DELETE"
) {
    override fun store(dbFile: DBFile, record: Record) = Result()
}

data class SetllStmt(
    override val searchArg: Expression,
    override val name: String,
    val specifiedRange: Range?
) : AbstractSetStmt(
    searchArg = searchArg,
    name = name,
    specifiedRange = specifiedRange,
    logPref = "SETLL"
) {
    override fun set(dbFile: DBFile, kList: List<String>) = true
}

data class SetgtStmt(
    override val searchArg: Expression,
    override val name: String,
    val specifiedRange: Range?
) : AbstractSetStmt(searchArg = searchArg, name = name, specifiedRange = specifiedRange, logPref = "SETGT") {

    override fun set(dbFile: DBFile, kList: List<String>) = true
}

data class CheckStmt(
    val comparatorString: Expression, // Factor1
    val baseString: Expression,
    val start: Int = 1,
    val wrongCharPosition: AssignableExpression?,
    val specifiedRange: Range? = null
) : Statement(specifiedRange)

data class CallStmt(
    val expression: Expression,
    val params: List<PlistParam>,
    val errorIndicator: IndicatorKey? = null,
    val specifiedRange: Range? = null
) : Statement(specifiedRange), StatementThatCanDefineData {
    override fun dataDefinition(): List<InStatementDataDefinition> {
        return params.mapNotNull() {
            it.dataDefinition
        }
    }
}

data class KListStmt(val name: String, val fields: List<String>, val specifiedRange: Range?) :
    Statement(
        specifiedRange
    ),
    StatementThatCanDefineData {
    companion object {
        operator fun invoke(name: String, fields: List<String>, specifiedRange: Range? = null): KListStmt {
            return KListStmt(name.uppercase(), fields, specifiedRange)
        }
    }

    override fun dataDefinition(): List<InStatementDataDefinition> = listOf(InStatementDataDefinition(name, KListType))
}

data class IfStmt(
    val condition: Expression,
    override val body: List<Statement>,
    val elseIfClauses: List<ElseIfClause> = emptyList(),
    val elseClause: ElseClause? = null,
    val specifiedRange: Range? = null
) : Statement(specifiedRange), CompositeStatement

data class ElseClause(override val body: List<Statement>, val specifiedRange: Range? = null) :
    ASTNode(
        specifiedRange
    ),
    CompositeStatement

data class ElseIfClause(
    val condition: Expression,
    override val body: List<Statement>,
    val specifiedRange: Range? = null
) : ASTNode(specifiedRange), CompositeStatement

data class SetStmt(
    val valueSet: ValueSet,
    val indicators: List<AssignableExpression>,
    val specifiedRange: Range? = null
) : Statement(specifiedRange) {
    enum class ValueSet {
        ON,
        OFF
    }
}

data class ReturnStmt(val expression: Expression?, val specifiedRange: Range? = null) : Statement(
    specifiedRange
)

// A Plist is a list of parameters

data class PlistStmt(
    val params: List<PlistParam>,
    val isEntry: Boolean,
    val specifiedRange: Range? = null
) : Statement(specifiedRange), StatementThatCanDefineData {
    override fun dataDefinition(): List<InStatementDataDefinition> {
        val allDataDefinitions = params.mapNotNull { it.dataDefinition }
        // We do not want params in plist to shadow existing data definitions
        // They are implicit data definitions only when explicit data definitions are not present
        val filtered = allDataDefinitions.filter { paramDataDef ->
            val containingCU = this.findAncestorOfType(CompilationUnit::class.java)
                ?: throw IllegalStateException("Not contained in a CU")
            containingCU.dataDefinitions.none { it.name == paramDataDef.name }
        }
        return filtered
    }
}

data class PlistParam(
    val param: ReferenceByName<AbstractDataDefinition>,
    @Derived val dataDefinition: InStatementDataDefinition? = null,
    val specifiedRange: Range? = null
) : ASTNode(specifiedRange)

data class ClearStmt(
    val value: Expression,
    @Derived val dataDefinition: InStatementDataDefinition? = null,
    val specifiedRange: Range? = null
) : Statement(specifiedRange), StatementThatCanDefineData {
    override fun dataDefinition(): List<InStatementDataDefinition> {
        if (dataDefinition != null) {
            return listOf(dataDefinition)
        }
        return emptyList()
    }
}

data class DefineStmt(
    val originalName: String,
    val newVarName: String,
    val specifiedRange: Range? = null
) : Statement(specifiedRange), StatementThatCanDefineData {
    override fun dataDefinition(): List<InStatementDataDefinition> {
        val containingCU = this.findAncestorOfType(CompilationUnit::class.java)
            ?: return emptyList()

        var originalDataDefinition = containingCU.dataDefinitions.find { it.name == originalName }
        // If definition was not found as a 'standalone' 'D spec' declaration,
        // maybe it can be found as a sub-field of DS in 'D specs' declarations
        containingCU.dataDefinitions.forEach {
            it.fields.forEach {
                if (it.name == originalName) {
                    return listOf(InStatementDataDefinition(newVarName, it.type, specifiedRange))
                }
            }
        }

        if (originalDataDefinition != null) {
            return listOf(InStatementDataDefinition(newVarName, originalDataDefinition.type, specifiedRange))
        } else {
            val inStatementDataDefinition =
                containingCU.main.stmts
                    .filterIsInstance(StatementThatCanDefineData::class.java)
                    .filter { it != this }
                    .asSequence()
                    .map(StatementThatCanDefineData::dataDefinition)
                    .flatten()
                    .find { it.name == originalName } ?: return emptyList()

            return listOf(InStatementDataDefinition(newVarName, inStatementDataDefinition.type, specifiedRange))
        }
    }
}

interface WithRightIndicators {
    fun allPresent(): Boolean = hi != null && lo != null && eq != null

    val hi: IndicatorKey?
    val lo: IndicatorKey?
    val eq: IndicatorKey?
}

data class RightIndicators(
    override val hi: IndicatorKey?,
    override val lo: IndicatorKey?,
    override val eq: IndicatorKey?
) : WithRightIndicators

data class CompStmt(
    val left: Expression,
    val right: Expression,
    val rightIndicators: WithRightIndicators,
    val specifiedRange: Range? = null
) : Statement(specifiedRange), WithRightIndicators by rightIndicators

data class ZAddStmt(
    val target: AssignableExpression,
    @Derived val dataDefinition: InStatementDataDefinition? = null,
    var expression: Expression,
    val specifiedRange: Range? = null
) :
    Statement(specifiedRange), StatementThatCanDefineData {
    override fun dataDefinition(): List<InStatementDataDefinition> {
        if (dataDefinition != null) {
            return listOf(dataDefinition)
        }
        return emptyList()
    }
}

data class MultStmt(
    val target: AssignableExpression,
    val halfAdjust: Boolean = false,
    val factor1: Expression?,
    val factor2: Expression,
    val specifiedRange: Range? = null
) : Statement(specifiedRange)

data class DivStmt(
    val target: AssignableExpression,
    val halfAdjust: Boolean = false,
    val factor1: Expression?,
    val factor2: Expression,
    val specifiedRange: Range? = null
) : Statement(specifiedRange)

data class AddStmt(
    val left: Expression?,
    val result: AssignableExpression,
    @Derived val dataDefinition: InStatementDataDefinition? = null,
    val right: Expression,
    val specifiedRange: Range? = null
) : Statement(specifiedRange), StatementThatCanDefineData {
    override fun dataDefinition(): List<InStatementDataDefinition> {
        if (dataDefinition != null) {
            return listOf(dataDefinition)
        }
        return emptyList()
    }

    @Derived
    val addend1: Expression
        get() = left ?: result
}

data class ZSubStmt(
    val target: AssignableExpression,
    @Derived val dataDefinition: InStatementDataDefinition? = null,
    var expression: Expression,
    val specifiedRange: Range? = null
) : Statement(specifiedRange), StatementThatCanDefineData {
    override fun dataDefinition(): List<InStatementDataDefinition> {
        if (dataDefinition != null) {
            return listOf(dataDefinition)
        }
        return emptyList()
    }
}

data class SubStmt(
    val left: Expression?,
    val result: AssignableExpression,
    @Derived val dataDefinition: InStatementDataDefinition? = null,
    val right: Expression,
    val specifiedRange: Range? = null
) : Statement(specifiedRange), StatementThatCanDefineData {
    override fun dataDefinition(): List<InStatementDataDefinition> {
        if (dataDefinition != null) {
            return listOf(dataDefinition)
        }
        return emptyList()
    }

    @Derived
    val minuend: Expression
        get() = left ?: result
}

data class TimeStmt(
    val value: Expression,
    val specifiedRange: Range? = null
) : Statement(specifiedRange)

data class DisplayStmt(
    val factor1: Expression?,
    val response: Expression?,
    val specifiedRange: Range? = null
) : Statement(specifiedRange)

data class DoStmt(
    val endLimit: Expression,
    val index: AssignableExpression?,
    override val body: List<Statement>,
    val startLimit: Expression = IntLiteral(1),
    val specifiedRange: Range? = null
) : Statement(specifiedRange), CompositeStatement

data class DowStmt(
    val endExpression: Expression,
    override val body: List<Statement>,
    val specifiedRange: Range? = null
) : Statement(specifiedRange), CompositeStatement

data class DouStmt(
    val endExpression: Expression,
    override val body: List<Statement>,
    val specifiedRange: Range? = null
) : Statement(specifiedRange), CompositeStatement

data class LeaveSrStmt(val specifiedRange: Range? = null) : Statement(specifiedRange)

data class LeaveStmt(val specifiedRange: Range? = null) : Statement(specifiedRange)

data class IterStmt(val specifiedRange: Range? = null) : Statement(specifiedRange)

data class OtherStmt(val specifiedRange: Range? = null) : Statement(specifiedRange)

data class TagStmt constructor(val tag: String, val specifiedRange: Range? = null) : Statement(
    specifiedRange
)

data class GotoStmt(val tag: String, val specifiedRange: Range? = null) : Statement(specifiedRange)

data class CabStmt(
    val factor1: Expression,
    val factor2: Expression,
    val comparison: ComparisonOperator?,
    val tag: String,
    val rightIndicators: WithRightIndicators,
    val specifiedRange: Range? = null
) : Statement(specifiedRange), WithRightIndicators by rightIndicators

data class ForStmt(
    var init: Expression,
    val endValue: Expression,
    val byValue: Expression,
    val downward: Boolean = false,
    override val body: List<Statement>,
    val specifiedRange: Range? = null
) : Statement(specifiedRange), CompositeStatement {
    fun iterDataDefinition(): AbstractDataDefinition {
        if (init is AssignmentExpr) {
            if ((init as AssignmentExpr).target is DataRefExpr) {
                return ((init as AssignmentExpr).target as DataRefExpr).variable.referred!!
            } else {
                throw UnsupportedOperationException()
            }
        } else {
            throw UnsupportedOperationException()
        }
    }
}

/*
* For an array data structure, the keyed-ds-array operand is a qualified name consisting
* of the array to be sorted followed by the subfield to be used as a key for the sort.
*/

data class SortAStmt(val target: Expression, val specifiedRange: Range? = null) : Statement(
    specifiedRange
)

data class CatStmt(
    val left: Expression?,
    val right: Expression,
    val target: AssignableExpression,
    val blanksInBetween: Int,
    val specifiedRange: Range? = null
) : Statement(specifiedRange)

data class LookupStmt(
    val left: Expression,
    val right: Expression,
    val rightIndicators: WithRightIndicators,
    val specifiedRange: Range? = null
) : Statement(specifiedRange), WithRightIndicators by rightIndicators

data class ScanStmt(
    val left: Expression,
    val leftLength: Int?,
    val right: Expression,
    val startPosition: Int,
    val target: AssignableExpression,
    val rightIndicators: WithRightIndicators,
    val specifiedRange: Range? = null
) : Statement(specifiedRange), WithRightIndicators by rightIndicators

data class XFootStmt(
    val left: Expression,
    val result: AssignableExpression,
    val rightIndicators: WithRightIndicators,
    @Derived val dataDefinition: InStatementDataDefinition? = null,
    val specifiedRange: Range? = null
) : Statement(specifiedRange), WithRightIndicators by rightIndicators, StatementThatCanDefineData {
    override fun dataDefinition(): List<InStatementDataDefinition> {
        if (dataDefinition != null) {
            return listOf(dataDefinition)
        }
        return emptyList()
    }
}
