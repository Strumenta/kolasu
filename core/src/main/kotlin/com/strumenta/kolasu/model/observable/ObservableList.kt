package com.strumenta.kolasu.model.observable

import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.ObservableSource
import io.reactivex.rxjava3.core.Observer
import io.reactivex.rxjava3.disposables.Disposable

//interface ListObserver<E> {
//    fun added(e: E)
//    fun removed(e: E)
//}

sealed class ListNotification<E>

data class ListAddition<E>(val added: E): ListNotification<E>()
data class ListRemoval<E>(val removed: E): ListNotification<E>()

class ObservableList<E>(private val base: MutableList<E> = mutableListOf()) : MutableList<E> by base,
    ObservableSource<ListNotification<E>>, Disposable {
    private val observers = mutableListOf<Observer<in ListNotification<E>>>()

//    fun registerObserver(observer: ListObserver<in E>) {
//        observers.add(observer)
//    }

    override fun addAll(elements: Collection<E>): Boolean {
        var modified = false
        elements.forEach { element ->
            modified = add(element) || modified
        }
        return modified
    }

    override fun addAll(index: Int, elements: Collection<E>): Boolean {
        TODO("Not yet implemented")
    }

    override fun add(index: Int, element: E) {
        TODO("Not yet implemented")
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
        TODO("Not yet implemented")
    }

    override fun set(index: Int, element: E): E {
        TODO("Not yet implemented")
    }

    override fun removeAll(elements: Collection<E>): Boolean {
        TODO("Not yet implemented")
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
