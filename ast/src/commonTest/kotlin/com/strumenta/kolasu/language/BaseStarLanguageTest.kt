package com.strumenta.kolasu.language

import kotlin.test.Test
import kotlin.test.assertEquals

class BaseStarLanguageTest {

    @Test
    fun checkASTNode() {
        assertEquals("ASTNode", BaseStarLasuLanguage.astNode.name)
        assertEquals("com.strumenta.basestarlasulanguage.ASTNode", BaseStarLasuLanguage.astNode.qualifiedName)
    }
}