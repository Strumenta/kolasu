package com.strumenta.kolasu.model

import com.badoo.reaktive.disposable.Disposable
import com.badoo.reaktive.observable.ObservableObserver
import com.badoo.reaktive.observable.filter
import com.badoo.reaktive.observable.map
import com.badoo.reaktive.subject.getObserver
import com.badoo.reaktive.subject.publish.PublishSubject
import com.strumenta.kolasu.model.observable.ReferenceSet
import com.strumenta.kolasu.model.observable.ReferencedToAdded
import com.strumenta.kolasu.model.observable.ReferencedToRemoved
import java.io.Serializable
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1
import kotlin.reflect.KType
import kotlin.reflect.KTypeProjection
import kotlin.reflect.KVariance
import kotlin.reflect.full.createType
import kotlin.reflect.full.isSubtypeOf

/**
 * An entity that can have a name
 */
@NodeType
interface PossiblyNamed {
    /**
     * The optional name of the entity.
     */
    val name: String?
}

/**
 * An entity which has a name.
 */
@NodeType
interface Named : PossiblyNamed {
    /**
     * The mandatory name of the entity.
     */
    override val name: String
}

data class ReferenceChangeNotification<N : PossiblyNamed>(
    val oldValue: N?,
    val newValue: N?,
)

/**
 * A reference associated by using a name.
 * It can be used only to refer to Nodes and not to other values.
 *
 * This is not statically enforced as we may want to use some interface, which cannot extend Node.
 * However, this is enforced dynamically.
 */
class ReferenceByName<N>(
    val name: String,
    initialReferred: N? = null,
) : Serializable where N : PossiblyNamed {
    val changes = PublishSubject<ReferenceChangeNotification<N>>()

    var referred: N? = null
        set(value) {
            require(value is INode || value == null) {
                "We cannot enforce it statically but only Node should be referred to. Instead $value was assigned " +
                    "(class: ${value?.javaClass})"
            }
            changes.onNext(ReferenceChangeNotification(field, value))
            field = value
        }

    private var container: INode? = null
    private var referenceName: String? = null

    /**
     * When we instantiate the reference by name, we wired it so that changes to the reference by name will
     * be propagated to the owner of the reference, so that the observers of the owner will be notified
     * of changes in the reference.
     */
    fun setContainer(
        node: INode,
        referenceName: String,
    ) {
        if (container == node) {
            return
        }
        if (container != null) {
            throw IllegalStateException("$this is already contained in $container as ${this.referenceName}")
        }
        changes
            .map {
                // When the reference is changed, we propagate it to the container
                ReferenceSet(node, referenceName, it.oldValue as INode?, it.newValue as INode?)
            }.subscribe(node.changes.getObserver { })
        changes
            .filter { it.oldValue != null }
            .map { ReferencedToRemoved(it.oldValue as INode, referenceName, node) }
            .subscribe(
                object : ObservableObserver<ReferencedToRemoved<INode>> {
                    override fun onComplete() {
                        TODO("Not yet implemented")
                    }

                    override fun onError(error: Throwable) {
                        TODO("Not yet implemented")
                    }

                    override fun onNext(value: ReferencedToRemoved<INode>) {
                        value.node.changes.onNext(value)
                    }

                    override fun onSubscribe(disposable: Disposable) {
                    }
                },
            )
        changes
            .filter { it.newValue != null }
            .map { ReferencedToAdded(it.newValue as INode, referenceName, node) }
            .subscribe(
                object : ObservableObserver<ReferencedToAdded<INode>> {
                    override fun onComplete() {
                        TODO("Not yet implemented")
                    }

                    override fun onError(error: Throwable) {
                        TODO("Not yet implemented")
                    }

                    override fun onNext(value: ReferencedToAdded<INode>) {
                        value.node.changes.onNext(value)
                    }

                    override fun onSubscribe(disposable: Disposable) {
                    }
                },
            )
        container = node
        this.referenceName = referenceName
    }

    init {
        this.referred = initialReferred
    }

    override fun toString(): String {
        return if (isResolved) {
            "Ref($name)[Solved]"
        } else {
            "Ref($name)[Unsolved]"
        }
    }

    val isResolved: Boolean
        get() = referred != null

    override fun hashCode(): Int {
        return name.hashCode() * (7 + if (isResolved) 2 else 1)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ReferenceByName<*>) return false

        if (name != other.name) return false
        if (referred != other.referred) return false

        return true
    }
}

/**
 * Try to resolve the reference by finding a named element with a matching name.
 * The name match is performed in a case-sensitive or insensitive way depending on the value of @param[caseInsensitive].
 */
fun <N> ReferenceByName<N>.tryToResolve(
    candidates: Iterable<N>,
    caseInsensitive: Boolean = false,
): Boolean where N : PossiblyNamed {
    val res: N? = candidates.find { if (it.name == null) false else it.name.equals(this.name, caseInsensitive) }
    this.referred = res
    return res != null
}

/**
 * Try to resolve the reference by assigning [possibleValue]. The assignment is not performed if
 * [possibleValue] is null.
 *
 * @param possibleValue the candidate value.
 * @return true if the assignment has been performed
 */
fun <N> ReferenceByName<N>.tryToResolve(possibleValue: N?): Boolean where N : PossiblyNamed =
    if (possibleValue == null) {
        false
    } else {
        this.referred = possibleValue
        true
    }

/**
 * Typealias representing reference properties.
 **/
typealias KReferenceByName<S> = KProperty1<S, ReferenceByName<out PossiblyNamed>?>

/**
 * Builds a type representation for a reference
 **/
fun kReferenceByNameType(targetClass: KClass<out PossiblyNamed> = PossiblyNamed::class): KType =
    ReferenceByName::class.createType(
        arguments = listOf(KTypeProjection(variance = KVariance.OUT, type = targetClass.createType())),
        nullable = true,
    )

/**
 * Retrieves the referred type for a given reference property.
 **/
@Suppress("unchecked_cast")
fun KReferenceByName<*>.getReferredType(): KClass<out PossiblyNamed> =
    this
        .returnType
        .arguments[0]
        .type!!
        .classifier!! as KClass<out PossiblyNamed>

/**
 * Retrieves all reference properties for a given node.
 **/
fun INode.kReferenceByNameProperties(targetClass: KClass<out PossiblyNamed> = PossiblyNamed::class) =
    this.nodeProperties.filter { it.returnType.isSubtypeOf(kReferenceByNameType(targetClass)) }
