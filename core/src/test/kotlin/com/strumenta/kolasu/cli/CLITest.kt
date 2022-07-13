package com.strumenta.kolasu.cli

import com.github.ajalt.clikt.core.PrintHelpMessage
import com.github.ajalt.clikt.output.CliktConsole
import com.strumenta.kolasu.model.Named
import com.strumenta.kolasu.model.Node
import com.strumenta.kolasu.parsing.ASTParser
import com.strumenta.kolasu.parsing.ParsingResult
import org.junit.Test
import java.io.File
import java.nio.charset.Charset
import kotlin.io.path.createTempDirectory
import kotlin.io.path.createTempFile
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

data class MyCompilationUnit(val decls: List<MyEntityDecl>) : Node()
data class MyEntityDecl(override var name: String, val fields: List<MyFieldDecl>) : Node(), Named
data class MyFieldDecl(override var name: String) : Node(), Named

class MyDummyParser : ASTParser<MyCompilationUnit> {
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
                expectedResults[myFile.toFile()] = ParsingResult(
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
                                )
                            )
                        )
                    )
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
            console.stdOutput
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
                expectedResults[myFile.toFile()] = ParsingResult(
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
                                )
                            )
                        )
                    )
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
            console.stdOutput
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
                expectedResults[myFile.toFile()] = ParsingResult(
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
                                )
                            )
                        )
                    )
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
            console.stdOutput.trim()
        )
        assertEquals("", console.errOutput)
    }

    @Test
    fun runSimpleASTSaverSimpleFileSpecifyingDebugFormat() {
        val myDir = createTempDirectory()
        val myFile = createTempFile(myDir, "myfile.mylang")
        myDir.toFile().deleteOnExit()
        myFile.toFile().deleteOnExit()

        val parserInstantiator = { file: File ->
            MyDummyParser().apply {
                expectedResults[myFile.toFile()] = ParsingResult(
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
                                )
                            )
                        )
                    )
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
            console.stdOutput.trim()
        )
        assertEquals("", console.errOutput)
    }

    @Test
    fun runStatsOnSimpleFile() {
        val myDir = createTempDirectory()
        val myFile = createTempFile(myDir, "myfile.mylang")
        myDir.toFile().deleteOnExit()
        myFile.toFile().deleteOnExit()

        val parserInstantiator = { file: File ->
            MyDummyParser().apply {
                expectedResults[myFile.toFile()] = ParsingResult(
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
                                )
                            )
                        )
                    )
                )
            }
        }
        val console = CapturingCliktConsole()
        val cliTool = CLITool(parserInstantiator, console)
        cliTool.parse(arrayOf("stats", myFile.toString()))
        assertEquals("""== Stats ==

 [Did processing complete?]
  files processed         : 1
     processing failed    : 0
     processing completed : 1

 [Did processing complete successfully?]
  processing completed with errors    : 0
  processing completed without errors : 1
  total number of errors              : 0""", console.stdOutput.trim())
        assertEquals("", console.errOutput)
    }

    @Test
    fun runStatsOnSimpleFileNoStats() {
        val myDir = createTempDirectory()
        val myFile = createTempFile(myDir, "myfile.mylang")
        myDir.toFile().deleteOnExit()
        myFile.toFile().deleteOnExit()

        val parserInstantiator = { file: File ->
            MyDummyParser().apply {
                expectedResults[myFile.toFile()] = ParsingResult(
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
                                )
                            )
                        )
                    )
                )
            }
        }
        val console = CapturingCliktConsole()
        val cliTool = CLITool(parserInstantiator, console)
        cliTool.parse(arrayOf("stats", "--no-stats", myFile.toString()))
        assertEquals("""""", console.stdOutput.trim())
        assertEquals("", console.errOutput)
    }

    @Test
    fun runStatsOnSimpleFileNodeStats() {
        val myDir = createTempDirectory()
        val myFile = createTempFile(myDir, "myfile.mylang")
        myDir.toFile().deleteOnExit()
        myFile.toFile().deleteOnExit()

        val parserInstantiator = { file: File ->
            MyDummyParser().apply {
                expectedResults[myFile.toFile()] = ParsingResult(
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
                                )
                            )
                        )
                    )
                )
            }
        }
        val console = CapturingCliktConsole()
        val cliTool = CLITool(parserInstantiator, console)
        cliTool.parse(arrayOf("stats", "--no-stats", "--node-stats", myFile.toString()))
        assertEquals("""== Node Stats ==

  com.strumenta.kolasu.cli.MyCompilationUnit        : 1
  com.strumenta.kolasu.cli.MyEntityDecl             : 2
  com.strumenta.kolasu.cli.MyFieldDecl              : 3

  total number of nodes                             : 6""", console.stdOutput.trim())
        assertEquals("", console.errOutput)
    }

    @Test
    fun runStatsOnSimpleFileNodeStatsSimpleNames() {
        val myDir = createTempDirectory()
        val myFile = createTempFile(myDir, "myfile.mylang")
        myDir.toFile().deleteOnExit()
        myFile.toFile().deleteOnExit()

        val parserInstantiator = { file: File ->
            MyDummyParser().apply {
                expectedResults[myFile.toFile()] = ParsingResult(
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
                                )
                            )
                        )
                    )
                )
            }
        }
        val console = CapturingCliktConsole()
        val cliTool = CLITool(parserInstantiator, console)
        cliTool.parse(arrayOf("stats", "--no-stats", "--node-stats", "-sn", myFile.toString()))
        assertEquals("""== Node Stats ==

  MyCompilationUnit        : 1
  MyEntityDecl             : 2
  MyFieldDecl              : 3

  total number of nodes    : 6""", console.stdOutput.trim())
        assertEquals("", console.errOutput)
    }
}
