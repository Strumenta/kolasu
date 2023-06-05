package com.strumenta.kolasu.model.observable

import io.reactivex.rxjava3.core.ObservableSource
import io.reactivex.rxjava3.core.Observer
import io.reactivex.rxjava3.disposables.Disposable

sealed class ListNotification<E>

data class ListAddition<E>(val added: E) : ListNotification<E>()
data class ListRemoval<E>(val removed: E) : ListNotification<E>()

class ObservableList<E>(private val base: MutableList<E> = mutableListOf()) :
    MutableList<E> by base,
    ObservableSource<ListNotification<E>>,
    Disposable {
    private val observers = mutableListOf<Observer<in ListNotification<E>>>()

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
        observers.forEach { it.onNext(ListAddition(element)) }
    }

    override fun add(element: E): Boolean {
        return if (base.add(element)) {
            observers.forEach { it.onNext(ListAddition(element)) }
            true
        } else {
            false
        }
    }

    override fun removeAt(index: Int): E {
        val element = base.removeAt(index)
        observers.forEach { it.onNext(ListRemoval(element)) }
        return element
    }

    override fun set(index: Int, element: E): E {
        val oldElement = base.set(index, element)
        observers.forEach { it.onNext(ListRemoval(oldElement)) }
        observers.forEach { it.onNext(ListAddition(element)) }
        return oldElement
    }

    override fun removeAll(elements: Collection<E>): Boolean {
        var changed = false
        elements.forEach { changed = changed || remove(it) }
        return changed
    }

    override fun remove(element: E): Boolean {
        return if (base.remove(element)) {
            observers.forEach { it.onNext(ListRemoval(element)) }
            true
        } else {
            false
        }
    }

    override fun subscribe(observer: Observer<in ListNotification<E>>) {
        observers.add(observer)
        observer.onSubscribe(this)
    }

    override fun dispose() {
        throw UnsupportedOperationException()
    }

    override fun isDisposed(): Boolean {
        return false
    }
}
