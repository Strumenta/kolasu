package com.strumenta.kolasu.emf

import com.strumenta.kolasu.model.NodeLike
import com.strumenta.kolasu.model.PropertyTypeDescription
import com.strumenta.kolasu.model.isANode
import com.strumenta.kolasu.model.processFeatures
import org.eclipse.emf.ecore.EClass
import org.eclipse.emf.ecore.EClassifier
import org.eclipse.emf.ecore.EDataType
import org.eclipse.emf.ecore.EEnum
import org.eclipse.emf.ecore.EGenericType
import org.eclipse.emf.ecore.EPackage
import org.eclipse.emf.ecore.ETypeParameter
import org.eclipse.emf.ecore.ETypedElement
import org.eclipse.emf.ecore.EcoreFactory
import org.eclipse.emf.ecore.resource.Resource
import java.util.LinkedList
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.KTypeParameter
import kotlin.reflect.KTypeProjection
import kotlin.reflect.KVariance
import kotlin.reflect.full.allSupertypes
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.full.superclasses
import kotlin.reflect.full.withNullability

private val KClass<*>.packageName: String?
    get() {
        val isInternal = this.java.name.contains('$')
        val qname =
            if (isInternal) {
                this
                    .java
                    .name
                    .split("$")
                    .first()
            } else {
                this.qualifiedName ?: throw IllegalStateException("The class has no qualified name: $this")
            }
        return if (qname == this.simpleName) {
            null
        } else {
            if (isInternal) {
                val last = qname.split(".").last()
                require(qname.endsWith(".$last"))
                qname.removeSuffix(".$last")
            } else {
                require(qname.endsWith(".${this.simpleName}"))
                qname.removeSuffix(".${this.simpleName}")
            }
        }
    }

/**
 * When building multiple related EPackages use MetamodelsBuilder instead.
 *
 * @param metamodelName this is the name of the metamodel, or the name assigned to the EPackage to be created
 * @param kotlinPackageName this is where we look for Kotlin classes. By default it coincides with the metamodel name
 *                          but in certain cases it may be different (for example having a suffix like model or ast)
 */
class MetamodelBuilder(
    metamodelName: String,
    nsURI: String,
    nsPrefix: String,
    resource: Resource? = null,
    val kotlinPackageName: String = metamodelName,
) : ClassifiersProvider {
    private val ePackage: EPackage = EcoreFactory.eINSTANCE.createEPackage()
    private val eClasses = HashMap<KClass<*>, EClass>()
    private val dataTypes = HashMap<KType, EDataType>()
    private val eclassTypeHandlers = LinkedList<EClassTypeHandler>()
    private val dataTypeHandlers = LinkedList<EDataTypeHandler>()
    internal var container: MetamodelsBuilder? = null

    init {
        ePackage.name = metamodelName
        ePackage.nsURI = nsURI
        ePackage.nsPrefix = nsPrefix
        if (resource == null) {
            ePackage.setResourceURI(nsURI)
        } else {
            resource.contents.add(ePackage)
            eclassTypeHandlers.add(ResourceClassTypeHandler(resource, ePackage))
        }

        dataTypeHandlers.add(StringHandler)
        dataTypeHandlers.add(CharHandler)
        dataTypeHandlers.add(BooleanHandler)
        dataTypeHandlers.add(IntHandler)
        dataTypeHandlers.add(IntegerHandler)
        dataTypeHandlers.add(FloatHandler)
        dataTypeHandlers.add(DoubleHandler)
        dataTypeHandlers.add(LongHandler)
        dataTypeHandlers.add(BigIntegerHandler)
        dataTypeHandlers.add(BigDecimalHandler)

        eclassTypeHandlers.add(LocalDateHandler)
        eclassTypeHandlers.add(LocalTimeHandler)
        eclassTypeHandlers.add(LocalDateTimeHandler)

        eclassTypeHandlers.add(NodeHandler)
        eclassTypeHandlers.add(NodeLikeHandler)
        eclassTypeHandlers.add(NamedHandler)
        eclassTypeHandlers.add(RangeHandler)
        eclassTypeHandlers.add(PossiblyNamedHandler)
        eclassTypeHandlers.add(ReferenceValueHandler)
        eclassTypeHandlers.add(ResultHandler)

        eclassTypeHandlers.add(StatementHandler)
        eclassTypeHandlers.add(ExpressionHandler)
        eclassTypeHandlers.add(EntityDeclarationHandler)
        eclassTypeHandlers.add(EntityGroupDeclarationHandler)
        eclassTypeHandlers.add(TypeAnnotationHandler)
        eclassTypeHandlers.add(BehaviorDeclarationHandler)
        eclassTypeHandlers.add(ParameterHandler)
        eclassTypeHandlers.add(DocumentationHandler)
        eclassTypeHandlers.add(PlaceholderElementHandler)

        eclassTypeHandlers.add(ErrorNodeHandler)
        eclassTypeHandlers.add(GenericErrorNodeHandler)
        eclassTypeHandlers.add(GenericNodeHandler)
    }

    /**
     * Normally a class is not treated as a DataType, so we need specific DataTypeHandlers
     * to recognize it as such
     */
    fun addDataTypeHandler(eDataTypeHandler: EDataTypeHandler) {
        dataTypeHandlers.add(eDataTypeHandler)
    }

    /**
     * This should be needed only to customize how we want to deal with a class when translating
     * it to an EClass
     */
    fun addEClassTypeHandler(eClassTypeHandler: EClassTypeHandler) {
        eclassTypeHandlers.add(eClassTypeHandler)
    }

    private fun createEEnum(kClass: KClass<out Enum<*>>): EEnum {
        val eEnum = EcoreFactory.eINSTANCE.createEEnum()
        eEnum.name = kClass.eClassifierName
        kClass.java.enumConstants.forEach {
            val eLiteral = EcoreFactory.eINSTANCE.createEEnumLiteral()
            eLiteral.name = it.name
            eLiteral.value = it.ordinal
            eEnum.eLiterals.add(eLiteral)
        }
        return eEnum
    }

    override fun provideDataType(ktype: KType): EDataType? {
        if (!dataTypes.containsKey(ktype)) {
            val eDataType: EDataType
            var external = false
            when {
                (ktype.classifier as? KClass<*>)?.isSubclassOf(Enum::class) == true -> {
                    eDataType = createEEnum(ktype.classifier as KClass<out Enum<*>>)
                }

                else -> {
                    val handler = dataTypeHandlers.find { it.canHandle(ktype) }
                    if (handler == null) {
                        // throw RuntimeException("Unable to handle data type $ktype, with classifier ${ktype.classifier}")\
                        return null
                    } else {
                        external = handler.external()
                        eDataType = handler.toDataType(ktype)
                    }
                }
            }
            if (!external) {
                ensureClassifierNameIsNotUsed(eDataType)
                ePackage.eClassifiers.add(eDataType)
            }
            dataTypes[ktype] = eDataType
        }
        return dataTypes[ktype]!!
    }

    private fun classToEClass(kClass: KClass<*>): EClass {
        if (kClass == Any::class) {
            return EcoreFactory.eINSTANCE.ecorePackage.eObject
        }

        if (kClass.packageName != kotlinPackageName) {
            if (container != null) {
                for (sibling in container!!.singleMetamodelsBuilders) {
                    if (sibling.canProvideClass(kClass)) {
                        return sibling.provideClass(kClass)
                    }
                }
            }
            throw Error(
                "This class does not belong to this EPackage: ${kClass.qualifiedName}. " +
                    "This EPackage: ${this.ePackage.name}. Kotlin Package Name: $kotlinPackageName",
            )
        }

        val eClass = EcoreFactory.eINSTANCE.createEClass()
        // This is necessary because some classes refer to themselves
        registerKClassForEClass(kClass, eClass)

        kClass.superclasses.forEach {
            if (it != Any::class &&
                (!it.java.isInterface || it.allSupertypes.map { it.classifier }.contains(NodeLike::class))
            ) {
                eClass.eSuperTypes.add(provideClass(it))
            }
        }
        eClass.name = kClass.eClassifierName

        eClass.isAbstract = kClass.isAbstract || kClass.isSealed
        eClass.isInterface = kClass.java.isInterface

        kClass.typeParameters.forEach { kTypeParameter: KTypeParameter ->
            eClass.eTypeParameters.add(
                EcoreFactory.eINSTANCE.createETypeParameter().apply {
                    // TODO consider bounds, taking in account that in Kotlin we have variance (in/out)
                    // which may not exactly correspond to how bounds work in EMF
                    name = kTypeParameter.name
                },
            )
        }

        kClass.processFeatures { prop ->
            try {
                if (eClass.eAllStructuralFeatures.any { sf -> sf.name == prop.name }) {
                    // skip
                } else {
                    // do not process inherited properties
                    val valueType = prop.valueType
                    if (prop.provideNodes) {
                        registerReference(prop, valueType, eClass)
                    } else {
                        val nullable = prop.valueType.isMarkedNullable
                        val dataType = provideDataType(prop.valueType.withNullability(false))
                        if (dataType == null) {
                            // We can treat it like a class
                            registerReference(prop, valueType, eClass)
                        } else {
                            val ea = EcoreFactory.eINSTANCE.createEAttribute()
                            ea.name = prop.name
                            if (prop.multiple) {
                                ea.lowerBound = 0
                                ea.upperBound = -1
                            } else {
                                ea.lowerBound = if (nullable) 0 else 1
                                ea.upperBound = 1
                            }
                            ea.eType = dataType
                            eClass.eStructuralFeatures.add(ea)
                        }
                    }
                }
            } catch (e: Exception) {
                throw RuntimeException("Issue processing property $prop in class $kClass", e)
            }
        }
        val featuresNames = mutableSetOf<String>()
        eClass.eAllStructuralFeatures.forEach { f ->
            if (featuresNames.contains(f.name)) {
                throw IllegalStateException("Duplicate Feature with name ${f.name} in ${eClass.name}")
            } else {
                featuresNames.add(f.name)
            }
        }

        return eClass
    }

    private fun registerReference(
        prop: PropertyTypeDescription,
        valueType: KType,
        eClass: EClass,
    ) {
        if (valueType.classifier is KClass<*> && (valueType.classifier as KClass<*>).isSubclassOf(Collection::class)) {
            throw IllegalStateException("We do not support references to lists. EClass $eClass, property $prop")
        }

        val ec = EcoreFactory.eINSTANCE.createEReference()
        ec.name = prop.name
        if (prop.multiple) {
            ec.lowerBound = 0
            ec.upperBound = -1
        } else {
            ec.lowerBound = 0
            ec.upperBound = 1
        }
        // Note: it's the reference that's a child here, not the referred object.
        // We represent references as ReferenceByName instances, so that we can also retain information about
        // the name of the referred node in case the reference couldn't be resolved.
        ec.isContainment = true
        // No type parameters on methods should be allowed elsewhere and only the type parameters
        // on the class should be visible. We are not expecting containing classes to expose
        // type parameters
        val visibleTypeParameters = eClass.eTypeParameters.associateBy { it.name }
        setType(ec, valueType, visibleTypeParameters)
        eClass.eStructuralFeatures.add(ec)
    }

    private fun provideType(valueType: KTypeProjection): EGenericType {
        when (valueType.variance) {
            KVariance.INVARIANT -> {
                return provideType(valueType.type!!)
            }

            else -> TODO("Variance ${valueType.variance} not yet sypported")
        }
    }

    private fun provideType(valueType: KType): EGenericType {
        val dataType = provideDataType(valueType.withNullability(false))
        if (dataType != null) {
            return EcoreFactory.eINSTANCE.createEGenericType().apply {
                eClassifier = dataType
            }
        }
        if (valueType.arguments.isNotEmpty()) {
            TODO("Not yet supported: type arguments in $valueType")
        }
        if (valueType.classifier is KClass<*>) {
            return EcoreFactory.eINSTANCE.createEGenericType().apply {
                eClassifier = provideClass(valueType.classifier as KClass<*>)
            }
        } else {
            TODO("Not yet supported: ${valueType.classifier}")
        }
    }

    private fun setType(
        element: ETypedElement,
        valueType: KType,
        visibleTypeParameters: Map<String, ETypeParameter>,
    ) {
        when (val classifier = valueType.classifier) {
            is KClass<*> -> {
                if (classifier.typeParameters.isEmpty()) {
                    element.eType = provideClass(classifier)
                } else {
                    element.eGenericType =
                        EcoreFactory.eINSTANCE.createEGenericType().apply {
                            eClassifier = provideClass(classifier)
                            require(classifier.typeParameters.size == valueType.arguments.size)
                            eTypeArguments.addAll(
                                valueType.arguments.map {
                                    provideType(it)
                                },
                            )
                        }
                }
            }

            is KTypeParameter -> {
                element.eGenericType =
                    EcoreFactory.eINSTANCE.createEGenericType().apply {
                        eTypeParameter = visibleTypeParameters[classifier.name]
                            ?: throw IllegalStateException("Type parameter not found")
                    }
            }

            else -> throw Error("Not a valid classifier: $classifier")
        }
    }

    private fun ensureClassifierNameIsNotUsed(classifier: EClassifier) {
        if (ePackage.hasClassifierNamed(classifier.name)) {
            throw IllegalStateException(
                "There is already a Classifier named ${classifier.name}: ${ePackage.classifierByName(classifier.name)}",
            )
        }
    }

    private fun registerKClassForEClass(
        kClass: KClass<*>,
        eClass: EClass,
    ) {
        if (eClasses.containsKey(kClass)) {
            require(eClasses[kClass] == eClass)
        } else {
            eClasses[kClass] = eClass
        }
    }

    fun canProvideClass(kClass: KClass<*>): Boolean {
        if (eClasses.containsKey(kClass)) {
            return true
        }
        if (eclassTypeHandlers.any { it.canHandle(kClass) }) {
            return true
        }
        if (kClass == Any::class) {
            return true
        }

        return kClass.packageName == this.kotlinPackageName
    }

    override fun provideClass(kClass: KClass<*>): EClass {
        if (!eClasses.containsKey(kClass)) {
            val ch = eclassTypeHandlers.find { it.canHandle(kClass) }
            val eClass = ch?.toEClass(kClass, this) ?: classToEClass(kClass)
            if (kClass.packageName != this.kotlinPackageName) {
                return eClass
            }
            if (ch == null || !ch.external()) {
                ensureClassifierNameIsNotUsed(eClass)
                ePackage.eClassifiers.add(eClass)
            }
            registerKClassForEClass(kClass, eClass)
            if (kClass.isSealed) {
                kClass.sealedSubclasses.forEach {
                    queue.add(it)
                }
            }
            kClass.nestedClasses.forEach {
                if (it.isANode()) {
                    queue.add(it)
                }
            }
        }
        while (queue.isNotEmpty()) {
            provideClass(queue.removeFirst())
        }
        return eClasses[kClass]!!
    }

    private val queue = LinkedList<KClass<*>>()

    fun generate(): EPackage = ePackage
}
