package com.strumenta.kolasu.serialization

import com.strumenta.kolasu.model.ASTNode

data class MyRoot(val mainSection: Section, val otherSections: List<Section>) : ASTNode()
data class Section(val name: String, val contents: List<AbstractContent>) : ASTNode()
sealed class AbstractContent : ASTNode()
data class Content(val id: Int, val annidatedContent: Content?) : AbstractContent()
data class OtherContent(val values: List<Int>) : AbstractContent()
