package com.strumenta.kolasu.cli

import com.github.ajalt.clikt.core.PrintHelpMessage
import com.github.ajalt.clikt.output.CliktConsole
import com.strumenta.kolasu.model.Named
import com.strumenta.kolasu.model.Node
import com.strumenta.kolasu.model.Source
import com.strumenta.kolasu.model.debugPrint
import com.strumenta.kolasu.parsing.ASTParser
import com.strumenta.kolasu.parsing.ParsingResult
import org.junit.Test
import java.io.File
import java.nio.charset.Charset
import kotlin.io.path.createTempDirectory
import kotlin.io.path.createTempFile
import kotlin.io.path.pathString
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

data class MyCompilationUnit(
    val decls: List<MyEntityDecl>,
) : Node()

data class MyEntityDecl(
    override var name: String,
    val fields: List<MyFieldDecl>,
) : Node(),
    Named

data class MyFieldDecl(
    override var name: String,
) : Node(),
    Named

class MyDummyParser : ASTParser<MyCompilationUnit> {
    val expectedResults = HashMap<File, ParsingResult<MyCompilationUnit>>()

    override fun parse(
        code: String,
        considerRange: Boolean,
        measureLexingTime: Boolean,
        source: Source?,
    ): ParsingResult<MyCompilationUnit> {
        TODO("Not yet implemented")
    }

    override fun parse(
        file: File,
        charset: Charset,
        considerPosition: Boolean,
        measureLexingTime: Boolean,
    ): ParsingResult<MyCompilationUnit> =
        expectedResults[file] ?: throw java.lang.IllegalArgumentException("Unexpected file $file")
}

class CapturingCliktConsole : CliktConsole {
    override val lineSeparator: String
        get() = "\n"

    var stdOutput = ""
    var errOutput = ""

    override fun print(
        text: String,
        error: Boolean,
    ) {
        if (error) {
            errOutput += text
        } else {
            stdOutput += text
        }
    }

    override fun promptForLine(
        prompt: String,
        hideInput: Boolean,
    ): String? {
        TODO("Not yet implemented")
    }
}

class CLITest {
    @Test
    fun runSimpleASTSaverWithoutSpecifyingCommands() {
        val parserInstantiator = { file: File ->
            MyDummyParser()
        }
        val console = CapturingCliktConsole()
        val cliTool = CLITool(parserInstantiator, console)
        assertFailsWith(PrintHelpMessage::class) {
            cliTool.parse(emptyArray())
        }
    }

    @Test
    fun runSimpleASTSaverAskHelp() {
        val parserInstantiator = { file: File ->
            MyDummyParser()
        }
        val console = CapturingCliktConsole()
        val cliTool = CLITool(parserInstantiator, console)
        assertFailsWith(PrintHelpMessage::class) {
            cliTool.parse(arrayOf("-h"))
        }
    }

    @Test
    fun runSimpleASTSaverSimpleFileWithoutSpecifyingFormat() {
        val myDir = createTempDirectory()
        val myFile = createTempFile(myDir, "myfile.mylang")
        myDir.toFile().deleteOnExit()
        myFile.toFile().deleteOnExit()

        val parserInstantiator = { file: File ->
            MyDummyParser().apply {
                expectedResults[myFile.toFile()] =
                    ParsingResult(
                        emptyList(),
                        MyCompilationUnit(
                            mutableListOf(
                                MyEntityDecl("Entity1", mutableListOf()),
                                MyEntityDecl(
                                    "Entity2",
                                    mutableListOf(
                                        MyFieldDecl("f1"),
                                        MyFieldDecl("f2"),
                                        MyFieldDecl("f3"),
                                    ),
                                ),
                            ),
                        ),
                    )
            }
        }
        val console = CapturingCliktConsole()
        val cliTool = CLITool(parserInstantiator, console)
        cliTool.parse(arrayOf("ast", myFile.toString(), "--print"))
        assertEquals(
            "{\n" +
                "  \"issues\": [],\n" +
                "  \"root\": {\n" +
                "    \"#type\": \"com.strumenta.kolasu.cli.MyCompilationUnit\",\n" +
                "    \"decls\": [\n" +
                "      {\n" +
                "        \"#type\": \"com.strumenta.kolasu.cli.MyEntityDecl\",\n" +
                "        \"fields\": [],\n" +
                "        \"name\": \"Entity1\"\n" +
                "      },\n" +
                "      {\n" +
                "        \"#type\": \"com.strumenta.kolasu.cli.MyEntityDecl\",\n" +
                "        \"fields\": [\n" +
                "          {\n" +
                "            \"#type\": \"com.strumenta.kolasu.cli.MyFieldDecl\",\n" +
                "            \"name\": \"f1\"\n" +
                "          },\n" +
                "          {\n" +
                "            \"#type\": \"com.strumenta.kolasu.cli.MyFieldDecl\",\n" +
                "            \"name\": \"f2\"\n" +
                "          },\n" +
                "          {\n" +
                "            \"#type\": \"com.strumenta.kolasu.cli.MyFieldDecl\",\n" +
                "            \"name\": \"f3\"\n" +
                "          }\n" +
                "        ],\n" +
                "        \"name\": \"Entity2\"\n" +
                "      }\n" +
                "    ]\n" +
                "  }\n" +
                "}\n",
            console.stdOutput,
        )
        assertEquals("", console.errOutput)
    }

    @Test
    fun runSimpleASTSaverSimpleFileSpecifyingJSON() {
        val myDir = createTempDirectory()
        val myFile = createTempFile(myDir, "myfile.mylang")
        myDir.toFile().deleteOnExit()
        myFile.toFile().deleteOnExit()

        val parserInstantiator = { file: File ->
            MyDummyParser().apply {
                expectedResults[myFile.toFile()] =
                    ParsingResult(
                        emptyList(),
                        MyCompilationUnit(
                            mutableListOf(
                                MyEntityDecl("Entity1", mutableListOf()),
                                MyEntityDecl(
                                    "Entity2",
                                    mutableListOf(
                                        MyFieldDecl("f1"),
                                        MyFieldDecl("f2"),
                                        MyFieldDecl("f3"),
                                    ),
                                ),
                            ),
                        ),
                    )
            }
        }
        val console = CapturingCliktConsole()
        val cliTool = CLITool(parserInstantiator, console)
        cliTool.parse(arrayOf("ast", myFile.toString(), "--print", "--format", "json"))
        assertEquals(
            "{\n" +
                "  \"issues\": [],\n" +
                "  \"root\": {\n" +
                "    \"#type\": \"com.strumenta.kolasu.cli.MyCompilationUnit\",\n" +
                "    \"decls\": [\n" +
                "      {\n" +
                "        \"#type\": \"com.strumenta.kolasu.cli.MyEntityDecl\",\n" +
                "        \"fields\": [],\n" +
                "        \"name\": \"Entity1\"\n" +
                "      },\n" +
                "      {\n" +
                "        \"#type\": \"com.strumenta.kolasu.cli.MyEntityDecl\",\n" +
                "        \"fields\": [\n" +
                "          {\n" +
                "            \"#type\": \"com.strumenta.kolasu.cli.MyFieldDecl\",\n" +
                "            \"name\": \"f1\"\n" +
                "          },\n" +
                "          {\n" +
                "            \"#type\": \"com.strumenta.kolasu.cli.MyFieldDecl\",\n" +
                "            \"name\": \"f2\"\n" +
                "          },\n" +
                "          {\n" +
                "            \"#type\": \"com.strumenta.kolasu.cli.MyFieldDecl\",\n" +
                "            \"name\": \"f3\"\n" +
                "          }\n" +
                "        ],\n" +
                "        \"name\": \"Entity2\"\n" +
                "      }\n" +
                "    ]\n" +
                "  }\n" +
                "}\n",
            console.stdOutput,
        )
        assertEquals("", console.errOutput)
    }

    @Test
    fun runSimpleASTSaverSimpleFileSpecifyingXML() {
        val myDir = createTempDirectory()
        val myFile = createTempFile(myDir, "myfile.mylang")
        myDir.toFile().deleteOnExit()
        myFile.toFile().deleteOnExit()

        val parserInstantiator = { file: File ->
            MyDummyParser().apply {
                expectedResults[myFile.toFile()] =
                    ParsingResult(
                        emptyList(),
                        MyCompilationUnit(
                            mutableListOf(
                                MyEntityDecl("Entity1", mutableListOf()),
                                MyEntityDecl(
                                    "Entity2",
                                    mutableListOf(
                                        MyFieldDecl("f1"),
                                        MyFieldDecl("f2"),
                                        MyFieldDecl("f3"),
                                    ),
                                ),
                            ),
                        ),
                    )
            }
        }
        val console = CapturingCliktConsole()
        val cliTool = CLITool(parserInstantiator, console)
        cliTool.parse(arrayOf("ast", myFile.toString(), "--print", "--format", "xml"))
        assertEquals(
            "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>\n" +
                "<result>\n" +
                "    <issues/>\n" +
                "    <root type=\"MyCompilationUnit\">\n" +
                "        <decls name=\"Entity1\" type=\"MyEntityDecl\"/>\n" +
                "        <decls name=\"Entity2\" type=\"MyEntityDecl\">\n" +
                "            <fields name=\"f1\" type=\"MyFieldDecl\"/>\n" +
                "            <fields name=\"f2\" type=\"MyFieldDecl\"/>\n" +
                "            <fields name=\"f3\" type=\"MyFieldDecl\"/>\n" +
                "        </decls>\n" +
                "    </root>\n" +
                "</result>",
            console.stdOutput.trim(),
        )
        assertEquals("", console.errOutput)
    }

    @Test
    fun debugPrint() {
        assertEquals(
            """MyFieldDecl {
            |  name = f3
            |} // MyFieldDecl
            |
            """.trimMargin(),
            MyFieldDecl("f3").debugPrint(),
        )
    }

    @Test
    fun runSimpleASTSaverSimpleFileSpecifyingDebugFormat() {
        val myDir = createTempDirectory()
        val myFile = createTempFile(myDir, "myfile.mylang")
        myDir.toFile().deleteOnExit()
        myFile.toFile().deleteOnExit()

        val parserInstantiator = { file: File ->
            MyDummyParser().apply {
                expectedResults[myFile.toFile()] =
                    ParsingResult(
                        emptyList(),
                        MyCompilationUnit(
                            mutableListOf(
                                MyEntityDecl("Entity1", mutableListOf()),
                                MyEntityDecl(
                                    "Entity2",
                                    mutableListOf(
                                        MyFieldDecl("f1"),
                                        MyFieldDecl("f2"),
                                        MyFieldDecl("f3"),
                                    ),
                                ),
                            ),
                        ),
                    )
            }
        }
        val console = CapturingCliktConsole()
        val cliTool = CLITool(parserInstantiator, console)
        cliTool.parse(arrayOf("ast", myFile.toString(), "--print", "--format", "debug-format"))
        assertEquals(
            "Result {\n" +
                "  issues= [\n" +
                "  ]\n" +
                "  root = [\n" +
                "    MyCompilationUnit {\n" +
                "      decls = [\n" +
                "        MyEntityDecl {\n" +
                "          fields = []\n" +
                "          name = Entity1\n" +
                "        } // MyEntityDecl\n" +
                "        MyEntityDecl {\n" +
                "          fields = [\n" +
                "            MyFieldDecl {\n" +
                "              name = f1\n" +
                "            } // MyFieldDecl\n" +
                "            MyFieldDecl {\n" +
                "              name = f2\n" +
                "            } // MyFieldDecl\n" +
                "            MyFieldDecl {\n" +
                "              name = f3\n" +
                "            } // MyFieldDecl\n" +
                "          ]\n" +
                "          name = Entity2\n" +
                "        } // MyEntityDecl\n" +
                "      ]\n" +
                "    } // MyCompilationUnit\n" +
                "  ]\n" +
                "}",
            console.stdOutput.trim(),
        )
        assertEquals("", console.errOutput)
    }

    @Test
    fun runSimpleASTSaverSimpleFileSpecifyingJSONOnDiskWithMultipleFiles() {
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
                expectedResults[myFile1] =
                    ParsingResult(
                        emptyList(),
                        MyCompilationUnit(
                            mutableListOf(
                                MyEntityDecl("EntityFoo", mutableListOf()),
                            ),
                        ),
                    )
                expectedResults[myFile2] =
                    ParsingResult(
                        emptyList(),
                        MyCompilationUnit(
                            mutableListOf(
                                MyEntityDecl("EntityBar", mutableListOf()),
                            ),
                        ),
                    )
            }
        }
        val console = CapturingCliktConsole()
        val cliTool = CLITool(parserInstantiator, console)
        cliTool.parse(arrayOf("ast", myDir.toString(), "-o", outDir.pathString, "--format", "json", "-v"))
        println(console.stdOutput)
//        assertEquals("", console.stdOutput)
//        assertEquals("", console.errOutput)
        val outMyFile1 = File(outDir.toFile(), "myfile1.mylang.json")
        val outMyFile2 = File(File(outDir.toFile(), "mySubDir"), "myfile2.mylang.json")
        assert(outMyFile1.exists())
        assertEquals(
            """{
  "issues": [],
  "root": {
    "#type": "com.strumenta.kolasu.cli.MyCompilationUnit",
    "decls": [
      {
        "#type": "com.strumenta.kolasu.cli.MyEntityDecl",
        "fields": [],
        "name": "EntityFoo"
      }
    ]
  }
}""",
            outMyFile1.readText(),
        )
        assertEquals(
            """{
  "issues": [],
  "root": {
    "#type": "com.strumenta.kolasu.cli.MyCompilationUnit",
    "decls": [
      {
        "#type": "com.strumenta.kolasu.cli.MyEntityDecl",
        "fields": [],
        "name": "EntityBar"
      }
    ]
  }
}""",
            outMyFile2.readText(),
        )
        assert(outMyFile2.exists())
    }

    @Test
    fun runSimpleASTSaverSimpleFileSpecifyingXMLOnDiskWithMultipleFiles() {
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
                expectedResults[myFile1] =
                    ParsingResult(
                        emptyList(),
                        MyCompilationUnit(
                            mutableListOf(
                                MyEntityDecl("EntityFoo", mutableListOf()),
                            ),
                        ),
                    )
                expectedResults[myFile2] =
                    ParsingResult(
                        emptyList(),
                        MyCompilationUnit(
                            mutableListOf(
                                MyEntityDecl("EntityBar", mutableListOf()),
                            ),
                        ),
                    )
            }
        }
        val console = CapturingCliktConsole()
        val cliTool = CLITool(parserInstantiator, console)
        cliTool.parse(arrayOf("ast", myDir.toString(), "-o", outDir.pathString, "--format", "xml", "-v"))
        println(console.stdOutput)
//        assertEquals("", console.stdOutput)
//        assertEquals("", console.errOutput)
        val outMyFile1 = File(outDir.toFile(), "myfile1.mylang.xml")
        val outMyFile2 = File(File(outDir.toFile(), "mySubDir"), "myfile2.mylang.xml")
        assert(outMyFile1.exists())
        assertEquals(
            """<?xml version="1.0" encoding="UTF-8" standalone="no"?>
<result>
    <issues/>
    <root type="MyCompilationUnit">
        <decls name="EntityFoo" type="MyEntityDecl"/>
    </root>
</result>
""".replace("\n", System.lineSeparator()),
            outMyFile1.readText(),
        )
        assertEquals(
            """<?xml version="1.0" encoding="UTF-8" standalone="no"?>
<result>
    <issues/>
    <root type="MyCompilationUnit">
        <decls name="EntityBar" type="MyEntityDecl"/>
    </root>
</result>
""".replace("\n", System.lineSeparator()),
            outMyFile2.readText(),
        )
        assert(outMyFile2.exists())
    }

    @Test
    fun runSimpleASTSaverSimpleFileSpecifyingDebugFormatOnDiskWithMultipleFiles() {
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
                expectedResults[myFile1] =
                    ParsingResult(
                        emptyList(),
                        MyCompilationUnit(
                            mutableListOf(
                                MyEntityDecl("EntityFoo", mutableListOf()),
                            ),
                        ),
                    )
                expectedResults[myFile2] =
                    ParsingResult(
                        emptyList(),
                        MyCompilationUnit(
                            mutableListOf(
                                MyEntityDecl("EntityBar", mutableListOf()),
                            ),
                        ),
                    )
            }
        }
        val console = CapturingCliktConsole()
        val cliTool = CLITool(parserInstantiator, console)
        cliTool.parse(arrayOf("ast", myDir.toString(), "-o", outDir.pathString, "--format", "debug-format", "-v"))
        println(console.stdOutput)
//        assertEquals("", console.stdOutput)
//        assertEquals("", console.errOutput)
        val outMyFile1 = File(outDir.toFile(), "myfile1.mylang.txt")
        val outMyFile2 = File(File(outDir.toFile(), "mySubDir"), "myfile2.mylang.txt")
        assert(outMyFile1.exists())
        assertEquals(
            """Result {
  issues= [
  ]
  root = [
    MyCompilationUnit {
      decls = [
        MyEntityDecl {
          fields = []
          name = EntityFoo
        } // MyEntityDecl
      ]
    } // MyCompilationUnit
  ]
}
""",
            outMyFile1.readText(),
        )
        assertEquals(
            """Result {
  issues= [
  ]
  root = [
    MyCompilationUnit {
      decls = [
        MyEntityDecl {
          fields = []
          name = EntityBar
        } // MyEntityDecl
      ]
    } // MyCompilationUnit
  ]
}
""",
            outMyFile2.readText(),
        )
        assert(outMyFile2.exists())
    }
}
