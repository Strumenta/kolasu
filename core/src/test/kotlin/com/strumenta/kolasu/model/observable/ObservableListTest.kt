package com.strumenta.kolasu.model.observable

import com.badoo.reaktive.disposable.Disposable
import com.badoo.reaktive.observable.ObservableObserver
import kotlin.test.Test
import kotlin.test.assertEquals

class MyListObserver : ObservableObserver<ListNotification<Int>> {
    val observations = mutableListOf<String>()

    override fun onSubscribe(d: Disposable) {
    }

    override fun onError(e: Throwable) {
    }

    override fun onComplete() {
    }

    override fun onNext(notification: ListNotification<Int>) {
        when (notification) {
            is ListAddition -> observations.add("added ${notification.added}")
            is ListRemoval -> observations.add("removed ${notification.removed}")
        }
    }
}

class ObservableListTest {
    @Test
    fun observeAdditionAndRemoval() {
        val li = ObservableList<Int>()
        val o = MyListObserver()
        li.subscribe(o)
        li.add(1)
        li.add(2)
        li.add(3)
        li.remove(2)
        li.remove(2)
        assertEquals(listOf("added 1", "added 2", "added 3", "removed 2"), o.observations)
    }
}
