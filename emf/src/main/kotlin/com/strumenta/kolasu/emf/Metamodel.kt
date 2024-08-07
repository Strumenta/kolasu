package com.strumenta.kolasu.emf

import com.strumenta.kolasu.model.BehaviorDeclaration
import com.strumenta.kolasu.model.Documentation
import com.strumenta.kolasu.model.EntityDeclaration
import com.strumenta.kolasu.model.EntityGroupDeclaration
import com.strumenta.kolasu.model.ErrorNode
import com.strumenta.kolasu.model.Expression
import com.strumenta.kolasu.model.GenericErrorNode
import com.strumenta.kolasu.model.Named
import com.strumenta.kolasu.model.Node
import com.strumenta.kolasu.model.NodeLike
import com.strumenta.kolasu.model.Parameter
import com.strumenta.kolasu.model.PlaceholderElement
import com.strumenta.kolasu.model.PossiblyNamed
import com.strumenta.kolasu.model.Range
import com.strumenta.kolasu.model.ReferenceValue
import com.strumenta.kolasu.model.Statement
import com.strumenta.kolasu.model.TypeAnnotation
import com.strumenta.kolasu.transformation.GenericNode
import com.strumenta.kolasu.validation.Result
import org.eclipse.emf.ecore.EClass
import org.eclipse.emf.ecore.EClassifier
import org.eclipse.emf.ecore.EDataType
import org.eclipse.emf.ecore.EPackage
import org.eclipse.emf.ecore.EcorePackage
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
    fun canHandle(ktype: KType): Boolean =
        if (ktype.classifier is KClass<*>) {
            canHandle(ktype.classifier as KClass<*>)
        } else {
            false
        }

    fun canHandle(kclass: KClass<*>): Boolean

    fun toEClass(
        kclass: KClass<*>,
        eClassProvider: ClassifiersProvider,
    ): EClass

    fun external(): Boolean
}

interface ClassifiersProvider {
    fun isDataType(ktype: KType): Boolean =
        try {
            provideDataType(ktype)
            true
        } catch (e: Exception) {
            false
        }

    fun provideClass(kClass: KClass<*>): EClass

    fun provideDataType(ktype: KType): EDataType?
}

class KolasuClassHandler(
    val kolasuKClass: KClass<*>,
    val kolasuEClass: EClass,
) : EClassTypeHandler {
    override fun canHandle(kclass: KClass<*>): Boolean = kclass == kolasuKClass

    override fun toEClass(
        kclass: KClass<*>,
        eClassProvider: ClassifiersProvider,
    ): EClass = kolasuEClass

    override fun external(): Boolean = true

    companion object {
        fun forKClass(kclass: KClass<*>): KolasuClassHandler =
            KolasuClassHandler(
                kclass,
                STARLASU_METAMODEL
                    .getEClass(kclass.simpleName!!),
            )
    }
}

class KolasuDataTypeHandler(
    val kolasuKClass: KClass<*>,
    val kolasuDataType: EDataType,
) : EDataTypeHandler {
    override fun canHandle(ktype: KType): Boolean = ktype.classifier == kolasuKClass && ktype.arguments.isEmpty()

    override fun toDataType(ktype: KType): EDataType = kolasuDataType

    override fun external(): Boolean = true
}

val LocalDateHandler = KolasuClassHandler(LocalDate::class, STARLASU_METAMODEL.getEClass("LocalDate"))
val LocalTimeHandler = KolasuClassHandler(LocalTime::class, STARLASU_METAMODEL.getEClass("LocalTime"))
val LocalDateTimeHandler = KolasuClassHandler(LocalDateTime::class, STARLASU_METAMODEL.getEClass("LocalDateTime"))

val NodeHandler = KolasuClassHandler(Node::class, STARLASU_METAMODEL.getEClass("ASTNode"))
val NodeLikeHandler = KolasuClassHandler(NodeLike::class, STARLASU_METAMODEL.getEClass("ASTNode"))
val NamedHandler = KolasuClassHandler(Named::class, STARLASU_METAMODEL.getEClass("Named"))
val RangeHandler = KolasuClassHandler(Range::class, STARLASU_METAMODEL.getEClass("Position"))
val PossiblyNamedHandler = KolasuClassHandler(PossiblyNamed::class, STARLASU_METAMODEL.getEClass("PossiblyNamed"))
val ReferenceValueHandler = KolasuClassHandler(ReferenceValue::class, STARLASU_METAMODEL.getEClass("ReferenceByName"))

// This class is saved with the name Position for compatibility reasons
val ResultHandler = KolasuClassHandler(Result::class, STARLASU_METAMODEL.getEClass("Position"))

val StatementHandler = KolasuClassHandler.forKClass(Statement::class)
val ExpressionHandler = KolasuClassHandler.forKClass(Expression::class)
val EntityDeclarationHandler = KolasuClassHandler.forKClass(EntityDeclaration::class)
val EntityGroupDeclarationHandler = KolasuClassHandler.forKClass(EntityGroupDeclaration::class)
val TypeAnnotationHandler = KolasuClassHandler.forKClass(TypeAnnotation::class)
val PlaceholderElementHandler = KolasuClassHandler.forKClass(PlaceholderElement::class)
val BehaviorDeclarationHandler = KolasuClassHandler.forKClass(BehaviorDeclaration::class)
val ParameterHandler = KolasuClassHandler.forKClass(Parameter::class)
val DocumentationHandler = KolasuClassHandler.forKClass(Documentation::class)

val ErrorNodeHandler = KolasuClassHandler(ErrorNode::class, STARLASU_METAMODEL.getEClass("ErrorNode"))
val GenericErrorNodeHandler =
    KolasuClassHandler(
        GenericErrorNode::class,
        STARLASU_METAMODEL.getEClass("GenericErrorNode"),
    )
val GenericNodeHandler =
    KolasuClassHandler(
        GenericNode::class,
        STARLASU_METAMODEL.getEClass("GenericNode"),
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
    get() =
        if (this.enclosingClass != null) {
            "${this.enclosingClass.simpleName}.${this.simpleName}"
        } else {
            this.simpleName
        }

class ResourceClassTypeHandler(
    val resource: Resource,
    val ownPackage: EPackage,
) : EClassTypeHandler {
    override fun canHandle(kclass: KClass<*>): Boolean = getPackage(packageName(kclass)) != null

    private fun getPackage(packageName: String): EPackage? =
        resource.contents.find { it is EPackage && it != ownPackage && it.name == packageName } as EPackage?

    override fun toEClass(
        kclass: KClass<*>,
        eClassProvider: ClassifiersProvider,
    ): EClass =
        getPackage(packageName(kclass))!!.eClassifiers.find {
            it is EClass && it.name == kclass.simpleName
        } as EClass? ?: throw NoClassDefFoundError(kclass.qualifiedName)

    override fun external(): Boolean = true
}

internal fun EPackage.hasClassifierNamed(name: String): Boolean = this.eClassifiers.any { it.name == name }

internal fun EPackage.classifierByName(name: String): EClassifier =
    this.eClassifiers.find { it.name == name } ?: throw IllegalArgumentException(
        "No classifier named $name was found",
    )
