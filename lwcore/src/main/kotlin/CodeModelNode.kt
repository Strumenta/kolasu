import io.lionweb.lioncore.java.model.impl.DynamicNode
import io.lionweb.lioncore.kotlin.BaseNode

abstract class CodeModelNode : BaseNode()

class MyCompilationUnit : CodeModelNode() {
    // Creiamo i nodi o come radici di un CodeModel o inseriti in un CodeModel

    companion object {
        fun root(populator: (MyCompilationUnit.()->Unit)? = null) : MyCompilationUnit {
            val mc = MyCompilationUnit()
            populator?.invoke(mc)
            return mc
        }
    }

    fun classDecl(populator: (MyClassDecl.()->Unit)? = null) : MyClassDecl {
        val mc = MyClassDecl()
        populator?.invoke(mc)
        return mc
    }
}

class MyClassDecl : CodeModelNode() {
    var name : String? = null
    companion object {

    }
}

val myAst = MyCompilationUnit.root {
    classDecl {
        name = "foo"
    }
}

