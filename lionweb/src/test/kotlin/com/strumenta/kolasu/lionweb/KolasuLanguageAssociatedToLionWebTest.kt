package com.strumenta.kolasu.lionweb

import com.strumenta.kolasu.language.KolasuLanguage
import com.strumenta.kolasu.language.StarLasuLanguage
import com.strumenta.kolasu.language.explore
import com.strumenta.kolasu.model.Named
import com.strumenta.kolasu.model.Node
import com.strumenta.kolasu.model.ReferenceValue
import io.lionweb.lioncore.java.language.Concept
import io.lionweb.lioncore.java.language.Enumeration
import io.lionweb.lioncore.java.language.Language
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@LionWebAssociation("key-123")
data class LWRoot(
    val id: Int,
    val childrez: MutableList<LWNodeA>,
) : Node()

@LionWebAssociation("key-456")
data class LWNodeA(
    override val name: String,
    val ref: ReferenceValue<LWNodeA>,
    val child: LWNodeB?,
) : Node(),
    Named

@LionWebAssociation("key-000")
enum class MyEnum {
    A,
    B,
    C,
}

@LionWebAssociation("key-789")
data class LWNodeB(
    val value: String,
    val anotherValue: MyEnum,
) : Node()

object StarLasuLanguageInstance : StarLasuLanguage("com.strumenta.kolasu.lionweb") {
    init {
        explore(
            LWRoot::class,
            NodeWithEnum::class,
            NodeWithPropertiesNotInConstructor::class,
            NodeWithPropertiesNotInConstructorMutableProps::class,
        )
    }
}

class KolasuLanguageAssociatedToLionWebTest {
    @Test
    fun enumsAreRecorded() {
        val lwImpExp = LionWebModelConverter()
        val lwLanguage =
            lwImpExp.exportLanguageToLionWeb(
                KolasuLanguage("pricing").apply {
                    addClass(LWRoot::class)
                },
            )
        assertEquals(4, lwLanguage.elements.size)
        val myEnum = lwLanguage.getElementByName("MyEnum")
        assertTrue { myEnum is Enumeration }
    }

    @Test
    fun conceptsAreAssociatedWithRightKey() {
        val lwLang =
            Language("LangA").apply {
                id = "lang-id"
                key = "lang-key"
                version = "45"
            }
        val lwRoot =
            Concept(lwLang, "Root").apply {
                key = "key-123"
            }
        val lwNodeA =
            Concept(lwLang, "NodeA").apply {
                key = "key-456"
            }
        val lwNodeB =
            Concept(lwLang, "NodeA").apply {
                key = "key-789"
            }
        val lwEnum =
            Enumeration(lwLang, "MyEnum").apply {
                key = "key-000"
            }
        val kolasuLanguage =
            KolasuLanguage("LangA").apply {
                addClass(LWRoot::class)
            }
        val lie = LionWebLanguageConverter()
        lie.associateLanguages(lwLang, kolasuLanguage)
        assertEquals(LWRoot::class, lie.correspondingKolasuClass(lwRoot))
        assertEquals(LWNodeA::class, lie.correspondingKolasuClass(lwNodeA))
        assertEquals(LWNodeB::class, lie.correspondingKolasuClass(lwNodeB))
    }
}
