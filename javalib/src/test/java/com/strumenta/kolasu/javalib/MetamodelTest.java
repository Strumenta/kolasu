package com.strumenta.kolasu.javalib;

import org.junit.Test;
import org.lionweb.lioncore.java.metamodel.Concept;

import static org.junit.Assert.assertEquals;

public class MetamodelTest {

    @Test
    public void testCompilationUnitConcept() {
        Concept concept = MetamodelUtils.getConcept(CompilationUnit.class);
        assertEquals(1, concept.allContainments().size());
    }
}
