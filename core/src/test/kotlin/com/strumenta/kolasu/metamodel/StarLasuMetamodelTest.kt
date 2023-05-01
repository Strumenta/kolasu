package com.strumenta.kolasu.metamodel

import org.junit.Test
import org.lionweb.lioncore.java.utils.MetamodelValidator
import kotlin.test.assertEquals

class StarLasuMetamodelTest {

    @Test
    fun validateStarLasuMetamodel() {
        val vr = MetamodelValidator().validateMetamodel(StarLasuMetamodel)
        assertEquals(true, vr.isSuccessful)
    }
}
