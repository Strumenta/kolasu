package com.strumenta.kolasu.emf.cli

import com.github.ajalt.clikt.core.PrintHelpMessage
import com.github.ajalt.clikt.output.CliktConsole
import com.strumenta.kolasu.emf.EcoreEnabledParser
import com.strumenta.kolasu.emf.MetamodelBuilder
import com.strumenta.kolasu.model.ASTNode
import com.strumenta.kolasu.model.Named
import com.strumenta.kolasu.parsing.ParsingResult
import com.strumenta.kolasu.validation.Issue
import org.antlr.v4.runtime.CharStream
import org.antlr.v4.runtime.Lexer
import org.antlr.v4.runtime.Parser
import org.antlr.v4.runtime.ParserRuleContext
import org.antlr.v4.runtime.TokenStream
import org.eclipse.emf.ecore.resource.Resource
import org.junit.Test
import java.io.File
import java.nio.charset.Charset
import kotlin.io.path.createTempDirectory
import kotlin.io.path.pathString
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

data class MyCompilationUnit(val decls: List<MyEntityDecl>) : ASTNode()
data class MyEntityDecl(override var name: String, val fields: List<MyFieldDecl>) : ASTNode(), Named
data class MyFieldDecl(override var name: String) : ASTNode(), Named

class MyDummyParser : EcoreEnabledParser<MyCompilationUnit, Parser, ParserRuleContext>() {
    override fun doGenerateMetamodel(resource: Resource) {
        val mmbuilder = MetamodelBuilder("com.strumenta.kolasu.emf.cli", "https://dummy.com/mm", "dm")
        mmbuilder.provideClass(MyCompilationUnit::class)
        val mm = mmbuilder.generate()
        resource.contents.add(mm)
    }

    override fun createANTLRLexer(charStream: CharStream): Lexer {
        TODO("Not yet implemented")
    }

    override fun createANTLRParser(tokenStream: TokenStream): Parser {
        TODO("Not yet implemented")
    }

    override fun parseTreeToAst(
        parseTreeRoot: ParserRuleContext,
        considerPosition: Boolean,
        issues: MutableList<Issue>
    ): MyCompilationUnit? {
        TODO("Not yet implemented")
    }

    override fun parse(
        code: String,
        considerPosition: Boolean,
        measureLexingTime: Boolean
    ): ParsingResult<MyCompilationUnit> {
        TODO("Not yet implemented")
    }

    val expectedResults = HashMap<File, ParsingResult<MyCompilationUnit>>()

    override fun parse(file: File, charset: Charset, considerPosition: Boolean): ParsingResult<MyCompilationUnit> {
        return expectedResults[file] ?: throw java.lang.IllegalArgumentException("Unexpected file $file")
    }
}

class CapturingCliktConsole : CliktConsole {
    override val lineSeparator: String
        get() = "\n"

    var stdOutput = ""
    var errOutput = ""

    override fun print(text: String, error: Boolean) {
        if (error) {
            errOutput += text
        } else {
            stdOutput += text
        }
    }

    override fun promptForLine(prompt: String, hideInput: Boolean): String? {
        TODO("Not yet implemented")
    }
}

class EMFCLIToolTest {
    @Test
    fun runSimpleASTSaverWithoutSpecifyingCommands() {
        val parserInstantiator = { file: File ->
            MyDummyParser()
        }
        val mmSupport = MyDummyParser()
        val console = CapturingCliktConsole()
        val cliTool = EMFCLITool(parserInstantiator, mmSupport, console)
        assertFailsWith(PrintHelpMessage::class) {
            cliTool.parse(emptyArray())
        }
    }

    @Test
    fun runSimpleASTSaverAskHelp() {
        val parserInstantiator = { file: File ->
            MyDummyParser()
        }
        val mmSupport = MyDummyParser()
        val console = CapturingCliktConsole()
        val cliTool = EMFCLITool(parserInstantiator, mmSupport, console)
        assertFailsWith(PrintHelpMessage::class) {
            cliTool.parse(arrayOf("-h"))
        }
    }

    @Test
    fun runMetamodel() {
        val parserInstantiator = { file: File ->
            MyDummyParser()
        }
        val mmSupport = MyDummyParser()
        val console = CapturingCliktConsole()
        val cliTool = EMFCLITool(parserInstantiator, mmSupport, console)
        val myDir = createTempDirectory()
        val myFile = File(myDir.toFile(), "mymetamodel.json")
        myDir.toFile().deleteOnExit()
        cliTool.parse(arrayOf("metamodel", "-o", myFile.path, "-v"))
        println(console.stdOutput)
        assertEquals("", console.errOutput)
        assert(myFile.exists())
        assertEquals(
            """{
  "eClass" : "http://www.eclipse.org/emf/2002/Ecore#//EPackage",
  "name" : "com.strumenta.kolasu.emf.cli",
  "nsURI" : "https://dummy.com/mm",
  "nsPrefix" : "dm",
  "eClassifiers" : [ {
    "eClass" : "http://www.eclipse.org/emf/2002/Ecore#//EClass",
    "name" : "MyFieldDecl",
    "eSuperTypes" : [ {
      "eClass" : "http://www.eclipse.org/emf/2002/Ecore#//EClass",
      "${'$'}ref" : "https://strumenta.com/starlasu/v2#//ASTNode"
    }, {
      "eClass" : "http://www.eclipse.org/emf/2002/Ecore#//EClass",
      "${'$'}ref" : "https://strumenta.com/starlasu/v2#//Named"
    } ]
  }, {
    "eClass" : "http://www.eclipse.org/emf/2002/Ecore#//EClass",
    "name" : "MyEntityDecl",
    "eSuperTypes" : [ {
      "eClass" : "http://www.eclipse.org/emf/2002/Ecore#//EClass",
      "${'$'}ref" : "https://strumenta.com/starlasu/v2#//ASTNode"
    }, {
      "eClass" : "http://www.eclipse.org/emf/2002/Ecore#//EClass",
      "${'$'}ref" : "https://strumenta.com/starlasu/v2#//Named"
    } ],
    "eStructuralFeatures" : [ {
      "eClass" : "http://www.eclipse.org/emf/2002/Ecore#//EReference",
      "name" : "fields",
      "upperBound" : -1,
      "eType" : {
        "eClass" : "http://www.eclipse.org/emf/2002/Ecore#//EClass",
        "${'$'}ref" : "//MyFieldDecl"
      },
      "containment" : true
    } ]
  }, {
    "eClass" : "http://www.eclipse.org/emf/2002/Ecore#//EClass",
    "name" : "MyCompilationUnit",
    "eSuperTypes" : [ {
      "eClass" : "http://www.eclipse.org/emf/2002/Ecore#//EClass",
      "${'$'}ref" : "https://strumenta.com/starlasu/v2#//ASTNode"
    } ],
    "eStructuralFeatures" : [ {
      "eClass" : "http://www.eclipse.org/emf/2002/Ecore#//EReference",
      "name" : "decls",
      "upperBound" : -1,
      "eType" : {
        "eClass" : "http://www.eclipse.org/emf/2002/Ecore#//EClass",
        "${'$'}ref" : "//MyEntityDecl"
      },
      "containment" : true
    } ]
  } ]
}""",
            myFile.readText()
        )
    }

    @Test
    fun runMetamodelIncludingKolasu() {
        val parserInstantiator = { file: File ->
            MyDummyParser()
        }
        val mmSupport = MyDummyParser()
        val console = CapturingCliktConsole()
        val cliTool = EMFCLITool(parserInstantiator, mmSupport, console)
        val myDir = createTempDirectory()
        val myFile = File(myDir.toFile(), "mymetamodel.json")
        myDir.toFile().deleteOnExit()
        cliTool.parse(arrayOf("metamodel", "-o", myFile.path, "-v", "-ik"))
        println(console.stdOutput)
        assertEquals("", console.errOutput)
        assert(myFile.exists())
        assertEquals(
            """[ {
  "eClass" : "http://www.eclipse.org/emf/2002/Ecore#//EPackage",
  "name" : "StrumentaLanguageSupport",
  "nsURI" : "https://strumenta.com/starlasu/v2",
  "eClassifiers" : [ {
    "eClass" : "http://www.eclipse.org/emf/2002/Ecore#//EClass",
    "name" : "LocalDate",
    "eStructuralFeatures" : [ {
      "eClass" : "http://www.eclipse.org/emf/2002/Ecore#//EAttribute",
      "name" : "year",
      "lowerBound" : 1,
      "eType" : {
        "eClass" : "http://www.eclipse.org/emf/2002/Ecore#//EDataType",
        "${'$'}ref" : "http://www.eclipse.org/emf/2002/Ecore#//EInt"
      }
    }, {
      "eClass" : "http://www.eclipse.org/emf/2002/Ecore#//EAttribute",
      "name" : "month",
      "lowerBound" : 1,
      "eType" : {
        "eClass" : "http://www.eclipse.org/emf/2002/Ecore#//EDataType",
        "${'$'}ref" : "http://www.eclipse.org/emf/2002/Ecore#//EInt"
      }
    }, {
      "eClass" : "http://www.eclipse.org/emf/2002/Ecore#//EAttribute",
      "name" : "dayOfMonth",
      "lowerBound" : 1,
      "eType" : {
        "eClass" : "http://www.eclipse.org/emf/2002/Ecore#//EDataType",
        "${'$'}ref" : "http://www.eclipse.org/emf/2002/Ecore#//EInt"
      }
    } ]
  }, {
    "eClass" : "http://www.eclipse.org/emf/2002/Ecore#//EClass",
    "name" : "LocalTime",
    "eStructuralFeatures" : [ {
      "eClass" : "http://www.eclipse.org/emf/2002/Ecore#//EAttribute",
      "name" : "hour",
      "lowerBound" : 1,
      "eType" : {
        "eClass" : "http://www.eclipse.org/emf/2002/Ecore#//EDataType",
        "${'$'}ref" : "http://www.eclipse.org/emf/2002/Ecore#//EInt"
      }
    }, {
      "eClass" : "http://www.eclipse.org/emf/2002/Ecore#//EAttribute",
      "name" : "minute",
      "lowerBound" : 1,
      "eType" : {
        "eClass" : "http://www.eclipse.org/emf/2002/Ecore#//EDataType",
        "${'$'}ref" : "http://www.eclipse.org/emf/2002/Ecore#//EInt"
      }
    }, {
      "eClass" : "http://www.eclipse.org/emf/2002/Ecore#//EAttribute",
      "name" : "second",
      "lowerBound" : 1,
      "eType" : {
        "eClass" : "http://www.eclipse.org/emf/2002/Ecore#//EDataType",
        "${'$'}ref" : "http://www.eclipse.org/emf/2002/Ecore#//EInt"
      }
    }, {
      "eClass" : "http://www.eclipse.org/emf/2002/Ecore#//EAttribute",
      "name" : "nanosecond",
      "lowerBound" : 1,
      "eType" : {
        "eClass" : "http://www.eclipse.org/emf/2002/Ecore#//EDataType",
        "${'$'}ref" : "http://www.eclipse.org/emf/2002/Ecore#//EInt"
      }
    } ]
  }, {
    "eClass" : "http://www.eclipse.org/emf/2002/Ecore#//EClass",
    "name" : "LocalDateTime",
    "eStructuralFeatures" : [ {
      "eClass" : "http://www.eclipse.org/emf/2002/Ecore#//EReference",
      "name" : "date",
      "lowerBound" : 1,
      "eType" : {
        "eClass" : "http://www.eclipse.org/emf/2002/Ecore#//EClass",
        "${'$'}ref" : "/0/LocalDate"
      },
      "containment" : true
    }, {
      "eClass" : "http://www.eclipse.org/emf/2002/Ecore#//EReference",
      "name" : "time",
      "lowerBound" : 1,
      "eType" : {
        "eClass" : "http://www.eclipse.org/emf/2002/Ecore#//EClass",
        "${'$'}ref" : "/0/LocalTime"
      },
      "containment" : true
    } ]
  }, {
    "eClass" : "http://www.eclipse.org/emf/2002/Ecore#//EClass",
    "name" : "Point",
    "eStructuralFeatures" : [ {
      "eClass" : "http://www.eclipse.org/emf/2002/Ecore#//EAttribute",
      "name" : "line",
      "lowerBound" : 1,
      "eType" : {
        "eClass" : "http://www.eclipse.org/emf/2002/Ecore#//EDataType",
        "${'$'}ref" : "http://www.eclipse.org/emf/2002/Ecore#//EInt"
      }
    }, {
      "eClass" : "http://www.eclipse.org/emf/2002/Ecore#//EAttribute",
      "name" : "column",
      "lowerBound" : 1,
      "eType" : {
        "eClass" : "http://www.eclipse.org/emf/2002/Ecore#//EDataType",
        "${'$'}ref" : "http://www.eclipse.org/emf/2002/Ecore#//EInt"
      }
    } ]
  }, {
    "eClass" : "http://www.eclipse.org/emf/2002/Ecore#//EClass",
    "name" : "Position",
    "eStructuralFeatures" : [ {
      "eClass" : "http://www.eclipse.org/emf/2002/Ecore#//EReference",
      "name" : "start",
      "lowerBound" : 1,
      "eType" : {
        "eClass" : "http://www.eclipse.org/emf/2002/Ecore#//EClass",
        "${'$'}ref" : "/0/Point"
      },
      "containment" : true
    }, {
      "eClass" : "http://www.eclipse.org/emf/2002/Ecore#//EReference",
      "name" : "end",
      "lowerBound" : 1,
      "eType" : {
        "eClass" : "http://www.eclipse.org/emf/2002/Ecore#//EClass",
        "${'$'}ref" : "/0/Point"
      },
      "containment" : true
    } ]
  }, {
    "eClass" : "http://www.eclipse.org/emf/2002/Ecore#//EClass",
    "name" : "Origin",
    "abstract" : true
  }, {
    "eClass" : "http://www.eclipse.org/emf/2002/Ecore#//EClass",
    "name" : "Destination",
    "abstract" : true
  }, {
    "eClass" : "http://www.eclipse.org/emf/2002/Ecore#//EClass",
    "name" : "NodeDestination",
    "eSuperTypes" : [ {
      "eClass" : "http://www.eclipse.org/emf/2002/Ecore#//EClass",
      "${'$'}ref" : "/0/Destination"
    } ],
    "eStructuralFeatures" : [ {
      "eClass" : "http://www.eclipse.org/emf/2002/Ecore#//EReference",
      "name" : "node",
      "lowerBound" : 1,
      "eType" : {
        "eClass" : "http://www.eclipse.org/emf/2002/Ecore#//EClass",
        "${'$'}ref" : "/0/ASTNode"
      }
    } ]
  }, {
    "eClass" : "http://www.eclipse.org/emf/2002/Ecore#//EClass",
    "name" : "TextFileDestination",
    "eSuperTypes" : [ {
      "eClass" : "http://www.eclipse.org/emf/2002/Ecore#//EClass",
      "${'$'}ref" : "/0/Destination"
    } ],
    "eStructuralFeatures" : [ {
      "eClass" : "http://www.eclipse.org/emf/2002/Ecore#//EReference",
      "name" : "position",
      "eType" : {
        "eClass" : "http://www.eclipse.org/emf/2002/Ecore#//EClass",
        "${'$'}ref" : "/0/Position"
      },
      "containment" : true
    } ]
  }, {
    "eClass" : "http://www.eclipse.org/emf/2002/Ecore#//EClass",
    "name" : "ASTNode",
    "abstract" : true,
    "eSuperTypes" : [ {
      "eClass" : "http://www.eclipse.org/emf/2002/Ecore#//EClass",
      "${'$'}ref" : "/0/Origin"
    } ],
    "eStructuralFeatures" : [ {
      "eClass" : "http://www.eclipse.org/emf/2002/Ecore#//EReference",
      "name" : "position",
      "eType" : {
        "eClass" : "http://www.eclipse.org/emf/2002/Ecore#//EClass",
        "${'$'}ref" : "/0/Position"
      },
      "containment" : true
    }, {
      "eClass" : "http://www.eclipse.org/emf/2002/Ecore#//EReference",
      "name" : "origin",
      "eType" : {
        "eClass" : "http://www.eclipse.org/emf/2002/Ecore#//EClass",
        "${'$'}ref" : "/0/Origin"
      },
      "containment" : true
    }, {
      "eClass" : "http://www.eclipse.org/emf/2002/Ecore#//EReference",
      "name" : "destination",
      "eType" : {
        "eClass" : "http://www.eclipse.org/emf/2002/Ecore#//EClass",
        "${'$'}ref" : "/0/Destination"
      },
      "containment" : true
    } ]
  }, {
    "eClass" : "http://www.eclipse.org/emf/2002/Ecore#//EClass",
    "name" : "ParseTreeOrigin",
    "eSuperTypes" : [ {
      "eClass" : "http://www.eclipse.org/emf/2002/Ecore#//EClass",
      "${'$'}ref" : "/0/Origin"
    } ],
    "eStructuralFeatures" : [ {
      "eClass" : "http://www.eclipse.org/emf/2002/Ecore#//EReference",
      "name" : "position",
      "eType" : {
        "eClass" : "http://www.eclipse.org/emf/2002/Ecore#//EClass",
        "${'$'}ref" : "/0/Position"
      },
      "containment" : true
    } ]
  }, {
    "eClass" : "http://www.eclipse.org/emf/2002/Ecore#//EClass",
    "name" : "NodeOrigin",
    "eSuperTypes" : [ {
      "eClass" : "http://www.eclipse.org/emf/2002/Ecore#//EClass",
      "${'$'}ref" : "/0/Origin"
    } ],
    "eStructuralFeatures" : [ {
      "eClass" : "http://www.eclipse.org/emf/2002/Ecore#//EReference",
      "name" : "node",
      "lowerBound" : 1,
      "eType" : {
        "eClass" : "http://www.eclipse.org/emf/2002/Ecore#//EClass",
        "${'$'}ref" : "/0/ASTNode"
      }
    } ]
  }, {
    "eClass" : "http://www.eclipse.org/emf/2002/Ecore#//EClass",
    "name" : "Statement",
    "interface" : true
  }, {
    "eClass" : "http://www.eclipse.org/emf/2002/Ecore#//EClass",
    "name" : "Expression",
    "interface" : true
  }, {
    "eClass" : "http://www.eclipse.org/emf/2002/Ecore#//EClass",
    "name" : "EntityDeclaration",
    "interface" : true
  }, {
    "eClass" : "http://www.eclipse.org/emf/2002/Ecore#//EClass",
    "name" : "PlaceholderElement",
    "interface" : true,
    "eStructuralFeatures" : [ {
      "eClass" : "http://www.eclipse.org/emf/2002/Ecore#//EAttribute",
      "name" : "placeholderName",
      "eType" : {
        "eClass" : "http://www.eclipse.org/emf/2002/Ecore#//EDataType",
        "${'$'}ref" : "http://www.eclipse.org/emf/2002/Ecore#//EString"
      }
    } ]
  }, {
    "eClass" : "http://www.eclipse.org/emf/2002/Ecore#//EEnum",
    "name" : "IssueType",
    "eLiterals" : [ {
      "name" : "LEXICAL"
    }, {
      "name" : "SYNTACTIC",
      "value" : 1
    }, {
      "name" : "SEMANTIC",
      "value" : 2
    } ]
  }, {
    "eClass" : "http://www.eclipse.org/emf/2002/Ecore#//EEnum",
    "name" : "IssueSeverity",
    "eLiterals" : [ {
      "name" : "ERROR"
    }, {
      "name" : "WARNING",
      "value" : 1
    }, {
      "name" : "INFO",
      "value" : 2
    } ]
  }, {
    "eClass" : "http://www.eclipse.org/emf/2002/Ecore#//EClass",
    "name" : "Issue",
    "eStructuralFeatures" : [ {
      "eClass" : "http://www.eclipse.org/emf/2002/Ecore#//EAttribute",
      "name" : "type",
      "lowerBound" : 1,
      "eType" : {
        "eClass" : "http://www.eclipse.org/emf/2002/Ecore#//EEnum",
        "${'$'}ref" : "/0/IssueType"
      }
    }, {
      "eClass" : "http://www.eclipse.org/emf/2002/Ecore#//EAttribute",
      "name" : "message",
      "lowerBound" : 1,
      "eType" : {
        "eClass" : "http://www.eclipse.org/emf/2002/Ecore#//EDataType",
        "${'$'}ref" : "http://www.eclipse.org/emf/2002/Ecore#//EString"
      }
    }, {
      "eClass" : "http://www.eclipse.org/emf/2002/Ecore#//EAttribute",
      "name" : "severity",
      "eType" : {
        "eClass" : "http://www.eclipse.org/emf/2002/Ecore#//EEnum",
        "${'$'}ref" : "/0/IssueSeverity"
      }
    }, {
      "eClass" : "http://www.eclipse.org/emf/2002/Ecore#//EReference",
      "name" : "position",
      "eType" : {
        "eClass" : "http://www.eclipse.org/emf/2002/Ecore#//EClass",
        "${'$'}ref" : "/0/Position"
      },
      "containment" : true
    } ]
  }, {
    "eClass" : "http://www.eclipse.org/emf/2002/Ecore#//EClass",
    "name" : "PossiblyNamed",
    "interface" : true,
    "eStructuralFeatures" : [ {
      "eClass" : "http://www.eclipse.org/emf/2002/Ecore#//EAttribute",
      "name" : "name",
      "eType" : {
        "eClass" : "http://www.eclipse.org/emf/2002/Ecore#//EDataType",
        "${'$'}ref" : "http://www.eclipse.org/emf/2002/Ecore#//EString"
      }
    } ]
  }, {
    "eClass" : "http://www.eclipse.org/emf/2002/Ecore#//EClass",
    "name" : "Named",
    "interface" : true,
    "eSuperTypes" : [ {
      "eClass" : "http://www.eclipse.org/emf/2002/Ecore#//EClass",
      "${'$'}ref" : "/0/PossiblyNamed"
    } ],
    "eStructuralFeatures" : [ {
      "eClass" : "http://www.eclipse.org/emf/2002/Ecore#//EAttribute",
      "name" : "name",
      "lowerBound" : 1,
      "eType" : {
        "eClass" : "http://www.eclipse.org/emf/2002/Ecore#//EDataType",
        "${'$'}ref" : "http://www.eclipse.org/emf/2002/Ecore#//EString"
      }
    } ]
  }, {
    "eClass" : "http://www.eclipse.org/emf/2002/Ecore#//EClass",
    "name" : "ReferenceByName",
    "eTypeParameters" : [ {
      "name" : "N",
      "eBounds" : [ {
        "eClassifier" : {
          "eClass" : "http://www.eclipse.org/emf/2002/Ecore#//EClass",
          "${'$'}ref" : "/0/ASTNode"
        }
      } ]
    } ],
    "eStructuralFeatures" : [ {
      "eClass" : "http://www.eclipse.org/emf/2002/Ecore#//EAttribute",
      "name" : "name",
      "lowerBound" : 1,
      "eType" : {
        "eClass" : "http://www.eclipse.org/emf/2002/Ecore#//EDataType",
        "${'$'}ref" : "http://www.eclipse.org/emf/2002/Ecore#//EString"
      }
    }, {
      "eClass" : "http://www.eclipse.org/emf/2002/Ecore#//EReference",
      "name" : "referenced",
      "eGenericType" : {
        "eTypeParameter" : {
          "eClass" : "http://www.eclipse.org/emf/2002/Ecore#//ETypeParameter",
          "${'$'}ref" : "/0/ReferenceByName/N"
        }
      }
    } ]
  }, {
    "eClass" : "http://www.eclipse.org/emf/2002/Ecore#//EClass",
    "name" : "ErrorNode",
    "interface" : true,
    "eStructuralFeatures" : [ {
      "eClass" : "http://www.eclipse.org/emf/2002/Ecore#//EAttribute",
      "name" : "message",
      "lowerBound" : 1,
      "eType" : {
        "eClass" : "http://www.eclipse.org/emf/2002/Ecore#//EDataType",
        "${'$'}ref" : "http://www.eclipse.org/emf/2002/Ecore#//EString"
      }
    } ]
  }, {
    "eClass" : "http://www.eclipse.org/emf/2002/Ecore#//EClass",
    "name" : "GenericErrorNode",
    "eSuperTypes" : [ {
      "eClass" : "http://www.eclipse.org/emf/2002/Ecore#//EClass",
      "${'$'}ref" : "/0/ASTNode"
    }, {
      "eClass" : "http://www.eclipse.org/emf/2002/Ecore#//EClass",
      "${'$'}ref" : "/0/ErrorNode"
    } ]
  }, {
    "eClass" : "http://www.eclipse.org/emf/2002/Ecore#//EClass",
    "name" : "GenericNode",
    "eSuperTypes" : [ {
      "eClass" : "http://www.eclipse.org/emf/2002/Ecore#//EClass",
      "${'$'}ref" : "/0/ASTNode"
    } ]
  }, {
    "eClass" : "http://www.eclipse.org/emf/2002/Ecore#//EClass",
    "name" : "Result",
    "eTypeParameters" : [ {
      "name" : "CU",
      "eBounds" : [ {
        "eClassifier" : {
          "eClass" : "http://www.eclipse.org/emf/2002/Ecore#//EClass",
          "${'$'}ref" : "/0/ASTNode"
        }
      } ]
    } ],
    "eStructuralFeatures" : [ {
      "eClass" : "http://www.eclipse.org/emf/2002/Ecore#//EReference",
      "name" : "root",
      "eGenericType" : {
        "eTypeParameter" : {
          "eClass" : "http://www.eclipse.org/emf/2002/Ecore#//ETypeParameter",
          "${'$'}ref" : "/0/Result/CU"
        }
      },
      "containment" : true
    }, {
      "eClass" : "http://www.eclipse.org/emf/2002/Ecore#//EReference",
      "name" : "issues",
      "upperBound" : -1,
      "eType" : {
        "eClass" : "http://www.eclipse.org/emf/2002/Ecore#//EClass",
        "${'$'}ref" : "/0/Issue"
      },
      "containment" : true
    } ]
  } ]
}, {
  "eClass" : "http://www.eclipse.org/emf/2002/Ecore#//EPackage",
  "name" : "com.strumenta.kolasu.emf.cli",
  "nsURI" : "https://dummy.com/mm",
  "nsPrefix" : "dm",
  "eClassifiers" : [ {
    "eClass" : "http://www.eclipse.org/emf/2002/Ecore#//EClass",
    "name" : "MyFieldDecl",
    "eSuperTypes" : [ {
      "eClass" : "http://www.eclipse.org/emf/2002/Ecore#//EClass",
      "${'$'}ref" : "/0/ASTNode"
    }, {
      "eClass" : "http://www.eclipse.org/emf/2002/Ecore#//EClass",
      "${'$'}ref" : "/0/Named"
    } ]
  }, {
    "eClass" : "http://www.eclipse.org/emf/2002/Ecore#//EClass",
    "name" : "MyEntityDecl",
    "eSuperTypes" : [ {
      "eClass" : "http://www.eclipse.org/emf/2002/Ecore#//EClass",
      "${'$'}ref" : "/0/ASTNode"
    }, {
      "eClass" : "http://www.eclipse.org/emf/2002/Ecore#//EClass",
      "${'$'}ref" : "/0/Named"
    } ],
    "eStructuralFeatures" : [ {
      "eClass" : "http://www.eclipse.org/emf/2002/Ecore#//EReference",
      "name" : "fields",
      "upperBound" : -1,
      "eType" : {
        "eClass" : "http://www.eclipse.org/emf/2002/Ecore#//EClass",
        "${'$'}ref" : "/1/MyFieldDecl"
      },
      "containment" : true
    } ]
  }, {
    "eClass" : "http://www.eclipse.org/emf/2002/Ecore#//EClass",
    "name" : "MyCompilationUnit",
    "eSuperTypes" : [ {
      "eClass" : "http://www.eclipse.org/emf/2002/Ecore#//EClass",
      "${'$'}ref" : "/0/ASTNode"
    } ],
    "eStructuralFeatures" : [ {
      "eClass" : "http://www.eclipse.org/emf/2002/Ecore#//EReference",
      "name" : "decls",
      "upperBound" : -1,
      "eType" : {
        "eClass" : "http://www.eclipse.org/emf/2002/Ecore#//EClass",
        "${'$'}ref" : "/1/MyEntityDecl"
      },
      "containment" : true
    } ]
  } ]
} ]""",
            myFile.readText()
        )
    }

    @Test
    fun runSimpleModelSaverSWithMultipleFiles() {
        val myDir = createTempDirectory()
        val myFile1 = File(myDir.toFile(), "myfile1.mylang")
        myFile1.writeText("")
        val mySubDir = File(myDir.toFile(), "mySubDir")
        mySubDir.mkdir()
        val myFile2 = File(mySubDir, "myfile2.mylang")
        myFile2.writeText("")
        myFile1.deleteOnExit()
        myFile2.deleteOnExit()
        mySubDir.deleteOnExit()
        myDir.toFile().deleteOnExit()

        val outDir = createTempDirectory()
        outDir.toFile().deleteOnExit()

        val parserInstantiator = { file: File ->
            MyDummyParser().apply {
                expectedResults[myFile1] = ParsingResult(
                    emptyList(),
                    MyCompilationUnit(
                        mutableListOf(
                            MyEntityDecl("EntityFoo", mutableListOf()),
                        )
                    )
                )
                expectedResults[myFile2] = ParsingResult(
                    emptyList(),
                    MyCompilationUnit(
                        mutableListOf(
                            MyEntityDecl("EntityBar", mutableListOf()),
                        )
                    )
                )
            }
        }
        val mmSupport = MyDummyParser()
        val console = CapturingCliktConsole()
        val cliTool = EMFCLITool(parserInstantiator, mmSupport, console)
        cliTool.parse(arrayOf("model", myDir.toString(), "-o", outDir.pathString, "-v"))
        println(console.stdOutput)
//        assertEquals("", console.stdOutput)
//        assertEquals("", console.errOutput)
        val outMyFile1 = File(outDir.toFile(), "myfile1.json")
        val outMyFile2 = File(File(outDir.toFile(), "mySubDir"), "myfile2.json")
        assert(outMyFile1.exists())
        assertEquals(
            """{
  "eClass" : "https://strumenta.com/starlasu/v2#//Result",
  "root" : {
    "eClass" : "#//MyCompilationUnit",
    "decls" : [ {
      "name" : "EntityFoo"
    } ]
  }
}""",
            outMyFile1.readText()
        )
        assertEquals(
            """{
  "eClass" : "https://strumenta.com/starlasu/v2#//Result",
  "root" : {
    "eClass" : "#//MyCompilationUnit",
    "decls" : [ {
      "name" : "EntityBar"
    } ]
  }
}""",
            outMyFile2.readText()
        )
        assert(outMyFile2.exists())
    }
}
