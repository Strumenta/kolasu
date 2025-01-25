import com.strumenta.starlasuv2.StarLasuGen
import io.lionweb.lioncore.java.language.Concept
import io.lionweb.lioncore.java.language.Containment
import io.lionweb.lioncore.java.language.Language
import io.lionweb.lioncore.kotlin.createConcept

@StarLasuGen // A KSP Should generate from it
object StarLasuIdiomsLWLanguage : Language() {
    init {
        id = "com-strumenta-starlasu-idioms"
        key = "com-strumenta-starlasu-idioms"
        version = "2024.1"
        val idiom = createConcept("Idiom")
        val idiomElement = createConcept("IdiomElement")
        val idiomElements = Containment.createMultiple("elements", idiomElement)
        idiom.addFeature(idiomElements)

        val featureConstraint = Concept(this, "FeatureConstraint")
        featureConstraint.isAbstract = true

        val propertyConstraint = Concept(this, "PropertyConstraint")
        propertyConstraint.isAbstract = true
        propertyConstraint.extendedConcept = featureConstraint

        val exactPropertyConstraint = Concept(this, "ExactPropertyConstraint")
        exactPropertyConstraint.extendedConcept = propertyConstraint
        // val exactPropertyConstraintValue =
    }
}
