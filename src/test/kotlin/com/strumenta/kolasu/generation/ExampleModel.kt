package com.strumenta.kolasu.generation

import com.strumenta.kolasu.model.Node

data class MyRoot(val mainSection: Section, val otherSections: List<Section>) : Node()
data class Section(val name: String, val contents: List<Content>) : Node()
data class Content(val id: Int, val annidatedContent: Content?) : Node()