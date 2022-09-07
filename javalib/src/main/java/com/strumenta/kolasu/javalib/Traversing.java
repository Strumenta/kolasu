package com.strumenta.kolasu.javalib;

import com.strumenta.kolasu.model.Node;
import com.strumenta.kolasu.model.Position;
import com.strumenta.kolasu.traversing.StructurallyKt;
import kotlin.jvm.internal.Reflection;
import kotlin.sequences.Sequence;

import java.util.Iterator;
import java.util.List;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Consumer;
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
        return asStream(StructurallyKt.walk(node));
    }

    /**
     * Performs a post-order (or leaves-first) node traversal starting with a given node.
     */
    public static Stream<Node> walkLeavesFirst(Node node) {
        return asStream(StructurallyKt.walkLeavesFirst(node));
    }

    public static Stream<Node> walkAncestors(Node node) {
        return asStream(StructurallyKt.walkAncestors(node));
    }

    public static Stream<Node> walkDescendantsBreadthFirst(Node node) {
        return asStream(StructurallyKt.walkDescendants(node, StructurallyKt::walk));
    }

    public static Stream<Node> walkDescendantsLeavesFirst(Node node) {
        return asStream(StructurallyKt.walkDescendants(node, StructurallyKt::walkLeavesFirst));
    }

    public static <N> Stream<N> walkDescendantsBreadthFirst(Node node, Class<N> clazz) {
        return asStream(StructurallyKt.walkDescendants(node, Reflection.createKotlinClass(clazz), StructurallyKt::walk));
    }

    public static <N> Stream<N> walkDescendantsLeavesFirst(Node node, Class<N> clazz) {
        return asStream(StructurallyKt.walkDescendants(node, Reflection.createKotlinClass(clazz), StructurallyKt::walkLeavesFirst));
    }

    public static void walk(Node node, Consumer<Node> consumer) {
        consumeSequence(StructurallyKt.walk(node), consumer);
    }

    /**
     * Performs a post-order (or leaves-first) node traversal starting with a given node.
     */
    public static void walkLeavesFirst(Node node, Consumer<Node> consumer) {
        consumeSequence(StructurallyKt.walkLeavesFirst(node), consumer);
    }

    public static void walkAncestors(Node node, Consumer<Node> consumer) {
        consumeSequence(StructurallyKt.walkAncestors(node), consumer);
    }

    public static void walkDescendantsBreadthFirst(Node node, Consumer<Node> consumer) {
        consumeSequence(StructurallyKt.walkDescendants(node, StructurallyKt::walk), consumer);
    }

    public static void walkDescendantsLeavesFirst(Node node, Consumer<Node> consumer) {
        consumeSequence(StructurallyKt.walkDescendants(node, StructurallyKt::walkLeavesFirst), consumer);
    }

    public static <N> void walkDescendantsBreadthFirst(Node node, Class<N> clazz, Consumer<Node> consumer) {
        consumeSequence(StructurallyKt.walkDescendants(node, Reflection.createKotlinClass(clazz), StructurallyKt::walk), consumer);
    }

    public static <N> void walkDescendantsLeavesFirst(Node node, Class<N> clazz, Consumer<Node> consumer) {
        consumeSequence(StructurallyKt.walkDescendants(node, Reflection.createKotlinClass(clazz), StructurallyKt::walkLeavesFirst), consumer);
    }

    /**
     * Walks the AST within the given position starting from the given node
     * and returns the result as sequence to consume.
     *
     * @param node     the node from which the walk should start
     * @param position the position within which the walk should remain
     */
    public static <N> void walkWithin(Node node, Position position, Consumer<Node> consumer) {
        consumeSequence(StructurallyKt.walkWithin(node, position), consumer);
    }

    /**
     * Walks the AST within the given position starting from each give node
     * and concatenates all results in a single sequence to consume.
     *
     * @param nodes    the nodes from which the walk should start
     * @param position the position within which the walk should remain
     */
    public static <N> void walkWithin(List<Node> nodes, Position position, Consumer<Node> consumer) {
        consumeSequence(StructurallyKt.walkWithin(nodes, position), consumer);
    }

    public static <T> T findAncestorOfType(Node node, Class<T> clazz) {
        return StructurallyKt.findAncestorOfType(node, clazz);
    }
}