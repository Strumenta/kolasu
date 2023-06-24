import com.strumenta.kolasu.lionweb.StarLasuLWLanguage
import io.lionweb.lioncore.java.language.Language
import io.lionweb.lioncore.java.serialization.JsonSerialization
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test


class ASTClassesGenerationTest {

    @Test
    fun allASTClassesAreGeneratedAsExpected() {
        val inputStream = this.javaClass.getResourceAsStream("/properties-language.json")
        val jsonser = JsonSerialization.getStandardSerialization()
        jsonser.nodeResolver.addTree(StarLasuLWLanguage)
        val propertiesLanguage = jsonser.unserializeToNodes(inputStream).first() as Language
        val generated = ASTGenerator("com.strumenta.properties", propertiesLanguage).generateClasses()
        assertEquals(1, generated.size)
        println(generated.first().code)
    }
}