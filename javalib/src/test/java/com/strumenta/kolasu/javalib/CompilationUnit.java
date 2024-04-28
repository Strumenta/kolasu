package com.strumenta.kolasu.javalib;

import com.badoo.reaktive.observable.ObservableObserver;
import com.strumenta.kolasu.model.*;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedList;
import java.util.List;

public class CompilationUnit extends Node {

    private List<A> as = new LinkedList<>();

    public List<A> getAs() {
        return as;
    }
    @Override
    public void subscribe(@NotNull ObservableObserver<? super NodeNotification<? super NodeLike>> observer) {
        throw new UnsupportedOperationException();
    }

}
