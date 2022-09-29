package com.strumenta.kolasu.emf

import com.strumenta.kolasu.model.*
import com.strumenta.kolasu.validation.Result
import org.eclipse.emf.ecore.*
import org.eclipse.emf.ecore.resource.Resource
import java.math.BigDecimal
import java.math.BigInteger
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import kotlin.reflect.KClass
import kotlin.reflect.KType

interface EDataTypeHandler {
    fun canHandle(ktype: KType): Boolean
    fun toDataType(ktype: KType): EDataType
    fun external(): Boolean
}

interface EClassTypeHandler {
    fun canHandle(ktype: KType): Boolean {
        return if (ktype.classifier is KClass<*>) {
            canHandle(ktype.classifier as KClass<*>)
        } else {
            false
        }
    }
    fun canHandle(kclass: KClass<*>): Boolean
    fun toEClass(kclass: KClass<*>, eClassProvider: ClassifiersProvider): EClass
    fun external(): Boolean
}

interface ClassifiersProvider {
    fun isDataType(ktype: KType): Boolean {
        return try {
            provideDataType(ktype)
            true
        } catch (e: Exception) {
            false
        }
    }
    fun provideClass(kClass: KClass<*>): EClass
    fun provideDataType(ktype: KType): EDataType?
}

class KolasuClassHandler(val kolasuKClass: KClass<*>, val kolasuEClass: EClass) : EClassTypeHandler {
    override fun canHandle(kclass: KClass<*>): Boolean = kclass == kolasuKClass

    override fun toEClass(kclass: KClass<*>, eClassProvider: ClassifiersProvider): EClass {
        return kolasuEClass
    }

    override fun external(): Boolean = true
}

class KolasuDataTypeHandler(val kolasuKClass: KClass<*>, val kolasuDataType: EDataType) : EDataTypeHandler {
    override fun canHandle(ktype: KType): Boolean {
        return ktype.classifier == kolasuKClass && ktype.arguments.isEmpty()
    }

    override fun toDataType(ktype: KType): EDataType {
        return kolasuDataType
    }

    override fun external(): Boolean = true
}

val LocalDateHandler = KolasuClassHandler(LocalDate::class, STARLASU_METAMODEL.getEClass("LocalDate"))
val LocalTimeHandler = KolasuClassHandler(LocalTime::class, STARLASU_METAMODEL.getEClass("LocalTime"))
val LocalDateTimeHandler = KolasuClassHandler(LocalDateTime::class, STARLASU_METAMODEL.getEClass("LocalDateTime"))

val NodeHandler = KolasuClassHandler(Node::class, STARLASU_METAMODEL.getEClass("ASTNode"))
val NamedHandler = KolasuClassHandler(Named::class, STARLASU_METAMODEL.getEClass("Named"))
val PositionHandler = KolasuClassHandler(Position::class, STARLASU_METAMODEL.getEClass("Position"))
val PossiblyNamedHandler = KolasuClassHandler(PossiblyNamed::class, STARLASU_METAMODEL.getEClass("PossiblyNamed"))
val ReferenceByNameHandler = KolasuClassHandler(ReferenceByName::class, STARLASU_METAMODEL.getEClass("ReferenceByName"))
val ResultHandler = KolasuClassHandler(Result::class, STARLASU_METAMODEL.getEClass("Result"))

val StatementHandler = KolasuClassHandler(Statement::class, STARLASU_METAMODEL.getEClass("Statement"))
val ExpressionHandler = KolasuClassHandler(Expression::class, STARLASU_METAMODEL.getEClass("Expression"))
val EntityDeclarationHandler = KolasuClassHandler(
    EntityDeclaration::class,
    STARLASU_METAMODEL
        .getEClass("EntityDeclaration")
)

val StringHandler = KolasuDataTypeHandler(String::class, EcorePackage.eINSTANCE.eString)
val CharHandler = KolasuDataTypeHandler(Char::class, EcorePackage.eINSTANCE.eChar)
val BooleanHandler = KolasuDataTypeHandler(Boolean::class, EcorePackage.eINSTANCE.eBoolean)
val IntHandler = KolasuDataTypeHandler(Int::class, EcorePackage.eINSTANCE.eInt)
val IntegerHandler = KolasuDataTypeHandler(Integer::class, EcorePackage.eINSTANCE.eInt)
val FloatHandler = KolasuDataTypeHandler(Float::class, EcorePackage.eINSTANCE.eFloat)
val DoubleHandler = KolasuDataTypeHandler(Double::class, EcorePackage.eINSTANCE.eDouble)
val BigIntegerHandler = KolasuDataTypeHandler(BigInteger::class, EcorePackage.eINSTANCE.eBigInteger)
val BigDecimalHandler = KolasuDataTypeHandler(BigDecimal::class, EcorePackage.eINSTANCE.eBigDecimal)
val LongHandler = KolasuDataTypeHandler(Long::class, EcorePackage.eINSTANCE.eLong)

val KClass<*>.eClassifierName: String
    get() = this.java.eClassifierName

val Class<*>.eClassifierName: String
    get() = if (this.enclosingClass != null) {
        "${this.enclosingClass.simpleName}${this.simpleName}"
    } else {
        this.simpleName
    }

class ResourceClassTypeHandler(val resource: Resource, val ownPackage: EPackage) : EClassTypeHandler {
    override fun canHandle(ktype: KClass<*>): Boolean = getPackage(packageName(ktype)) != null

    private fun getPackage(packageName: String): EPackage? =
        resource.contents.find { it is EPackage && it != ownPackage && it.name == packageName } as EPackage?

    override fun toEClass(kclass: KClass<*>, eClassProvider: ClassifiersProvider): EClass {
        return getPackage(packageName(kclass))!!.eClassifiers.find {
            it is EClass && it.name == kclass.simpleName
        } as EClass? ?: throw NoClassDefFoundError(kclass.qualifiedName)
    }

    override fun external(): Boolean = true
}

internal fun EPackage.hasClassifierNamed(name: String): Boolean {
    return this.eClassifiers.any { it.name == name }
}

internal fun EPackage.classifierByName(name: String): EClassifier {
    return this.eClassifiers.find { it.name == name } ?: throw IllegalArgumentException(
        "No classifier named $name was found"
    )
}
