package com.strumenta.kolasu.javalib;

import com.strumenta.kolasu.model.*;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class CompilationUnit extends BaseASTNode {

    public static class A extends BaseASTNode {
        private List<B> bs = new LinkedList<>();

        public List<B> getBs() {
            return bs;
        }

        @Override
        @NotNull
        public List<PropertyDescription> getOriginalProperties() {
            return Arrays.asList(new PropertyDescription("bs", true, Multiplicity.MANY, getBs(), PropertyType.CONTAINMENT, false));
        }

        @Override
        @NotNull
        public List<PropertyDescription> getDerivedProperties() {
            return Collections.emptyList();
        }
    }

    public static class B extends BaseASTNode {

    }

    private List<A> as = new LinkedList<>();

    public List<A> getAs() {
        return as;
    }

    @NotNull
    @Override
    @Internal
    public List<PropertyDescription> getProperties() {
        return Arrays.asList(new PropertyDescription("as", true, Multiplicity.MANY, getAs(), PropertyType.CONTAINMENT, false));
    }

    @Override
    @NotNull
    public List<PropertyDescription> getOriginalProperties() {
        return getProperties();
    }
}
