package com.strumenta.kolasu.model.observable

interface ListObserver<E> {
    fun added(e: E)
    fun removed(e: E)
}

class ObservableList<E>(private val base : MutableList<E> = mutableListOf()) : MutableList<E> by base {
    private val observers = mutableListOf<ListObserver<in E>>()

    fun registerObserver(observer: ListObserver<in E>) {
        observers.add(observer)
    }

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
            observers.forEach { it.added(element) }
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
            observers.forEach { it.removed(element) }
            true
        } else {
            false
        }
    }

}