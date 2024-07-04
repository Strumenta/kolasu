package com.strumenta.kolasu.lionweb

import com.strumenta.kolasu.language.BaseStarLasuLanguage
import com.strumenta.kolasu.language.Concept
import com.strumenta.kolasu.language.ConceptInterface
import com.strumenta.kolasu.language.Containment
import com.strumenta.kolasu.language.EnumType
import com.strumenta.kolasu.language.EnumerationLiteral
import com.strumenta.kolasu.language.KolasuLanguage
import com.strumenta.kolasu.language.PrimitiveType
import com.strumenta.kolasu.language.Property
import com.strumenta.kolasu.language.Reference
import com.strumenta.kolasu.language.StarLasuLanguage
import com.strumenta.kolasu.language.explore
import com.strumenta.kolasu.language.intType
import com.strumenta.kolasu.language.stringType
import com.strumenta.kolasu.model.ASTRoot
import com.strumenta.kolasu.model.EntityDeclaration
import com.strumenta.kolasu.model.LanguageAssociation
import com.strumenta.kolasu.model.Multiplicity
import com.strumenta.kolasu.model.Named
import com.strumenta.kolasu.model.Node
import com.strumenta.kolasu.model.NodeLike
import com.strumenta.kolasu.model.ReferenceValue
import io.lionweb.lioncore.java.language.LionCoreBuiltins
import io.lionweb.lioncore.java.utils.LanguageValidator
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertSame

@LanguageAssociation(MyOtherStarLasuLanguageInstance::class)
@ASTRoot
data class SimpleRoot(
    val id: Int,
    val childrez: MutableList<SimpleDecl>,
) : Node()

@LanguageAssociation(MyOtherStarLasuLanguageInstance::class)
sealed class SimpleDecl :
    Node(),
    EntityDeclaration

@LanguageAssociation(MyOtherStarLasuLanguageInstance::class)
@ASTRoot(canBeNotRoot = true)
data class SimpleNodeA(
    override val name: String,
    val ref: ReferenceValue<SimpleNodeA>,
    val child: SimpleNodeB?,
) : SimpleDecl(),
    Named,
    MyRelevantInterface,
    MyIrrelevantInterface

data class MyNonPartition(
    val roots: MutableList<SimpleRoot>,
) : Node()

interface MyIrrelevantInterface

interface MyRelevantInterface : NodeLike

@LanguageAssociation(MyOtherStarLasuLanguageInstance::class)
data class SimpleNodeB(
    val value: String,
) : SimpleDecl()

data class MyNodeWithNullableContainmentLists(
    val children: MutableList<MyNodeWithNullableContainmentLists>?,
) : Node()

object MyOtherStarLasuLanguageInstance : StarLasuLanguage("com.strumenta.kolasu.lionweb") {
    init {
        explore(SimpleRoot::class, NodeWithEnum::class)
    }
}

class LionWebLanguageConverterTest {
    @Test
    fun exportSimpleKolasuLanguage() {
        val kLanguage =
            KolasuLanguage("com.strumenta.SimpleLang").apply {
                addClass(SimpleRoot::class)
            }
        assertEquals(5, kLanguage.astClasses.size)
        val lwLanguage = LionWebLanguageConverter().exportToLionWeb(kLanguage)
        assertEquals("1", lwLanguage.version)
        assertEquals(5, lwLanguage.elements.size)

        val simpleRoot = lwLanguage.getConceptByName("SimpleRoot")!!
        val simpleDecl = lwLanguage.getConceptByName("SimpleDecl")!!
        val simpleNodeA = lwLanguage.getConceptByName("SimpleNodeA")!!
        val simpleNodeB = lwLanguage.getConceptByName("SimpleNodeB")!!
        val myRelevantInterface = lwLanguage.getInterfaceByName("MyRelevantInterface")!!
        assertNull(lwLanguage.getInterfaceByName("MyIrrelevantInterface"))

        assertEquals("SimpleRoot", simpleRoot.name)
        assertSame(lwLanguage, simpleRoot.language)
        assertEquals(StarLasuLWLanguage.ASTNode, simpleRoot.extendedConcept)
        assertEquals(emptyList(), simpleRoot.implemented)
        assertEquals(false, simpleRoot.isAbstract)
        assertEquals(2, simpleRoot.features.size)
        assertEquals(5, simpleRoot.allFeatures().size)

        val simpleRootID = simpleRoot.getPropertyByName("id")!!
        assertEquals("id", simpleRootID.name)
        assertEquals(false, simpleRootID.isOptional)
        assertEquals(LionCoreBuiltins.getInteger(), simpleRootID.type)

        val simpleRootChildren = simpleRoot.getContainmentByName("childrez")!!
        assertEquals("childrez", simpleRootChildren.name)
        assertEquals(true, simpleRootChildren.isOptional)
        assertEquals(true, simpleRootChildren.isMultiple)
        assertEquals(simpleDecl, simpleRootChildren.type)

        assertEquals("SimpleDecl", simpleDecl.name)
        assertSame(lwLanguage, simpleDecl.language)
        assertEquals(StarLasuLWLanguage.ASTNode, simpleDecl.extendedConcept)
        assertEquals(emptyList(), simpleRoot.implemented)
        assertEquals(true, simpleDecl.isAbstract)

        assertEquals("SimpleNodeA", simpleNodeA.name)
        assertSame(lwLanguage, simpleNodeA.language)
        assertEquals(simpleDecl, simpleNodeA.extendedConcept)
        assertEquals(listOf(StarLasuLWLanguage.EntityDeclaration), simpleNodeA.extendedConcept!!.implemented)
        assertEquals(listOf(LionCoreBuiltins.getINamed(), myRelevantInterface), simpleNodeA.implemented)
        assertEquals(false, simpleNodeA.isAbstract)
        assertEquals(2, simpleNodeA.features.size)
        assertEquals(6, simpleNodeA.allFeatures().size)

        assertEquals(
            true,
            LionCoreBuiltins.getINamed().getPropertyByName("name") in simpleNodeA.allFeatures(),
        )

        val simpleNodeARef = simpleNodeA.getReferenceByName("ref")!!
        assertEquals("ref", simpleNodeARef.name)
        assertEquals(false, simpleNodeARef.isOptional)
        assertEquals(false, simpleNodeARef.isMultiple)
        assertEquals(simpleNodeA, simpleNodeARef.type)

        val simpleNodeAChild = simpleNodeA.getContainmentByName("child")!!
        assertEquals("child", simpleNodeAChild.name)
        assertEquals(true, simpleNodeAChild.isOptional)
        assertEquals(false, simpleNodeAChild.isMultiple)
        assertEquals(simpleNodeB, simpleNodeAChild.type)

        assertEquals("SimpleNodeB", simpleNodeB.name)
        assertSame(lwLanguage, simpleNodeB.language)
        assertEquals(simpleDecl, simpleNodeB.extendedConcept)
        assertEquals(false, simpleNodeB.isAbstract)
        assertEquals(1, simpleNodeB.features.size)
        assertEquals(4, simpleNodeB.allFeatures().size)

        val simpleNodeBValue = simpleNodeB.getPropertyByName("value")!!
        assertEquals("value", simpleNodeBValue.name)
        assertEquals(false, simpleNodeBValue.isOptional)
        assertEquals(LionCoreBuiltins.getString(), simpleNodeBValue.type)

        val validationResult = LanguageValidator().validate(lwLanguage)
        assertEquals(true, validationResult.isSuccessful, validationResult.issues.toString())
    }

    @Test
    fun exportSimpleStarLasuLanguage() {
        val sLanguage =
            StarLasuLanguage("com.strumenta.SimpleLang").apply {
                val kSimpleRoot = Concept(this, "SimpleRoot")
                types.add(kSimpleRoot)
                val kMyRelevantInterface = ConceptInterface(this, "MyRelevantInterface")
                types.add(kMyRelevantInterface)
                val kSimpleDecl = Concept(this, "SimpleDecl")
                kSimpleDecl.isAbstract = true
                types.add(kSimpleDecl)
                val kSimpleNodeA = Concept(this, "SimpleNodeA")
                kSimpleNodeA.superConcept = kSimpleDecl
                kSimpleNodeA.conceptInterfaces.add(BaseStarLasuLanguage.iNamed)
                kSimpleNodeA.conceptInterfaces.add(kMyRelevantInterface)
                types.add(kSimpleNodeA)
                val kSimpleNodeB = Concept(this, "SimpleNodeB")
                kSimpleNodeB.superConcept = kSimpleDecl
                types.add(kSimpleNodeB)
                val kAnEnum =
                    EnumType(
                        "AnEnum",
                        mutableListOf(
                            EnumerationLiteral("A"),
                            EnumerationLiteral("B"),
                            EnumerationLiteral("C"),
                        ),
                    )
                types.add(kAnEnum)
                val kAPrimitiveType = PrimitiveType("APrimitiveType")
                types.add(kAPrimitiveType)

                kSimpleRoot.declaredFeatures.add(
                    Property("id", false, intType, { n ->
                        (n as SimpleRoot).id
                    }),
                )
                kSimpleRoot.declaredFeatures.add(
                    Containment("childrez", Multiplicity.MANY, kSimpleDecl, { n ->
                        (n as SimpleRoot).childrez
                    }),
                )

                kSimpleNodeA.declaredFeatures.add(
                    Reference("ref", false, kSimpleNodeA, { n ->
                        (n as SimpleNodeA).ref
                    }),
                )
                kSimpleNodeA.declaredFeatures.add(
                    Containment("child", Multiplicity.OPTIONAL, kSimpleNodeB, { n ->
                        (n as SimpleNodeA).child
                    }),
                )

                kSimpleNodeB.declaredFeatures.add(
                    Property("value", false, stringType, { n ->
                        (n as SimpleNodeB).value
                    }),
                )
            }
        assertEquals(5, sLanguage.classifiers.size)
        assertEquals(1, sLanguage.primitives.size)
        assertEquals(1, sLanguage.enums.size)
        val lwLanguage = LionWebLanguageConverter().exportToLionWeb(sLanguage)
        assertEquals("1", lwLanguage.version)
        assertEquals(7, lwLanguage.elements.size)

        val simpleRoot = lwLanguage.getConceptByName("SimpleRoot")!!
        val simpleDecl = lwLanguage.getConceptByName("SimpleDecl")!!
        val simpleNodeA = lwLanguage.getConceptByName("SimpleNodeA")!!
        val simpleNodeB = lwLanguage.getConceptByName("SimpleNodeB")!!
        val myRelevantInterface = lwLanguage.getInterfaceByName("MyRelevantInterface")!!
        assertNull(lwLanguage.getInterfaceByName("MyIrrelevantInterface"))
        val anEnum = lwLanguage.getEnumerationByName("AnEnum")!!
        val aPrimitiveType = lwLanguage.getPrimitiveTypeByName("APrimitiveType")!!

        assertEquals("SimpleRoot", simpleRoot.name)
        assertSame(lwLanguage, simpleRoot.language)
        assertEquals(StarLasuLWLanguage.ASTNode, simpleRoot.extendedConcept)
        assertEquals(emptyList(), simpleRoot.implemented)
        assertEquals(false, simpleRoot.isAbstract)
        assertEquals(2, simpleRoot.features.size)
        assertEquals(5, simpleRoot.allFeatures().size)

        val simpleRootID = simpleRoot.getPropertyByName("id")!!
        assertEquals("id", simpleRootID.name)
        assertEquals(false, simpleRootID.isOptional)
        assertEquals(LionCoreBuiltins.getInteger(), simpleRootID.type)

        val simpleRootChildren = simpleRoot.getContainmentByName("childrez")!!
        assertEquals("childrez", simpleRootChildren.name)
        assertEquals(true, simpleRootChildren.isOptional)
        assertEquals(true, simpleRootChildren.isMultiple)
        assertEquals(simpleDecl, simpleRootChildren.type)

        assertEquals("SimpleDecl", simpleDecl.name)
        assertSame(lwLanguage, simpleDecl.language)
        assertEquals(StarLasuLWLanguage.ASTNode, simpleDecl.extendedConcept)
        assertEquals(emptyList(), simpleRoot.implemented)
        assertEquals(true, simpleDecl.isAbstract)

        assertEquals("SimpleNodeA", simpleNodeA.name)
        assertSame(lwLanguage, simpleNodeA.language)
        assertEquals(simpleDecl, simpleNodeA.extendedConcept)
        assertEquals(listOf(LionCoreBuiltins.getINamed(), myRelevantInterface), simpleNodeA.implemented)
        assertEquals(false, simpleNodeA.isAbstract)
        assertEquals(2, simpleNodeA.features.size)
        assertEquals(6, simpleNodeA.allFeatures().size)

        assertEquals(
            true,
            LionCoreBuiltins.getINamed().getPropertyByName("name") in simpleNodeA.allFeatures(),
        )

        val simpleNodeARef = simpleNodeA.getReferenceByName("ref")!!
        assertEquals("ref", simpleNodeARef.name)
        assertEquals(false, simpleNodeARef.isOptional)
        assertEquals(false, simpleNodeARef.isMultiple)
        assertEquals(simpleNodeA, simpleNodeARef.type)

        val simpleNodeAChild = simpleNodeA.getContainmentByName("child")!!
        assertEquals("child", simpleNodeAChild.name)
        assertEquals(true, simpleNodeAChild.isOptional)
        assertEquals(false, simpleNodeAChild.isMultiple)
        assertEquals(simpleNodeB, simpleNodeAChild.type)

        assertEquals("SimpleNodeB", simpleNodeB.name)
        assertSame(lwLanguage, simpleNodeB.language)
        assertEquals(simpleDecl, simpleNodeB.extendedConcept)
        assertEquals(false, simpleNodeB.isAbstract)
        assertEquals(1, simpleNodeB.features.size)
        assertEquals(4, simpleNodeB.allFeatures().size)

        val simpleNodeBValue = simpleNodeB.getPropertyByName("value")!!
        assertEquals("value", simpleNodeBValue.name)
        assertEquals(false, simpleNodeBValue.isOptional)
        assertEquals(LionCoreBuiltins.getString(), simpleNodeBValue.type)

        val validationResult = LanguageValidator().validate(lwLanguage)
        assertEquals(true, validationResult.isSuccessful, validationResult.issues.toString())
    }

    @Test
    fun exportSimpleLanguageWithPartitions() {
        val kLanguage =
            KolasuLanguage("com.strumenta.SimpleLang").apply {
                addClass(SimpleRoot::class)
                addClass(MyNonPartition::class)
            }
        assertEquals(6, kLanguage.astClasses.size)
        val lwLanguage = LionWebLanguageConverter().exportToLionWeb(kLanguage)
        assertEquals("1", lwLanguage.version)
        assertEquals(6, lwLanguage.elements.size)

        val myNonPartition = lwLanguage.getConceptByName("MyNonPartition")!!
        val simpleRoot = lwLanguage.getConceptByName("SimpleRoot")!!
        val simpleDecl = lwLanguage.getConceptByName("SimpleDecl")!!
        val simpleNodeA = lwLanguage.getConceptByName("SimpleNodeA")!!
        val simpleNodeB = lwLanguage.getConceptByName("SimpleNodeB")!!
        val myRelevantInterface = lwLanguage.getInterfaceByName("MyRelevantInterface")!!
        assertNull(lwLanguage.getInterfaceByName("MyIrrelevantInterface"))

        assertEquals("MyNonPartition", myNonPartition.name)
        assertEquals(false, myNonPartition.isPartition)
        assertSame(lwLanguage, myNonPartition.language)
        assertEquals(StarLasuLWLanguage.ASTNode, myNonPartition.extendedConcept)
        assertEquals(emptyList(), myNonPartition.implemented)
        assertEquals(false, myNonPartition.isAbstract)
        assertEquals(1, myNonPartition.features.size)
        assertEquals(4, myNonPartition.allFeatures().size)

        assertEquals("SimpleRoot", simpleRoot.name)
        assertEquals(false, simpleRoot.isPartition)
        assertSame(lwLanguage, simpleRoot.language)
        assertEquals(StarLasuLWLanguage.ASTNode, simpleRoot.extendedConcept)
        assertEquals(emptyList(), simpleRoot.implemented)
        assertEquals(false, simpleRoot.isAbstract)
        assertEquals(2, simpleRoot.features.size)
        assertEquals(5, simpleRoot.allFeatures().size)

        val simpleRootID = simpleRoot.getPropertyByName("id")!!
        assertEquals("id", simpleRootID.name)
        assertEquals(false, simpleRootID.isOptional)
        assertEquals(LionCoreBuiltins.getInteger(), simpleRootID.type)

        val simpleRootChildren = simpleRoot.getContainmentByName("childrez")!!
        assertEquals("childrez", simpleRootChildren.name)
        assertEquals(true, simpleRootChildren.isOptional)
        assertEquals(true, simpleRootChildren.isMultiple)
        assertEquals(simpleDecl, simpleRootChildren.type)

        assertEquals("SimpleDecl", simpleDecl.name)
        assertEquals(false, simpleDecl.isPartition)
        assertSame(lwLanguage, simpleDecl.language)
        assertEquals(StarLasuLWLanguage.ASTNode, simpleDecl.extendedConcept)
        assertEquals(emptyList(), simpleRoot.implemented)
        assertEquals(true, simpleDecl.isAbstract)

        assertEquals("SimpleNodeA", simpleNodeA.name)
        assertEquals(false, simpleNodeA.isPartition)
        assertSame(lwLanguage, simpleNodeA.language)
        assertEquals(simpleDecl, simpleNodeA.extendedConcept)
        assertEquals(listOf(LionCoreBuiltins.getINamed(), myRelevantInterface), simpleNodeA.implemented)
        assertEquals(false, simpleNodeA.isAbstract)
        assertEquals(2, simpleNodeA.features.size)
        assertEquals(6, simpleNodeA.allFeatures().size)

        assertEquals(
            true,
            LionCoreBuiltins.getINamed().getPropertyByName("name") in simpleNodeA.allFeatures(),
        )

        val simpleNodeARef = simpleNodeA.getReferenceByName("ref")!!
        assertEquals("ref", simpleNodeARef.name)
        assertEquals(false, simpleNodeARef.isOptional)
        assertEquals(false, simpleNodeARef.isMultiple)
        assertEquals(simpleNodeA, simpleNodeARef.type)

        val simpleNodeAChild = simpleNodeA.getContainmentByName("child")!!
        assertEquals("child", simpleNodeAChild.name)
        assertEquals(true, simpleNodeAChild.isOptional)
        assertEquals(false, simpleNodeAChild.isMultiple)
        assertEquals(simpleNodeB, simpleNodeAChild.type)

        assertEquals("SimpleNodeB", simpleNodeB.name)
        assertEquals(false, simpleNodeB.isPartition)
        assertSame(lwLanguage, simpleNodeB.language)
        assertEquals(simpleDecl, simpleNodeB.extendedConcept)
        assertEquals(false, simpleNodeB.isAbstract)
        assertEquals(1, simpleNodeB.features.size)
        assertEquals(4, simpleNodeB.allFeatures().size)

        val simpleNodeBValue = simpleNodeB.getPropertyByName("value")!!
        assertEquals("value", simpleNodeBValue.name)
        assertEquals(false, simpleNodeBValue.isOptional)
        assertEquals(LionCoreBuiltins.getString(), simpleNodeBValue.type)

        val validationResult = LanguageValidator().validate(lwLanguage)
        assertEquals(true, validationResult.isSuccessful, validationResult.issues.toString())
    }

    @Test(expected = RuntimeException::class)
    fun conversionOfNullableContainmentListIsForbidden() {
        val kLanguage =
            KolasuLanguage("com.strumenta.SimpleLang").apply {
                addClass(MyNodeWithNullableContainmentLists::class)
            }
        val lwLanguage = LionWebLanguageConverter().exportToLionWeb(kLanguage)
    }

    @Test
    fun exportEnumLiterals() {
        val converter = LionWebLanguageConverter()
        val lwLanguage =
            converter.exportToLionWeb(
                KolasuLanguage("myLanguage").apply {
                    addClass(NodeWithEnum::class)
                },
            )
        val enumeration = lwLanguage.getEnumerationByName("AnEnum") ?: throw IllegalStateException()
        assertEquals("AnEnum", enumeration.name)
        assertEquals(3, enumeration.literals.size)
        assertEquals("FOO", enumeration.literals[0].name)
        assertEquals("BAR", enumeration.literals[1].name)
        assertEquals("ZUM", enumeration.literals[2].name)
    }
}
