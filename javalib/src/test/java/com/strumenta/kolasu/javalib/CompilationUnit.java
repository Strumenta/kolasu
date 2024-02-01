package com.strumenta.kolasu.javalib;

import com.strumenta.kolasu.model.FeatureDescription;
import com.strumenta.kolasu.model.Internal;
import com.strumenta.kolasu.model.Multiplicity;
import com.strumenta.kolasu.model.FeatureType;
import com.strumenta.kolasu.model.*;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

public class CompilationUnit extends Node {

    private List<A> as = new LinkedList<>();

    public List<A> getAs() {
        return as;
    }


    public static class A extends Node {
        private List<B> bs = new LinkedList<>();

        public List<B> getBs() {
            return bs;
        }

        @NotNull
        @Override
        @Internal
        public List<FeatureDescription> getProperties() {
            return Arrays.asList(new FeatureDescription("bs", true, Multiplicity.MANY, getBs(), FeatureType.CONTAINMENT, false));
        }
    }

    public static class B extends Node {

    }


    @NotNull
    @Override
    @Internal
    public List<FeatureDescription> getProperties() {
        return Arrays.asList(new FeatureDescription("as", true, Multiplicity.MANY, getAs(), FeatureType.CONTAINMENT, false));
    }
}
