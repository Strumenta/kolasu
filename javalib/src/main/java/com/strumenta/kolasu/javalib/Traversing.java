package com.strumenta.kolasu.javalib;

import com.strumenta.kolasu.model.INode;
import com.strumenta.kolasu.model.Range;
import com.strumenta.kolasu.traversing.ProcessingByRange;
import com.strumenta.kolasu.traversing.ProcessingStructurally;
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

    public static Stream<INode> walk(INode node) {
        return asStream(ProcessingStructurally.walk(node));
    }

    /**
     * Performs a post-order (or leaves-first) node traversal starting with a given node.
     */
    public static Stream<INode> walkLeavesFirst(INode node) {
        return asStream(ProcessingStructurally.walkLeavesFirst(node));
    }

    public static Stream<INode> walkAncestors(INode node) {
        return asStream(ProcessingStructurally.walkAncestors(node));
    }

    public static Stream<INode> walkDescendantsBreadthFirst(INode node) {
        return asStream(ProcessingStructurally.walkDescendants(node, ProcessingStructurally::walk));
    }

    public static Stream<INode> walkDescendantsLeavesFirst(INode node) {
        return asStream(ProcessingStructurally.walkDescendants(node, ProcessingStructurally::walkLeavesFirst));
    }

    public static <N> Stream<N> walkDescendantsBreadthFirst(INode node, Class<N> clazz) {
        return asStream(ProcessingStructurally.walkDescendants(node, Reflection.createKotlinClass(clazz), ProcessingStructurally::walk));
    }

    public static <N> Stream<N> walkDescendantsLeavesFirst(INode node, Class<N> clazz) {
        return asStream(ProcessingStructurally.walkDescendants(node, Reflection.createKotlinClass(clazz), ProcessingStructurally::walkLeavesFirst));
    }

    public static void walk(INode node, Consumer<INode> consumer) {
        consumeSequence(ProcessingStructurally.walk(node), consumer);
    }

    /**
     * Performs a post-order (or leaves-first) node traversal starting with a given node.
     */
    public static void walkLeavesFirst(INode node, Consumer<INode> consumer) {
        consumeSequence(ProcessingStructurally.walkLeavesFirst(node), consumer);
    }

    public static void walkAncestors(INode node, Consumer<INode> consumer) {
        consumeSequence(ProcessingStructurally.walkAncestors(node), consumer);
    }

    public static void walkDescendantsBreadthFirst(INode node, Consumer<INode> consumer) {
        consumeSequence(ProcessingStructurally.walkDescendants(node, ProcessingStructurally::walk), consumer);
    }

    public static void walkDescendantsLeavesFirst(INode node, Consumer<INode> consumer) {
        consumeSequence(ProcessingStructurally.walkDescendants(node, ProcessingStructurally::walkLeavesFirst), consumer);
    }

    public static <N> void walkDescendantsBreadthFirst(INode node, Class<N> clazz, Consumer<INode> consumer) {
        consumeSequence(ProcessingStructurally.walkDescendants(node, Reflection.createKotlinClass(clazz), ProcessingStructurally::walk), consumer);
    }

    public static <N> void walkDescendantsLeavesFirst(INode node, Class<N> clazz, Consumer<INode> consumer) {
        consumeSequence(ProcessingStructurally.walkDescendants(node, Reflection.createKotlinClass(clazz), ProcessingStructurally::walkLeavesFirst), consumer);
    }

    /**
     * Walks the AST within the given range starting from the given node
     * and returns the result as sequence to consume.
     *
     * @param node     the node from which the walk should start
     * @param range the range within which the walk should remain
     */
    public static <N> void walkWithin(INode node, Range range, Consumer<INode> consumer) {
        consumeSequence(ProcessingByRange.walkWithin(node, range), consumer);
    }

    /**
     * Walks the AST within the given range starting from each give node
     * and concatenates all results in a single sequence to consume.
     *
     * @param nodes    the nodes from which the walk should start
     * @param range the range within which the walk should remain
     */
    public static <N> void walkWithin(List<INode> nodes, Range range, Consumer<INode> consumer) {
        consumeSequence(ProcessingByRange.walkWithin(nodes, range), consumer);
    }

    public static <T> T findAncestorOfType(INode node, Class<T> clazz) {
        return ProcessingStructurally.findAncestorOfType(node, clazz);
    }
}