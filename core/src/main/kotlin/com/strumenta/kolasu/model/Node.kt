package com.strumenta.kolasu.model

import com.badoo.reaktive.observable.ObservableObserver
import com.badoo.reaktive.subject.publish.PublishSubject
import com.strumenta.kolasu.language.Concept
import com.strumenta.kolasu.language.Property
import com.strumenta.kolasu.language.StarLasuLanguage
import com.strumenta.kolasu.language.StarLasuLanguagesRegistry
import com.strumenta.kolasu.transformation.GenericNode
import com.strumenta.kolasu.traversing.walk
import kotlin.reflect.KMutableProperty

/**
 * The Abstract Syntax Tree will be constituted by instances of Node.
 */
open class Node : NodeLike {
    @property:Internal
    override val changes = PublishSubject<NodeNotification<in NodeLike>>()

    @Internal
    private val annotationInstances: MutableList<AnnotationInstance> = mutableListOf()

    @Internal
    override val destinations = mutableListOf<Destination>()

    constructor()

    constructor(range: Range?) : this() {
        this.range = range
    }

    constructor(origin: Origin?) : this() {
        this.origin = origin
    }

    /**
     * The origin from which this AST Node has been generated, if any.
     */
    @property:Internal
    override var origin: Origin? = null

    /**
     * The parent node, if any.
     */
    @property:Internal
    override var parent: NodeLike? = null

    /**
     * The range of this node in the source text.
     * If a range has been provided when creating this node, it is returned.
     * Otherwise, the value of this property is the range of the origin, if any.
     */
    @property:Internal
    override var range: Range?
        get() = origin?.range
        set(value) {
            if (origin == null) {
                if (value != null) {
                    origin = SimpleOrigin(value)
                }
            } else {
                origin!!.range = value
            }
        }

    private var explicitlySetSource: Source? = null

    @property:Internal
    override var source: Source?
        get() = explicitlySetSource ?: origin?.source
        set(value) {
            // This is a limit of the current API: to specify a Source we need to specify coordinates
            if (this.range == null) {
                explicitlySetSource = value
            } else {
                this.origin = SimpleOrigin(this.range!!.copy(source = value))
            }
            require(this.source === value)
        }

    fun setSourceForTree(source: Source): NodeLike {
        this.source = source
        this.walk().forEach {
            it.source = source
        }
        return this
    }

    /**
     * The source text for this node
     */
    @Internal
    override val sourceText: String?
        get() = origin?.sourceText

    /**
     * This must be final because otherwise data classes extending this will automatically generate
     * their own implementation. If Link properties are present it could lead to stack overflows in case
     * of circular graphs.
     */
    final override fun toString(): String {
        if (this is GenericNode) {
            return "GenericNode"
        }
        return "${this.qualifiedNodeType}(${concept.declaredFeatures.joinToString(
            ", ",
        ) { "${it.name}=${it.valueToString(this)}" }})"
    }

    protected fun notifyOfPropertyChange(
        property: Property,
        oldValue: Any?,
        newValue: Any?,
    ) {
        changes.onNext(PropertyChangedNotification(this, property, oldValue, newValue))
    }

    @Internal
    override val allAnnotationInstances: List<AnnotationInstance>
        get() = annotationInstances

    override fun <A : AnnotationInstance> addAnnotation(annotation: A): A {
        if (annotation.annotatedNode != null) {
            throw java.lang.IllegalStateException("Annotation already attached")
        }
        annotation.attachTo(this)
        if (annotation.isSingle) {
            annotationInstances.filter { it.annotation == annotation.annotation }.forEach { removeAnnotation(it) }
        }
        annotationInstances.add(annotation)
        return annotation
    }

    override fun removeAnnotation(annotationInstance: AnnotationInstance) {
        require(annotationInstance.annotatedNode == this)
        annotationInstances.remove(annotationInstance)
        annotationInstance.detach()
    }

    fun getAttributeValue(name: String): Any? {
        // In the future, when we set AttributeValue it will be different
        return getProperty(name)
    }

    override fun <T : Any?> setProperty(
        propertyName: String,
        value: T,
    ) {
        val prop = nodeProperties.find { it.name == propertyName } as KMutableProperty<T>
        prop.setter.call(this, value)
    }

    override fun <T : Any?> getProperty(propertyName: String): T {
        val prop = nodeProperties.find { it.name == propertyName }!!
        return prop.call(this) as T
    }

    override fun <T : NodeLike> getContainmentValue(containmentName: String): List<T> {
        val prop = nodeProperties.find { it.name == containmentName }!!
        return when (val res = prop.call(this)) {
            null -> {
                emptyList()
            }

            is List<*> -> {
                res as List<T>
            }

            else -> {
                listOf(res as T)
            }
        }
    }

    override fun <T : NodeLike> addToContainment(
        containmentName: String,
        child: T,
    ) {
        val prop = nodeProperties.find { it.name == containmentName }!!
        val value = prop.call(this)
        if (value is List<*>) {
            (value as MutableList<T>).add(child)
        } else {
            (prop as KMutableProperty<T>).setter.call(this, child)
        }
    }

    override fun <T : NodeLike> removeFromContainment(
        containmentName: String,
        child: T,
    ) {
        val prop = nodeProperties.find { it.name == containmentName }!!
        val value = prop.call(this)
        if (value is List<*>) {
            (value as MutableList<T>).remove(child)
        } else {
            if (value == child) {
                (prop as KMutableProperty<T>).setter.call(this, null)
            }
        }
    }

    override fun <T : PossiblyNamed> setReferenceReferred(
        referenceName: String,
        referred: T,
    ) {
        val ref: ReferenceValue<T> = getReference(referenceName)
        ref.referred = referred
    }

    override fun subscribe(observer: ObservableObserver<NodeNotification<in NodeLike>>) {
        this.changes.subscribe(observer)
    }

    @property:Internal
    override val concept: Concept
        get() {
            try {
                val annotation = this.javaClass.getAnnotation(LanguageAssociation::class.java)
                val language: StarLasuLanguage =
                    if (annotation != null) {
                        annotation.language.objectInstance ?: throw IllegalStateException()
                    } else {
                        StarLasuLanguagesRegistry.getLanguage(this.javaClass.kotlin.packageName)
                    }
                return language.getConcept(this.javaClass.simpleName)
            } catch (e: Exception) {
                throw RuntimeException("Issue while retrieving concept for class ${this.javaClass.canonicalName}", e)
            }
        }
}
