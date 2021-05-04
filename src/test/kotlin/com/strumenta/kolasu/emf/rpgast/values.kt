package com.strumenta.kolasu.emf.rpgast

import com.smeup.rpgparser.parsing.ast.CompilationUnit
import java.math.BigDecimal
import java.nio.charset.Charset
import java.text.SimpleDateFormat
import java.util.*

const val PAD_CHAR = ' '
const val PAD_STRING = PAD_CHAR.toString()

val DEFAULT_CHARSET = Charset.forName("Cp037")

fun coerce(value: Value, type: Type): Value {
    return value
}

interface Value : Comparable<Value> {
    fun asInt(): IntValue = throw UnsupportedOperationException("${this.javaClass.simpleName} cannot be seen as an Int")
    fun asDecimal(): DecimalValue = throw UnsupportedOperationException("${this.javaClass.simpleName} cannot be seen as an Decimal")
    fun asString(): StringValue
    fun asBoolean(): BooleanValue = throw UnsupportedOperationException()
    fun asTimeStamp(): TimeStampValue = throw UnsupportedOperationException("${this.javaClass.simpleName} cannot be seen as an TimeStamp - $this")
    fun assignableTo(expectedType: Type): Boolean
    fun takeLast(n: Int): Value = TODO("takeLast not yet implemented for ${this.javaClass.simpleName}")
    fun takeFirst(n: Int): Value = TODO("takeFirst not yet implemented for ${this.javaClass.simpleName}")
    fun take(from: Int, to: Int): Value = TODO("take not yet implemented for ${this.javaClass.simpleName}")
    fun concatenate(other: Value): Value = TODO("concatenate not yet implemented for ${this.javaClass.simpleName}")
    fun asArray(): ArrayValue = throw UnsupportedOperationException()
    fun render(): String = "Nope"
    fun copy(): Value
    fun toArray(nElements: Int, elementType: Type): Value {
        val elements = LinkedList<Value>()
        for (i in 1..nElements) {
            elements.add(coerce(this.copy(), elementType))
        }
        return ConcreteArrayValue(elements, elementType)
    }
    override operator fun compareTo(other: Value): Int = TODO("Cannot compare $this to $other")
}

abstract class NumberValue : Value {
    fun isNegative(): Boolean = bigDecimal < BigDecimal.ZERO
    fun abs(): NumberValue = if (isNegative()) negate() else this
    abstract fun negate(): NumberValue
    abstract fun increment(amount: Long): NumberValue

    abstract val bigDecimal: BigDecimal
}

data class StringValue(var value: String, val varying: Boolean = false) : Value {

    override fun assignableTo(expectedType: Type): Boolean {
        return when (expectedType) {
            is StringType -> expectedType.length >= value.length.toLong()
            is CharacterType -> expectedType.nChars >= value.length.toLong()
            is DataStructureType -> expectedType.elementSize == value.length // Check for >= ???
            is ArrayType -> expectedType.size == value.length // Check for >= ???
            else -> false
        }
    }

    override fun takeLast(n: Int): Value {
        return StringValue(value.takeLast(n))
    }

    override fun takeFirst(n: Int): Value {
        return StringValue(value.take(n))
    }

    override fun concatenate(other: Value): Value {
        require(other is StringValue) {
            "Cannot concatenate $value to $other"
        }
        val stringBuilder = StringBuilder()
        val stringValue = stringBuilder.append(value.toString()).append(other.value.toString()).toString()
        return StringValue(stringValue)
    }

    companion object {
        fun blank(length: Int) = StringValue(" ".repeat(length))
        fun padded(value: String, size: Int) = StringValue(value.padEnd(size, ' '))
    }

    override fun equals(other: Any?): Boolean {
        return if (other is StringValue) {
            this.value == other.value
        } else {
            false
        }
    }

    fun pad(size: Int) {
        value += " ".repeat(size - value.length)
    }

    fun trimEnd() {
        value = value.trimEnd()
    }

    override fun hashCode(): Int {
        return value.hashCode()
    }

    override fun asBoolean(): BooleanValue {
        if (value == "1") {
            return BooleanValue.TRUE
        }
        return BooleanValue.FALSE
    }

    override fun asTimeStamp(): TimeStampValue = TimeStampValue(value.asIsoDate())

    fun setSubstring(startOffset: Int, endOffset: Int, substringValue: StringValue) {
        require(startOffset >= 0)
        require(startOffset <= value.length)
        require(endOffset >= startOffset)
        require(endOffset <= value.length) { "Asked startOffset=$startOffset, endOffset=$endOffset on string of length ${value.length}" }
        substringValue.pad(endOffset - startOffset)
        value = value.substring(0, startOffset) + substringValue.value + value.substring(endOffset)
    }

    fun getSubstring(startOffset: Int, endOffset: Int): StringValue {
        require(startOffset >= 0)
        require(startOffset <= value.length)
        require(endOffset >= startOffset)
        require(endOffset <= value.length) { "Asked startOffset=$startOffset, endOffset=$endOffset on string of length ${value.length}" }
        val s = value.substring(startOffset, endOffset)
        return StringValue(s)
    }

    override fun take(from: Int, to: Int): Value {
        return getSubstring(from - 1, to)
    }

    override fun toString(): String {
        return "StringValue[${value.length}]($value)"
    }

    override fun asString() = this

    fun isBlank(): Boolean {
        return this.value.isBlank()
    }

    override fun render(): String {
        return value
    }

    override fun copy(): StringValue = this

    fun length(varying: Boolean = this.varying): Int {
        if (varying) {
            if (value.isBlank()) {
                return 0
            }
        }
        return value.length
    }

    fun compare(other: StringValue, charset: Charset?, descend: Boolean = false): Int {
        require(charset != null)
        if (this == other) return 0
        val bs1 = this.value.toByteArray(charset)
        val bs2 = other.value.toByteArray(charset)

        // It is possible to translate the character codes from the CP 037 charset to ISO 8859-1
        // character codes, so that translation  back to the CP 037 charset is an
        // exact value-preserving round-trip conversion.
        val s1 = String(bs1, Charsets.ISO_8859_1)
        val s2 = String(bs2, Charsets.ISO_8859_1)

        if (descend) {
            return s2.compareTo(s1)
        }
        return s1.compareTo(s2)
    }

    override operator fun compareTo(other: Value): Int =
        when (other) {
            is StringValue -> compare(other, DEFAULT_CHARSET)
            else -> super.compareTo(other)
        }
}

/**
 * The charset should be sort of system setting
 * Cp037    EBCDIC US
 * Cp0280   EBCDIC ITALIAN
 * See: https://www.ibm.com/support/knowledgecenter/SSLTBW_2.1.0/com.ibm.zos.v2r1.idad400/ccsids.htm
 */
fun sortA(value: Value, charset: Charset) {
    when (value) {
        is ConcreteArrayValue -> {
            // TODO pass the correct charset to the default sorting algorithm
            // TODO ascending/descending
            value.elements.sort()
        }
        is ProjectedArrayValue -> {
            require(value.field.type is ArrayType)
            val strings = value.field.type.element is StringType
            val n = value.arrayLength
            val multiplier = if (value.field.descend) 1 else -1
            // the good old Bubble Sort
            for (i in 1..(value.arrayLength - 1)) {
                for (j in 1..(n - i - 1)) {
                    val compared =
                        if (strings) {
                            value.getElement(j).asString().compare(value.getElement(j + 1).asString(), charset, value.field.descend)
                        } else {
                            value.getElement(j).compareTo(value.getElement(j + 1)) * multiplier
                        }
                    if (compared > 0) {
                        // TODO support data structure swap
                        // For an array data structure, the keyed-ds-array operand is a qualified name
                        // consisting of the array to be sorted followed by the subfield to be used as
                        // a key for the sort.
                        // Swap
                        var tmp = value.getElement(j + 1)
                        value.setElement(j + 1, value.getElement(j))
                        value.setElement(j, tmp)
                    }
                }
            }
        }
    }
}

data class IntValue(val value: Long) : NumberValue() {
    override val bigDecimal: BigDecimal by lazy { BigDecimal(value) }
    override fun negate(): NumberValue = IntValue(-value)
    override fun increment(amount: Long): NumberValue = this + IntValue(amount)

    override fun assignableTo(expectedType: Type): Boolean {
        // TODO check decimals
        when (expectedType) {
            is NumberType -> return true
            is ArrayType -> {
                return expectedType.element is NumberType
            }
        }
        return false
    }

    override fun asInt() = this
    // TODO Verify conversion
    override fun asDecimal(): DecimalValue = DecimalValue(bigDecimal)

    override fun takeFirst(n: Int): Value {
        return IntValue(firstDigits(value, n))
    }

    override fun takeLast(n: Int): Value {
        return IntValue(lastDigits(value, n))
    }

    private fun lastDigits(n: Long, digits: Int): Long {
        return (n % Math.pow(10.0, digits.toDouble())).toLong()
    }

    private fun firstDigits(n: Long, digits: Int): Long {
        var localNr = n
        if (n < 0) {
            localNr = n * -1
        }
        val div = Math.pow(10.0, digits.toDouble()).toInt()
        while (localNr / div > 0) {
            localNr /= 10
        }
        return localNr * java.lang.Long.signum(n)
    }

    override fun take(from: Int, to: Int): Value =
        firstDigits(value, to)
            .let { lastDigits(it, (to - from) + 1) }
            .let(::IntValue)

    override fun concatenate(other: Value): Value {
        require(other is IntValue) {
            "Cannot concatenate $value to $other"
        }
        val stringBuilder = StringBuilder()
        val stringValue = stringBuilder.append(value.toString()).append(other.value.toString()).toString()
        return IntValue((stringValue).toLong())
    }

    companion object {
        val ZERO = IntValue(0)
        val ONE = IntValue(1)

        fun sequenceOfNines(length: Int): IntValue {
            require(length >= 1)
            val ed = "9".repeat(length)
            return IntValue("$ed".toLong())
        }
    }

    override fun render(): String {
        return value.toString()
    }

    override fun copy(): IntValue = this

    override operator fun compareTo(other: Value): Int = when (other) {
        is IntValue -> value.compareTo(other.value)
        is DecimalValue -> this.asDecimal().compareTo(other)
        else -> super.compareTo(other)
    }

    override fun asString(): StringValue {
        return StringValue(render())
    }

    operator fun plus(other: IntValue) = IntValue(this.bigDecimal.plus(other.bigDecimal).longValueExact())

    operator fun minus(other: IntValue) = IntValue(this.bigDecimal.minus(other.bigDecimal).longValueExact())

    operator fun times(other: IntValue) = IntValue(this.bigDecimal.times(other.bigDecimal).longValueExact())
}

data class DecimalValue(val value: BigDecimal) : NumberValue() {

    override val bigDecimal: BigDecimal
        get() = value

    override fun negate(): NumberValue = DecimalValue(-value)

    override fun increment(amount: Long): NumberValue = DecimalValue(value.add(BigDecimal(amount)))

    override fun asInt(): IntValue = IntValue(value.toLong())

    override fun asDecimal(): DecimalValue = this

    override fun assignableTo(expectedType: Type): Boolean {
        // TODO check decimals
        when (expectedType) {
            is NumberType -> return true
            is ArrayType -> {
                return expectedType.element is NumberType
            }
        }
        return false
    }

    fun isPositive(): Boolean {
        return value.signum() >= 0
    }

    companion object {
        val ZERO = DecimalValue(BigDecimal.ZERO)
        val ONE = DecimalValue(BigDecimal.ONE)
    }

    override fun render(): String {
        // zeros followed by decimal point has not be rendered
        return value.toPlainString().let {
            if (it.startsWith("0") && it.indexOf('.') != -1) {
                it.replace(Regex("^0+"), "")
            } else {
                it
            }
        }
    }

    override fun copy(): DecimalValue = this

    override operator fun compareTo(other: Value): Int =
        when (other) {
            is IntValue -> compareTo(other.asDecimal())
            is DecimalValue -> this.value.compareTo(other.value)
            else -> super.compareTo(other)
        }

    override fun asString(): StringValue {
        return StringValue(value.toPlainString())
    }
}

data class BooleanValue private constructor(val value: Boolean) : Value {
    override fun assignableTo(expectedType: Type): Boolean {
        return expectedType is BooleanType
    }

    override fun asBoolean() = this

    override fun asString() = StringValue(if (value) "1" else "0")

    companion object {
        val FALSE = BooleanValue(false)
        val TRUE = BooleanValue(true)
        operator fun invoke(value: Boolean) = if (value) TRUE else FALSE
    }
    override fun render(): String {
        return value.toString()
    }

    override fun copy(): BooleanValue = this
    override operator fun compareTo(other: Value): Int =
        when (other) {
            is BooleanValue -> value.compareTo(other.value)
            else -> super.compareTo(other)
        }
}

data class CharacterValue(val value: Array<Char>) : Value {
    override fun assignableTo(expectedType: Type): Boolean {
        return expectedType is CharacterType
    }

    override fun copy(): CharacterValue = this

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as CharacterValue

        if (!value.contentEquals(other.value)) return false

        return true
    }

    override fun hashCode(): Int {
        return value.contentHashCode()
    }

    override fun asString(): StringValue {
        return StringValue(value.toString())
    }
}

data class TimeStampValue(val value: Date) : Value {
    override fun assignableTo(expectedType: Type): Boolean {
        return expectedType is TimeStampType
    }

    override fun asTimeStamp() = this

    companion object {
        val LOVAL = TimeStampValue(GregorianCalendar(0, Calendar.JANUARY, 0).time)
    }

    override fun copy(): TimeStampValue = this

    override fun asString(): StringValue {
        return StringValue(value.toString())
    }
}

abstract class ArrayValue : Value {
    abstract fun arrayLength(): Int
    abstract fun elementSize(): Int
    fun totalSize() = elementSize() * arrayLength()
    abstract fun setElement(index: Int, value: Value)
    abstract fun getElement(index: Int): Value
    fun elements(): List<Value> {
        val elements = LinkedList<Value>()
        for (i in 0 until (arrayLength())) {
            elements.add(getElement(i + 1))
        }
        return elements
    }

    override fun asString(): StringValue {
        return StringValue(elements().map { it.asString() }.joinToString(""))
    }

    override fun assignableTo(expectedType: Type): Boolean {
        if (expectedType is DataStructureType) {
            // FIXME
            return true
        }
        if (expectedType is ArrayType) {
            return elements().all {
                it.assignableTo(expectedType.element)
            }
        }
        if (expectedType is StringType) {
            return expectedType.length >= arrayLength() * elementSize()
        }
        return false
    }
    override fun render(): String {
        return "Array(${elements().size})"
    }
    override fun asArray() = this

    fun areEquivalent(other: ArrayValue): Boolean {
        if (this.arrayLength() != other.arrayLength()) {
            return false
        }
        for (i in 1..this.arrayLength()) {
            if (this.getElement(i) != other.getElement(i)) {
                return false
            }
        }
        return true
    }

    override fun hashCode(): Int {
        var res = this.arrayLength()
        if (this.arrayLength() > 0) {
            res *= 7 * this.getElement(1).hashCode()
            res *= 3 * this.getElement(this.arrayLength()).hashCode()
        }
        return res
    }

    abstract val elementType: Type

    override fun copy(): ArrayValue {
        return ConcreteArrayValue(this.elements().map { it.copy() }.toMutableList(), this.elementType)
    }

    fun concatenateElementsFrom(index: Int): Value {
        var result: Value = getElement(index)
        for (i in (index + 1) until (arrayLength() + 1)) {
            result = result.concatenate(getElement(i))
        }
        return result
    }
}

data class ConcreteArrayValue(val elements: MutableList<Value>, override val elementType: Type) : ArrayValue() {
    init {
        require(elementType !is ArrayType) {
            "An array can't contain another array"
        }
    }

    override fun elementSize() = elementType.size

    override fun arrayLength() = elements.size

    override fun setElement(index: Int, value: Value) {
        require(index >= 1)
        require(index <= arrayLength())
        if (!value.assignableTo(elementType)) {
            println("boom")
        }
        require(value.assignableTo(elementType)) {
            "Cannot assign ${value::class.qualifiedName} to ${elementType::class.qualifiedName}"
        }
        if (elementType is StringType && !elementType.varying) {
            val v = (value as StringValue).copy()
            v.pad(elementType.length)
            elements[index - 1] = v
        } else {
            elements[index - 1] = value
        }
    }

    override fun getElement(index: Int): Value {
        require(index >= 1) { "Indexes should be >=1. Index asked: $index" }
        require(index <= arrayLength())
        return elements[index - 1]
    }

    override fun equals(other: Any?): Boolean {
        return if (other is ArrayValue) {
            this.areEquivalent(other)
        } else {
            false
        }
    }

    fun takeAll(): Value {
        var result = elements[0]
        for (i in 1 until elements.size) {
            result = result.concatenate(elements[i])
        }
        return result
    }

    override fun takeLast(n: Int): Value = takeAll().takeLast(n)

    override fun takeFirst(n: Int): Value = takeAll().takeFirst(n)
}

object BlanksValue : Value {
    override fun toString(): String {
        return "BlanksValue"
    }

    override fun assignableTo(expectedType: Type): Boolean {
        // FIXME
        return true
    }

    override fun copy(): BlanksValue = this

    override fun asString(): StringValue {
        return StringValue(this.toString())
    }
}

object HiValValue : Value {
    private val MAX_INT = IntValue(Long.MAX_VALUE)

    override fun asInt(): IntValue = MAX_INT

    override fun toString(): String {
        return "HiValValue"
    }

    override fun assignableTo(expectedType: Type): Boolean {
        // FIXME
        return true
    }

    override fun copy(): HiValValue = this

    override operator fun compareTo(other: Value): Int =
        if (other is HiValValue) 0 else 1

    override fun asString(): StringValue {
        TODO("Not yet implemented")
    }
}

object LowValValue : Value {
    override fun copy(): Value {
        TODO("not implemented")
    }

    override fun toString(): String {
        return "LowValValue"
    }

    override fun assignableTo(expectedType: Type): Boolean {
        // FIXME
        return true
    }

    override operator fun compareTo(other: Value): Int =
        if (other is LowValValue) 0 else -1

    override fun asString(): StringValue {
        TODO("Not yet implemented")
    }
}

object ZeroValue : Value {

    override fun copy(): Value {
        TODO("not implemented")
    }

    override fun toString(): String {
        return "ZeroValue"
    }

    override fun assignableTo(expectedType: Type): Boolean {
        // FIXME
        return true
    }

    override fun asString(): StringValue {
        TODO("Not yet implemented")
    }
}

class AllValue(val charsToRepeat: String) : Value {
    override fun assignableTo(expectedType: Type): Boolean {
        // FIXME
        return true
    }

    override fun copy(): AllValue = this

    override fun asString(): StringValue {
        TODO("Not yet implemented")
    }
}

/**
 * The container should always be a DS value
 */
class ProjectedArrayValue(
    val container: DataStructValue,
    val field: FieldDefinition,
    val startOffset: Int,
    val step: Int,
    val arrayLength: Int
) : ArrayValue() {
    override val elementType: Type
        get() = (this.field.type as ArrayType).element

    companion object {
        fun forData(containerValue: DataStructValue, data: FieldDefinition): ProjectedArrayValue {
            val stepSize = data.stepSize
            val arrayLength = data.declaredArrayInLine!!
            return ProjectedArrayValue(containerValue, data, data.startOffset, stepSize, arrayLength)
        }
    }

    override fun elementSize(): Int {
        TODO("not implemented") // To change body of created functions use File | Settings | File Templates.
    }

    override fun arrayLength() = arrayLength

    override fun setElement(index: Int, value: Value) {
        require(index >= 1)
        require(index <= arrayLength())
        require(value.assignableTo((field.type as ArrayType).element)) { "Assigning to field $field incompatible value $value" }
        val startIndex = (this.startOffset + this.step * (index - 1)).toInt()
        val endIndex = (startIndex + this.field.elementSize()).toInt()
        container.setSubstring(startIndex, endIndex, coerce(value, StringType(this.field.elementSize())) as StringValue)
    }

    override fun getElement(index: Int): Value {
        require(index >= 1) { "Indexes should be >=1. Index asked: $index" }
        if (index > arrayLength()) {
            println()
        }
        require(index <= arrayLength())

        val startIndex = (this.startOffset + this.step * (index - 1)).toInt()
        val endIndex = (startIndex + this.field.elementSize()).toInt()
        val substringValue = container.getSubstring(startIndex, endIndex)

        return coerce(substringValue, (this.field.type as ArrayType).element)
    }

    override fun equals(other: Any?): Boolean {
        return if (other is ArrayValue) {
            this.areEquivalent(other)
        } else {
            false
        }
    }

    override fun asString(): StringValue {
        TODO("Not yet implemented")
    }
}

fun createArrayValue(elementType: Type, n: Int, creator: (Int) -> Value) = ConcreteArrayValue(Array(n, creator).toMutableList(), elementType)

fun List<Value>.asConcreteArrayValue(elementType: Type) = createArrayValue(elementType, size) {
    this[it + 1]
}

fun blankString(length: Int) = StringValue(" ".repeat(length))

fun Long.asValue() = IntValue(this)

fun String.asValue() = StringValue(this)

private const val FORMAT_DATE_ISO = "yyyy-MM-dd-HH.mm.ss.SSS"

fun String.asIsoDate(): Date {
    var dateString = if (length >= FORMAT_DATE_ISO.length) {
        this.take(FORMAT_DATE_ISO.length)
    } else {
        // TODO
        this + "-00.00.00.000"
    }
    return SimpleDateFormat(FORMAT_DATE_ISO).parse(dateString)
}

fun createBlankFor(dataDefinition: DataDefinition): Value {
    val ds = DataStructValue.blank(dataDefinition.type.size)
    dataDefinition.fields.forEach {
        if (it.type is NumberType) ds.set(it, it.type.toRPGValue(dataDefinition.inz))
    }
    return ds
}

private fun NumberType.toRPGValue(iniz: Boolean): Value =
    when (rpgType) {
        RpgType.ZONED.rpgType, RpgType.PACKED.rpgType -> if (iniz) DecimalValue.ZERO else DecimalValue.ONE
        RpgType.BINARY.rpgType, RpgType.INTEGER.rpgType, RpgType.UNSIGNED.rpgType -> if (iniz) IntValue.ZERO else IntValue.ONE
        else -> TODO("Please handle RpgType $rpgType")
    }

fun Type.blank(): Value {
    return when (this) {
        is ArrayType -> createArrayValue(this.element, this.nElements) {
            this.element.blank()
        }
        is DataStructureType -> DataStructValue.blank(this.size)
        is StringType -> StringValue.blank(this.size)
        is NumberType -> IntValue(0)
        is BooleanType -> BooleanValue.FALSE
        is TimeStampType -> TimeStampValue.LOVAL
        // TODO check this during the process of revision of DB access
        is KListType -> throw UnsupportedOperationException("Blank value not supported for KList")
        is CharacterType -> CharacterValue(Array(this.nChars) { ' ' })
        is FigurativeType -> BlanksValue
        is LowValType, is HiValType -> TODO()
    }
}

/**
 * StringValue wrapper
 */

data class DataStructValue(var value: String, private val optionalExternalLen: Int? = null) : Value {
    // We can't serialize a class with a var computed from another one because of a bug in the serialization plugin
    // See https://github.com/Kotlin/kotlinx.serialization/issues/133
    val len by lazy { optionalExternalLen ?: value.length }

    override fun assignableTo(expectedType: Type): Boolean {
        return when (expectedType) {
            // Check if the size of the value matches the expected size within the DS
            // TO REVIEW
            is DataStructureType -> true
            is StringType -> expectedType.size >= this.value.length
            else -> false
        }
    }

    override fun copy(): DataStructValue = DataStructValue(value)

    /**
     * A DataStructure could also be an array of data structures. In that case the field is seen as
     * an array itself, so setting the field value requires an array value. In cases in which we
     * want to set a field of a single instance of the data structure we can use this method.
     */
    fun setSingleField(field: FieldDefinition, value: Value) {
        try {
            val v = (field.type as ArrayType).element.toDataStructureValue(value)
            val startIndex = field.startOffset
            val endIndex = field.startOffset + field.elementSize()
            try {
                this.setSubstring(startIndex, endIndex, v)
            } catch (e: Exception) {
                throw RuntimeException("Issue arose while setting field ${field.name}. Indexes: $startIndex to $endIndex. Field size: ${field.size}. Value: $value", e)
            }
        } catch (e: Throwable) {
            throw RuntimeException("Issue arose while setting field ${field.name}. Value: $value", e)
        }
    }

    fun set(field: FieldDefinition, value: Value) {
        val v = field.toDataStructureValue(value)
        val startIndex = field.startOffset
        val endIndex = field.startOffset + field.size
        try {
            this.setSubstring(startIndex, endIndex, v)
        } catch (e: Exception) {
            throw RuntimeException("Issue arose while setting field ${field.name}. Indexes: $startIndex to $endIndex. Field size: ${field.size}. Value: $value", e)
        }
    }

    operator fun get(data: FieldDefinition): Value {
        return if (data.declaredArrayInLine != null) {
            ProjectedArrayValue.forData(this, data)
        } else {
            coerce(this.getSubstring(data.startOffset, data.endOffset), data.type)
        }
    }

    /**
     * See setSingleField
     */
    fun getSingleField(data: FieldDefinition): Value {
        require(data.type is ArrayType)
        return coerce(this.getSubstring(data.startOffset, data.endOffset), data.type.element)
    }

    fun setSubstring(startOffset: Int, endOffset: Int, substringValue: StringValue) {
        require(startOffset >= 0)
        // Not clear this requirement
        if (startOffset >= value.length) {
            println()
        }
        require(startOffset <= value.length)
        require(endOffset >= startOffset)
        require(endOffset <= value.length) { "Asked startOffset=$startOffset, endOffset=$endOffset on string of length ${value.length}" }
        // require(endOffset - startOffset == substringValue.value.length) { "Setting value $substringValue, with length ${substringValue.value.length}, into field of length ${endOffset - startOffset}" }
        // changed to >= a small value fits in a bigger one
        require(endOffset - startOffset >= substringValue.value.length) { "Setting value $substringValue, with length ${substringValue.value.length}, into field of length ${endOffset - startOffset}" }
        substringValue.pad(endOffset - startOffset)
        value = value.substring(0, startOffset) + substringValue.value + value.substring(endOffset)
    }

    fun getSubstring(startOffset: Int, endOffset: Int): StringValue {
        require(startOffset >= 0)
        require(startOffset <= value.length)
        require(endOffset >= startOffset)
        require(endOffset <= value.length) { "Asked startOffset=$startOffset, endOffset=$endOffset on string of length ${value.length}" }
        val s = value.substring(startOffset, endOffset)
        return StringValue(s)
    }

    companion object {
        fun blank(length: Int) = DataStructValue(" ".repeat(length))

        /**
         * Create a new instance of DataStructValue
         * @param compilationUnit A compilation unit
         * @param dataStructName Name
         * @param values Initialization values
         * */
        fun createInstance(compilationUnit: CompilationUnit, dataStructName: String, values: Map<String, Value>): DataStructValue {
            val dataStructureDefinition = compilationUnit.getDataDefinition(dataStructName)
            val newInstance = blank(dataStructureDefinition.type.size)
            values.forEach {
                newInstance.set(
                    field = dataStructureDefinition.getFieldByName(it.key),
                    value = it.value
                )
            }
            return newInstance
        }
    }

    override fun toString(): String {
        return "DataStructureValue[${value.length}]($value)"
    }

    override fun asString() = StringValue(this.value)

    // Use this method when need to compare to StringValue
    fun asStringValue(): String {
        val builder = StringBuilder()
        value.forEach {
            if (it.toInt() < 32 || it.toInt() > 128 || it in '0'..'9') {
                builder.append(' ')
            } else {
                builder.append(it)
            }
        }
        return builder.toString()
    }

    fun isBlank(): Boolean {
        return this.value.isBlank()
    }
}

fun Int.asValue() = IntValue(this.toLong())
fun Boolean.asValue() = BooleanValue(this)

fun areEquals(value1: Value, value2: Value): Boolean {
    return when {
        value1 is DecimalValue && value2 is IntValue ||
            value1 is IntValue && value2 is DecimalValue -> {
            value1.asInt() == value2.asInt()
        }

        value1 is StringValue && value2 is BooleanValue -> {
            value1.asBoolean().value == value2.value
        }

        value1 is BooleanValue && value2 is StringValue -> {
            value2.asBoolean().value == value1.value
        }

        value1 is DecimalValue && value2 is DecimalValue -> {
            // Convert everything to Decimal then compare
            value1.asDecimal().value.compareTo(value2.asDecimal().value) == 0
        }

        value1 is BlanksValue && value2 is StringValue -> value2.isBlank()
        value2 is BlanksValue && value1 is StringValue -> value1.isBlank()

        value1 is StringValue && value2 is StringValue -> {
            val v1 = value1.value.trimEnd()
            val v2 = value2.value.trimEnd()
            v1 == v2
        }

        value1 is DataStructValue && value2 is StringValue -> {
            val v1 = value1.asStringValue().trimEnd()

            val v2 = value2.value.trimEnd()
            v1 == v2
        }
        value1 is StringValue && value2 is DataStructValue -> {
            val v1 = value1.value.trimEnd()
            val v2 = value2.asStringValue().trimEnd()

            v1 == v2
        }
        // To be review
        value1 is ProjectedArrayValue && value2 is StringValue -> {
            value1.asArray().getElement(1) == value2
        }
        else -> value1 == value2
    }
}
