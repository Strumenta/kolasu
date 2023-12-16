package com.strumenta.kolasu.model.observable

import com.badoo.reaktive.observable.ObservableObserver
import com.badoo.reaktive.subject.publish.PublishSubject

sealed class ListNotification<E>

data class ListAddition<E>(val added: E) : ListNotification<E>()
data class ListRemoval<E>(val removed: E) : ListNotification<E>()

class ObservableList<E>(private val base: MutableList<E> = mutableListOf()) :
    MutableList<E> by base {
    val changes = PublishSubject<ListNotification<E>>()

    override fun addAll(elements: Collection<E>): Boolean {
        var modified = false
        elements.forEach { element ->
            modified = add(element) || modified
        }
        return modified
    }

    override fun addAll(index: Int, elements: Collection<E>): Boolean {
        var currIndex = index
        for (element in elements) {
            add(currIndex, element)
            currIndex++
        }
        return true
    }

    override fun add(index: Int, element: E) {
        base.add(index, element)
        changes.onNext(ListAddition(element))
    }

    override fun add(element: E): Boolean {
        return if (base.add(element)) {
            changes.onNext(ListAddition(element))
            true
        } else {
            false
        }
    }

    override fun removeAt(index: Int): E {
        val element = base.removeAt(index)
        changes.onNext(ListRemoval(element))
        return element
    }

    override fun set(index: Int, element: E): E {
        val oldElement = base.set(index, element)
        changes.onNext(ListRemoval(oldElement))
        changes.onNext(ListAddition(element))
        return oldElement
    }

    override fun removeAll(elements: Collection<E>): Boolean {
        var changed = false
        elements.forEach { changed = changed || remove(it) }
        return changed
    }

    override fun remove(element: E): Boolean {
        return if (base.remove(element)) {
            changes.onNext(ListRemoval(element))
            true
        } else {
            false
        }
    }

    fun subscribe(observer: ObservableObserver<in ListNotification<E>>) {
        changes.subscribe(observer)
    }
}
