import com.strumenta.kolasu.lionweb.ASTGenerator
import io.lionweb.lioncore.java.emf.EMFMetamodelImporter
import org.eclipse.emf.common.util.URI
import org.eclipse.emf.ecore.EPackage
import org.eclipse.emf.ecore.EcorePackage
import org.eclipse.emf.ecore.xmi.impl.EcoreResourceFactoryImpl
import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.test.assertEquals

class ClassesGenerationFromEMFTest {
    private fun loadEPackage(name: String): EPackage {
        val inputStream = this.javaClass.getResourceAsStream("/$name.ecore")
        requireNotNull(inputStream)
        val resource = EcoreResourceFactoryImpl().createResource(URI.createURI("resource://$name.ecore"))
        resource.load(inputStream, mapOf<Any, Any>())
        assertEquals(1, resource.contents.size)
        return resource.contents.first() as EPackage
    }

    @Test
    @Ignore
    fun allASTClassesAreGeneratedAsExpected() {
        val jvmTypesPackage = loadEPackage("JavaVMTypes")
        val xbasePackage = loadEPackage("xbase")
        val xtendPackage = loadEPackage("xtend")
        val emfMMImporter = EMFMetamodelImporter()
        val jvmTypesLWLanguage = emfMMImporter.importEPackage(jvmTypesPackage)
        val xbaseLWLanguage = emfMMImporter.importEPackage(xbasePackage)
        val xtendLWLanguage = emfMMImporter.importEPackage(xtendPackage)

        val generated = ASTGenerator("xtend.stuff", xtendLWLanguage).generateClasses()
        assertEquals(1, generated.size)
        println(generated.first().code)
    }

    @Test
    fun allASTClassesAreGeneratedAsExpectedOCCI() {
        val occiPackage = loadEPackage("OCCI")
        val emfMMImporter = EMFMetamodelImporter()
        emfMMImporter.importEPackage(EcorePackage.eINSTANCE)
        val occiLWLanguage = emfMMImporter.importEPackage(occiPackage)
    }
}
