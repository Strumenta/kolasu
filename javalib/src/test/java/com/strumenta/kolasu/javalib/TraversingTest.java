package com.strumenta.kolasu.javalib;

import com.strumenta.kolasu.language.Concept;
import com.strumenta.kolasu.language.Containment;
import com.strumenta.kolasu.language.StarLasuLanguage;
import com.strumenta.kolasu.language.SupportKt;
import com.strumenta.kolasu.model.Multiplicity;
import com.strumenta.kolasu.model.NodeLike;
import com.strumenta.kolasu.model.Processing;
import com.strumenta.kolasu.traversing.ProcessingStructurally;
import org.junit.Test;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;


public class TraversingTest {

    static class StarLasuLanguageInstance extends StarLasuLanguage  {
        private StarLasuLanguageInstance() {
            super("com.strumenta.kolasu.javalib", new LinkedList<>());
            SupportKt.explore(this, CompilationUnit.class, A.class, B.class);
            Concept compilationUnitConcept = getConcept("CompilationUnit");
            Concept aConcept = getConcept("A");
            Concept bConcept = getConcept("B");
            compilationUnitConcept.getDeclaredFeatures().add(
                    new Containment("as", Multiplicity.MANY, aConcept,
                            nodeLike -> ((CompilationUnit)nodeLike).getAs(),
                            false));
            aConcept.getDeclaredFeatures().add(
                    new Containment("bs", Multiplicity.MANY, bConcept,
                            nodeLike -> ((A)nodeLike).getBs(),
                            false));
        }
        public static final StarLasuLanguageInstance INSTANCE = new StarLasuLanguageInstance();
    }

    private CompilationUnit cu = new CompilationUnit();
    private A a1 = new A();
    private B b1 = new B();
    private B b2 = new B();
    private A a2 = new A();
    private B b3 = new B();

    public TraversingTest() {
        StarLasuLanguageInstance.INSTANCE.ensureIsRegistered();
        cu.getAs().add(a1);
        cu.getAs().add(a2);
        a1.getBs().add(b1);
        a1.getBs().add(b2);
        a2.getBs().add(b3);
        Processing.assignParents(cu);
    }

    @Test
    public void testGetChildren() {
        assertEquals(Arrays.asList(a1, a2), ProcessingStructurally.getChildren(cu));
    }

    @Test
    public void testWalk() {
        List<NodeLike> nodes = Traversing.walk(cu).collect(Collectors.toList());
        assertEquals(Arrays.asList(cu, a1, b1, b2, a2, b3), nodes);
    }
}
