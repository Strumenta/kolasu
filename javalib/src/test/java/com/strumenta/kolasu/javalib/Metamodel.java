package com.strumenta.kolasu.javalib;

import com.strumenta.kolasu.metamodel.StarLasuMetamodel;
import com.strumenta.kolasu.model.lionweb.FacadeKt;
import org.lionweb.lioncore.java.metamodel.Concept;
import org.lionweb.lioncore.java.metamodel.Containment;

public class Metamodel extends org.lionweb.lioncore.java.metamodel.Metamodel {

    public static org.lionweb.lioncore.java.metamodel.Metamodel INSTANCE = new org.lionweb.lioncore.java.metamodel.Metamodel();

    private Metamodel() {

    }

    static {
        Concept compilationUnit = new Concept();
        compilationUnit.setExtendedConcept(StarLasuMetamodel.INSTANCE.getAstNode());

        Concept a = new Concept();
        a.setExtendedConcept(StarLasuMetamodel.INSTANCE.getAstNode());

        Concept b = new Concept();
        b.setExtendedConcept(StarLasuMetamodel.INSTANCE.getAstNode());

        compilationUnit.addFeature(Containment.createMultiple("as", a));

        a.addFeature(Containment.createMultiple("bs", b));

        FacadeKt.recordConceptForClass(CompilationUnit.class, compilationUnit);
        FacadeKt.recordConceptForClass(CompilationUnit.A.class, a);
        FacadeKt.recordConceptForClass(CompilationUnit.B.class, b);
    }
}
