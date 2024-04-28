package com.strumenta.kolasu.serialization

import com.strumenta.kolasu.language.StarLasuLanguage
import com.strumenta.kolasu.language.explore
import com.strumenta.kolasu.model.LanguageAssociation
import com.strumenta.kolasu.model.Node

object YetAnotherLanguageForSerialization : StarLasuLanguage("YetAnotherLanguageForSerialization") {
    init {
        explore(MyRoot::class, Section::class)
    }
}

@LanguageAssociation(YetAnotherLanguageForSerialization::class)
data class MyRoot(
    val mainSection: Section,
    val otherSections: List<Section>,
) : Node()

@LanguageAssociation(YetAnotherLanguageForSerialization::class)
data class Section(
    val name: String,
    val contents: List<AbstractContent>,
) : Node()

@LanguageAssociation(YetAnotherLanguageForSerialization::class)
sealed class AbstractContent : Node()

@LanguageAssociation(YetAnotherLanguageForSerialization::class)
data class Content(
    val id: Int,
    val annidatedContent: Content?,
) : AbstractContent()

@LanguageAssociation(YetAnotherLanguageForSerialization::class)
data class OtherContent(
    val values: List<IntHolder>,
) : AbstractContent()

@LanguageAssociation(YetAnotherLanguageForSerialization::class)
data class IntHolder(
    val value: Int,
) : AbstractContent()
