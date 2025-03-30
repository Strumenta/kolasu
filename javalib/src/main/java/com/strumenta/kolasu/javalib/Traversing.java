package com.strumenta.kolasu.javalib;

import com.strumenta.kolasu.model.BaseASTNode;
import com.strumenta.kolasu.model.Position;
import com.strumenta.kolasu.traversing.ProcessingByPosition;
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

    public static Stream<BaseASTNode> walk(BaseASTNode node) {
        return asStream(ProcessingStructurally.walk(node));
    }

    /**
     * Performs a post-order (or leaves-first) node traversal starting with a given node.
     */
    public static Stream<BaseASTNode> walkLeavesFirst(BaseASTNode node) {
        return asStream(ProcessingStructurally.walkLeavesFirst(node));
    }

    public static Stream<BaseASTNode> walkAncestors(BaseASTNode node) {
        return asStream(ProcessingStructurally.walkAncestors(node));
    }

    public static Stream<BaseASTNode> walkDescendantsBreadthFirst(BaseASTNode node) {
        return asStream(ProcessingStructurally.walkDescendants(node, ProcessingStructurally::walk));
    }

    public static Stream<BaseASTNode> walkDescendantsLeavesFirst(BaseASTNode node) {
        return asStream(ProcessingStructurally.walkDescendants(node, ProcessingStructurally::walkLeavesFirst));
    }

    public static <N> Stream<N> walkDescendantsBreadthFirst(BaseASTNode node, Class<N> clazz) {
        return asStream(ProcessingStructurally.walkDescendants(node, Reflection.createKotlinClass(clazz), ProcessingStructurally::walk));
    }

    public static <N> Stream<N> walkDescendantsLeavesFirst(BaseASTNode node, Class<N> clazz) {
        return asStream(ProcessingStructurally.walkDescendants(node, Reflection.createKotlinClass(clazz), ProcessingStructurally::walkLeavesFirst));
    }

    public static void walk(BaseASTNode node, Consumer<BaseASTNode> consumer) {
        consumeSequence(ProcessingStructurally.walk(node), consumer);
    }

    /**
     * Performs a post-order (or leaves-first) node traversal starting with a given node.
     */
    public static void walkLeavesFirst(BaseASTNode node, Consumer<BaseASTNode> consumer) {
        consumeSequence(ProcessingStructurally.walkLeavesFirst(node), consumer);
    }

    public static void walkAncestors(BaseASTNode node, Consumer<BaseASTNode> consumer) {
        consumeSequence(ProcessingStructurally.walkAncestors(node), consumer);
    }

    public static void walkDescendantsBreadthFirst(BaseASTNode node, Consumer<BaseASTNode> consumer) {
        consumeSequence(ProcessingStructurally.walkDescendants(node, ProcessingStructurally::walk), consumer);
    }

    public static void walkDescendantsLeavesFirst(BaseASTNode node, Consumer<BaseASTNode> consumer) {
        consumeSequence(ProcessingStructurally.walkDescendants(node, ProcessingStructurally::walkLeavesFirst), consumer);
    }

    public static <N> void walkDescendantsBreadthFirst(BaseASTNode node, Class<N> clazz, Consumer<BaseASTNode> consumer) {
        consumeSequence(ProcessingStructurally.walkDescendants(node, Reflection.createKotlinClass(clazz), ProcessingStructurally::walk), consumer);
    }

    public static <N> void walkDescendantsLeavesFirst(BaseASTNode node, Class<N> clazz, Consumer<BaseASTNode> consumer) {
        consumeSequence(ProcessingStructurally.walkDescendants(node, Reflection.createKotlinClass(clazz), ProcessingStructurally::walkLeavesFirst), consumer);
    }

    /**
     * Walks the AST within the given position starting from the given node
     * and returns the result as sequence to consume.
     *
     * @param node     the node from which the walk should start
     * @param position the position within which the walk should remain
     */
    public static <N> void walkWithin(BaseASTNode node, Position position, Consumer<BaseASTNode> consumer) {
        consumeSequence(ProcessingByPosition.walkWithin(node, position), consumer);
    }

    /**
     * Walks the AST within the given position starting from each give node
     * and concatenates all results in a single sequence to consume.
     *
     * @param nodes    the nodes from which the walk should start
     * @param position the position within which the walk should remain
     */
    public static <N> void walkWithin(List<BaseASTNode> nodes, Position position, Consumer<BaseASTNode> consumer) {
        consumeSequence(ProcessingByPosition.walkWithin(nodes, position), consumer);
    }

    public static <T> T findAncestorOfType(BaseASTNode node, Class<T> clazz) {
        return ProcessingStructurally.findAncestorOfType(node, clazz);
    }
}