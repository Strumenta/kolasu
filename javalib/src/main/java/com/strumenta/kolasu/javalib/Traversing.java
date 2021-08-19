package com.strumenta.kolasu.javalib;

import com.strumenta.kolasu.model.Node;
import com.strumenta.kolasu.model.TraversingKt;
import kotlin.jvm.internal.Reflection;
import kotlin.reflect.KClass;
import kotlin.reflect.KType;
import kotlin.sequences.Sequence;
import org.jetbrains.annotations.NotNull;

import java.util.Comparator;
import java.util.Iterator;
import java.util.Optional;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.BinaryOperator;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.function.ToDoubleFunction;
import java.util.function.ToIntFunction;
import java.util.function.ToLongFunction;
import java.util.stream.Collector;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class Traversing {

    private static <T> void consumeSequence(Sequence<T> sequence, Consumer<T> consumer) {
        for (Iterator<T> it = sequence.iterator(); it.hasNext();) {
            consumer.accept(it.next());
        }
    }

    private static <T> Stream<T> asStream(Sequence<T> sequence) {
        return StreamSupport.stream(Spliterators.spliteratorUnknownSize(sequence.iterator(),
                Spliterator.ORDERED), false);
    }

    public static Stream<Node> walk(Node node) {
        return asStream(TraversingKt.walk(node));
    }

    /**
     * Performs a post-order (or leaves-first) node traversal starting with a given node.
     */
    public static Stream<Node> walkLeavesFirst(Node node) {
        return asStream(TraversingKt.walkLeavesFirst(node));
    }

    public static Stream<Node> walkAncestors(Node node) {
        return asStream(TraversingKt.walkAncestors(node));
    }

    public static Stream<Node> walkDescendantsBreadthFirst(Node node) {
        return asStream(TraversingKt.walkDescendants(node, TraversingKt::walk));
    }

    public static Stream<Node> walkDescendantsLeavesFirst(Node node) {
        return asStream(TraversingKt.walkDescendants(node, TraversingKt::walkLeavesFirst));
    }

    public static <N> Stream<N> walkDescendantsBreadthFirst(Node node, Class<N> clazz) {
        return asStream(TraversingKt.walkDescendants(node, Reflection.createKotlinClass(clazz), TraversingKt::walk));
    }

    public static <N> Stream<N> walkDescendantsLeavesFirst(Node node, Class<N> clazz) {
        return asStream(TraversingKt.walkDescendants(node, Reflection.createKotlinClass(clazz), TraversingKt::walkLeavesFirst));
    }

    public static void walk(Node node, Consumer<Node> consumer) {
        consumeSequence(TraversingKt.walk(node), consumer);
    }

    /**
     * Performs a post-order (or leaves-first) node traversal starting with a given node.
     */
    public static void walkLeavesFirst(Node node, Consumer<Node> consumer) {
        consumeSequence(TraversingKt.walkLeavesFirst(node), consumer);
    }

    public static void walkAncestors(Node node, Consumer<Node> consumer) {
        consumeSequence(TraversingKt.walkAncestors(node), consumer);
    }

    public static void walkDescendantsBreadthFirst(Node node, Consumer<Node> consumer) {
        consumeSequence(TraversingKt.walkDescendants(node, TraversingKt::walk), consumer);
    }

    public static void walkDescendantsLeavesFirst(Node node, Consumer<Node> consumer) {
        consumeSequence(TraversingKt.walkDescendants(node, TraversingKt::walkLeavesFirst), consumer);
    }

    public static <N> void walkDescendantsBreadthFirst(Node node, Class<N> clazz, Consumer<Node> consumer) {
        consumeSequence(TraversingKt.walkDescendants(node, Reflection.createKotlinClass(clazz), TraversingKt::walk), consumer);
    }

    public static <N> void walkDescendantsLeavesFirst(Node node, Class<N> clazz, Consumer<Node> consumer) {
        consumeSequence(TraversingKt.walkDescendants(node, Reflection.createKotlinClass(clazz), TraversingKt::walkLeavesFirst), consumer);
    }

    public static <T> T findAncestorOfType(Node node, Class<T> clazz) {
        return TraversingKt.findAncestorOfType(node, clazz);
    }
}