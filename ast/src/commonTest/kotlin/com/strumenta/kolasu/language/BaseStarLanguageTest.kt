package com.strumenta.kolasu.language

import com.strumenta.kolasu.model.Multiplicity
import kotlin.test.Test
import kotlin.test.assertEquals

class BaseStarLanguageTest {

    @Test
    fun checkASTNode() {
        assertEquals("ASTNode", BaseStarLasuLanguage.astNode.name)
        assertEquals("com.strumenta.basestarlasulanguage.ASTNode", BaseStarLasuLanguage.astNode.qualifiedName)
    }

    @Test
    fun checkIntType() {
        assertEquals("kotlin.Int", intType.name)
    }

    @Test
    fun checkIPossiblyNamed() {
        assertEquals("IPossiblyNamed", BaseStarLasuLanguage.iPossiblyNamed.name)
        assertEquals(1, BaseStarLasuLanguage.iPossiblyNamed.allFeatures.size)
        val nameProp = BaseStarLasuLanguage.iPossiblyNamed.property("name")!!
        assertEquals("name", nameProp.name)
        assertEquals(stringType, nameProp.type)
        assertEquals(Multiplicity.OPTIONAL, nameProp.multiplicity)
    }

    @Test
    fun checkINamed() {
        assertEquals("INamed", BaseStarLasuLanguage.iNamed.name)
        assertEquals(1, BaseStarLasuLanguage.iNamed.allFeatures.size)
        val nameProp = BaseStarLasuLanguage.iNamed.property("name")!!
        assertEquals("name", nameProp.name)
        assertEquals(stringType, nameProp.type)
        assertEquals(Multiplicity.SINGULAR, nameProp.multiplicity)
    }

    @Test
    fun checkRange() {
        assertEquals("Range", BaseStarLasuLanguage.range.name)
    }

    @Test
    fun checkPoint() {
        assertEquals("Point", BaseStarLasuLanguage.point.name)
    }
}