package com.strumenta.kolasu.javalib;

import com.badoo.reaktive.observable.ObservableObserver;
import com.strumenta.kolasu.model.Node;
import com.strumenta.kolasu.model.NodeLike;
import com.strumenta.kolasu.model.NodeNotification;
import org.jetbrains.annotations.NotNull;

public class B extends Node {
    @Override
    public void subscribe(@NotNull ObservableObserver<? super NodeNotification<? super NodeLike>> observer) {
        throw new UnsupportedOperationException();
    }
}