package com.strumenta.kolasu.javalib;

import com.strumenta.kolasu.model.Node;
import com.strumenta.kolasu.validation.Issue;
import kotlin.Unit;
import org.junit.Test;

import java.util.ArrayList;

import static com.strumenta.kolasu.javalib.ASTTransformer.setter;
import static com.strumenta.kolasu.testing.Testing.assertASTsAreEqual;
import static org.junit.Assert.assertTrue;

public class TransformerTest {
    @Test
    public void testJavaNodes() {
        ArrayList<Issue> issues = new ArrayList<>();
        ASTTransformer t = new ASTTransformer(issues, false);
        t.registerNodeFactory(Node1.class, Node1.class)
                .withChild(Node1::getNode2, setter(Node1::setNode2), "node2");
        Node1 node1 = new Node1();
        node1.setNode2(new Node2());
        Node transformed = t.transform(node1);
        assertTrue(issues.toString(), issues.isEmpty());
        assertASTsAreEqual(node1, transformed);
    }
}

