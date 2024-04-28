package com.strumenta.kolasu.javalib;

import com.badoo.reaktive.observable.ObservableObserver;
import com.strumenta.kolasu.model.*;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedList;
import java.util.List;

public class A extends Node {
    private List<B> bs = new LinkedList<>();

    public List<B> getBs() {
        return bs;
    }

    @Override
    public void subscribe(@NotNull ObservableObserver<? super NodeNotification<? super NodeLike>> observer) {
        throw new UnsupportedOperationException();
    }
}